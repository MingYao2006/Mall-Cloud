package com.mall.order.task;

import com.mall.order.entity.MqOutbox;
import com.mall.order.mapper.MqOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地消息表的投递任务。
 * <p>
 * 职责：轮询 {@code mq_outbox} 里 {@code status=0} 且到期的记录，投递到 RabbitMQ，
 * <b>拿到 Broker 的生产者确认（publisher confirm）后</b>才置为已投递；失败则指数退避重试。
 * <p>
 * 为什么必须等 confirm 而不是 {@code send} 返回就当成功：
 * {@code send} 只是把字节写进 TCP 缓冲，Broker 可能没收到、可能没持久化、
 * 也可能路由不到队列。只有 confirm 的 ack 才能证明"消息真的进了 Broker"。
 * <p>
 * 与旧实现的区别：原来在 {@code afterCommit} 里直接发，发送异常被 catch 吞掉，
 * 进程崩溃则消息彻底丢失；现在"消息一定会发"由数据库记录保证，
 * 进程在任何一个点崩溃，重启后任务都会接着把 INIT 记录投出去。
 */
@Slf4j
@Component
public class OutboxSendTask {

    /** 单批投递条数 */
    private static final int BATCH_SIZE = 100;

    /** 单条消息等待 confirm 的超时时间 */
    private static final long CONFIRM_TIMEOUT_SECONDS = 5L;

    /** 超过此次数仍失败则置为 STATUS_FAILED，退出扫描范围等人工介入 */
    private static final int MAX_RETRY = 10;

    /** 退避基数（秒）：1, 2, 4, 8, 16 ... 上限 10 分钟 */
    private static final long BACKOFF_BASE_SECONDS = 1L;
    private static final long BACKOFF_MAX_SECONDS = 600L;

    @Autowired
    private MqOutboxMapper mqOutboxMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${mall.order.outbox-scan-interval-ms:5000}",
            initialDelayString = "${mall.order.outbox-initial-delay-ms:10000}")
    public void deliverPending() {
        List<MqOutbox> pending = mqOutboxMapper.selectPending(LocalDateTime.now(), BATCH_SIZE);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        // 两阶段：先全部发出去，再统一等 confirm。
        // 不能"发一条等一条"——那样 100 条消息的等待时间会累加成 100 × 超时，
        // 而 confirm 本身是可以流水线到达的，统一等待的总耗时约等于最慢的那一条。
        List<CorrelationData> inFlight = new ArrayList<>(pending.size());
        List<MqOutbox> inFlightMsgs = new ArrayList<>(pending.size());

        for (MqOutbox msg : pending) {
            CorrelationData correlationData = new CorrelationData(String.valueOf(msg.getId()));
            try {
                rabbitTemplate.send(msg.getExchangeName(), msg.getRoutingKey(), buildMessage(msg), correlationData);
                inFlight.add(correlationData);
                inFlightMsgs.add(msg);
            } catch (Exception e) {
                // 发送阶段就失败（连接断开等）：这条不进 inFlight，直接安排重试
                scheduleRetry(msg, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        int sent = 0;
        for (int i = 0; i < inFlight.size(); i++) {
            MqOutbox msg = inFlightMsgs.get(i);
            if (awaitConfirm(msg, inFlight.get(i))) {
                sent++;
            }
        }
        log.info("outbox 投递完成，本批 {} 条，发送 {} 条，确认成功 {} 条",
                pending.size(), inFlight.size(), sent);
    }

    /**
     * 构造要发送的消息。
     * <p>
     * 关键：payload 入库时已经是 JSON 字符串，必须走 {@code send} 发原始 Message，
     * 不能用 {@code convertAndSend}——后者会经 {@link org.springframework.amqp.support.converter.Jackson2JsonMessageConverter}
     * 把字符串再序列化一次，变成带引号的 JSON 字符串字面量，消费端拿到的就不是对象了。
     */
    private Message buildMessage(MqOutbox msg) {
        return MessageBuilder
                .withBody(msg.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(String.valueOf(msg.getId()))
                .build();
    }

    /**
     * 等待 Broker 确认。
     *
     * @return true 表示 ack 并已置为已投递
     */
    private boolean awaitConfirm(MqOutbox msg, CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (confirm != null && confirm.isAck()) {
                mqOutboxMapper.markSent(msg.getId());
                return true;
            }
            String reason = confirm == null ? "confirm 为空" : confirm.getReason();
            scheduleRetry(msg, "Broker nack: " + reason);
            return false;

        } catch (InterruptedException e) {
            // 任务线程被要求停止：不标记失败，记录仍是 INIT，下一轮会重发（消费端幂等兜底）
            Thread.currentThread().interrupt();
            log.warn("outbox 等待确认被中断，留待下一轮, id={}", msg.getId());
            return false;
        } catch (Exception e) {
            // 含 TimeoutException：confirm 没在超时内到达，保守地按失败重试。
            // 这可能导致重复投递，由消费端幂等吸收——宁可重复，不可丢失。
            scheduleRetry(msg, "等待 confirm 失败: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private void scheduleRetry(MqOutbox msg, String error) {
        int attempt = msg.getRetryCount() == null ? 0 : msg.getRetryCount();
        long delaySeconds = Math.min(BACKOFF_BASE_SECONDS << Math.min(attempt, 10), BACKOFF_MAX_SECONDS);
        LocalDateTime next = LocalDateTime.now().plusSeconds(delaySeconds);

        String trimmed = error != null && error.length() > 500 ? error.substring(0, 500) : error;
        mqOutboxMapper.markRetry(msg.getId(), MAX_RETRY, next, trimmed);

        if (attempt + 1 >= MAX_RETRY) {
            log.error("outbox 消息重试达上限，已置为 FAILED 需人工介入, id={}, bizKey={}, error={}",
                    msg.getId(), msg.getBizKey(), trimmed);
        } else {
            log.warn("outbox 投递失败，{}s 后重试, id={}, attempt={}, error={}",
                    delaySeconds, msg.getId(), attempt + 1, trimmed);
        }
    }
}
