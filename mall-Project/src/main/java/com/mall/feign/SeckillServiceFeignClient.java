package com.mall.feign;

import com.mall.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 秒杀服务 Feign 客户端
 * <p>
 * 与商品、搜索、用户等功能的整合架构保持一致：
 * 聚合服务（mall-Project）不再直接实现秒杀逻辑，统一通过 Feign 调用 mall-seckill-service 微服务。
 */
@FeignClient(
        name = "mall-seckill-service",
        contextId = "seckillServiceFeignClient",
        path = "/seckill"
)
public interface SeckillServiceFeignClient {

    /**
     * 测试接口
     * GET /seckill/test
     */
    @GetMapping("/test")
    Result<String> test();

    /**
     * 秒杀抢券
     * POST /seckill/coupon/{couponId}
     * userId 通过 X-User-Id 请求头传递，由微服务从请求头解析
     */
    @PostMapping("/coupon/{couponId}")
    Result<String> seckillCoupon(@RequestHeader("X-User-Id") Long userId,
                                 @PathVariable("couponId") Long couponId);

    /**
     * 秒杀抢券（GET 方式）
     * GET /seckill/doSeckill?couponId=xx
     * userId 通过 X-User-Id 请求头传递，由微服务从请求头解析
     */
    @GetMapping("/doSeckill")
    Result<String> doSeckill(@RequestHeader("X-User-Id") Long userId,
                             @RequestParam("couponId") Long couponId);
}
