package com.mall.seckill;

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
    public void handleSeckillMessage(Map<String, Object> message) {
        log.info("收到秒杀信息：{}", message);
        try {
            Long userId = Long.valueOf(message.get("userId").toString());
            Long couponId = Long.valueOf(message.get("couponId").toString());
            Result<String> result = seckillService.seckillCoupon(userId, couponId);
            log.info("秒杀处理结果：{}", result.getMessage());
        } catch (Exception e) {
            log.error("处理秒杀信息失败：{}", e.getMessage());
            e.printStackTrace();
        }
    }
}
