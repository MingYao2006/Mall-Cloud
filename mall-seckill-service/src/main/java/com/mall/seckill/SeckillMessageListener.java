package com.mall.seckill;

import com.alibaba.fastjson2.JSON;
import com.mall.common.Result;
import com.mall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SeckillMessageListener {

    @Autowired
    private SeckillService seckillService;

    /**
     * 监听秒杀队列，异步处理秒杀逻辑
     */
    @RabbitListener(queues = "seckill.queue")
    public void handleSeckillMessage(String message) {
        log.info("收到秒杀信息：{}", message);
        // 解析消息
        try {
            Map<String, Object> handlemessage = JSON.parseObject(message, Map.class);
            Long userId = Long.valueOf(handlemessage.get("userId").toString());
            Long couponId = Long.valueOf(handlemessage.get("couponId").toString());
            // 异步处理秒杀（扣库存、生成订单等）
            Result<String> result = seckillService.seckillCoupon(userId, couponId);
            log.info("秒杀处理结果：{}", result.getMessage());
        } catch (Exception e) {
            log.error("处理秒杀信息失败：{}", e.getMessage());
            e.printStackTrace();
            // 不要抛出异常，避免消息无限重试
        }
    }
}
