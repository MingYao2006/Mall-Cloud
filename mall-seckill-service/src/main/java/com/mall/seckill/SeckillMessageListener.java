package com.mall.seckill;

import com.mall.common.BusinessException;
import com.mall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeckillMessageListener {

    private static final String SECKILL_MSG_KEY = "seckill:msg:";

    @Autowired
    private SeckillService seckillService;

    @Autowired
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    /**
     * 监听秒杀队列，异步处理秒杀逻辑
     */
    @RabbitListener(queues = "seckill.queue")
    public void handleSeckillMessage(Map<String, Object> message) {
        log.info("收到秒杀信息：{}", message);
        Long userId = null;
        Long couponId = null;
        String dedupKey = null;
        String messageId = null;
        try {
            Object mid = message.get("messageId");
            messageId = mid == null ? null : mid.toString();
            if (messageId != null) {
                // 重复投递（ack 丢失、消费超时重投等）会在至少投递一次下再次进来，
                // 这里靠 messageId 抢占去重；每人限领多张时只有它能挡住同一条消息
                dedupKey = SECKILL_MSG_KEY + messageId;
                Boolean first = redisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", 24, TimeUnit.HOURS);
                if (!Boolean.TRUE.equals(first)) {
                    log.warn("秒杀消息重复投递，跳过处理: messageId={}", messageId);
                    return;
                }
            }

            userId = Long.valueOf(message.get("userId").toString());
            couponId = Long.valueOf(message.get("couponId").toString());
            seckillService.seckillCoupon(userId, couponId);
            log.info("秒杀处理成功: userId={}, couponId={}", userId, couponId);
            // 把结果写给前端轮询，否则用户点完"立即抢购"拿不到任何回执
            seckillService.saveSeckillResult(messageId, true, "恭喜，抢券成功！");
        } catch (BusinessException e) {
            // 已抢完 / 超出限领 / 活动已结束都是预期结果，确认掉即可，重试也不会变成功。
            // 但必须把失败原因写进结果，否则用户永远只看到"已提交"、不知道已经失败。
            log.warn("秒杀业务异常: userId={}, couponId={}, msg={}", userId, couponId, e.getMessage());
            seckillService.saveSeckillResult(messageId, false, e.getMessage());
        } catch (Exception e) {
            // 基础设施异常（DB / Redis 抖动）必须抛出：吞掉就等于这条抢券请求凭空消失。
            // 这里抛普通 RuntimeException，交给 yml 的 retry 配置接管（3 次，1s/2s/4s 退避）：
            // 重试耗尽后因 default-requeue-rejected=false 被 reject，再由队列的
            // x-dead-letter-exchange 投进 seckill.dlq，不会无限重投。
            //
            // 注意：不能用 AmqpRejectAndDontRequeueException —— 它的语义是"立刻拒绝、不重回队列"，
            // 会直接触发 reject 并绕过 RetryTemplate，导致 yml 里的 max-attempts 形同虚设。
            if (dedupKey != null) {
                // 让重试的那一次能重新占到位，否则去重键会把重试挡在门外
                redisTemplate.delete(dedupKey);
            }
            log.error("处理秒杀信息失败，转入重试: userId={}, couponId={}", userId, couponId, e);
            throw new RuntimeException("秒杀消费失败", e);
        }
    }
}
