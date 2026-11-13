package com.mall.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.order.entity.MqOutbox;
import com.mall.order.mapper.MqOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 本地消息表入队入口。
 * <p>
 * 调用方必须处在业务事务内（{@code PROPAGATION_REQUIRED}）：
 * 只有和业务写操作共享同一个事务，"业务落库"与"消息入队"才是原子的——
 * 业务回滚时这条 outbox 记录一起回滚，业务提交时它一定在。
 * <p>
 * 真正的投递由 {@link com.mall.order.task.OutboxSendTask} 在事务提交后异步完成，
 * 所以这里不碰 RabbitTemplate，也不会有"发送失败导致业务回滚"的问题。
 */
@Slf4j
@Service
public class OutboxService {

    @Autowired
    private MqOutboxMapper mqOutboxMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 在业务事务内登记一条待投递消息。
     *
     * @param bizKey     业务幂等键，如 {@code order:12345:cancel-resource}。
     *                   与 exchange 一起构成唯一索引，重复登记会被静默忽略。
     * @param exchange   目标交换机
     * @param routingKey 路由键
     * @param payload    消息体对象，会被序列化成 JSON 存库
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void enqueue(String bizKey, String exchange, String routingKey, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // 序列化失败说明消息体本身有问题，属于编码错误，应当立刻暴露而不是静默丢消息
            throw new IllegalStateException("outbox 消息序列化失败, bizKey=" + bizKey, e);
        }

        int rows = mqOutboxMapper.insertIgnore(bizKey, exchange, routingKey, json);
        if (rows == 0) {
            log.info("outbox 消息已存在，跳过重复登记, bizKey={}", bizKey);
        } else {
            log.debug("outbox 消息已登记, bizKey={}, exchange={}", bizKey, exchange);
        }
    }

    /**
     * 是否已成功投递（供需要判断"这条消息有没有出去"的场景使用）。
     */
    public boolean isSent(String bizKey, String exchange) {
        Long count = mqOutboxMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MqOutbox>()
                        .eq(MqOutbox::getBizKey, bizKey)
                        .eq(MqOutbox::getExchangeName, exchange)
                        .eq(MqOutbox::getStatus, MqOutbox.STATUS_SENT));
        return count != null && count > 0;
    }
}
