package com.mall.controller;

import com.mall.common.Result;
import com.mall.feign.SeckillServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀服务 Controller - 微服务整合版
 * <p>
 * 本 Controller 不再直接实现秒杀逻辑，统一通过 Feign 调用 mall-seckill-service 微服务。
 * userId 统一通过 @RequestAttribute("userId") 从 JwtInterceptor 注入的属性获取，
 * 不再接收前端明文传入，防止越权操作。
 */
@RestController
@RequestMapping("/seckill")
@Tag(name = "秒杀服务", description = "秒杀相关接口（聚合服务代理）")
public class SeckillController {

    private final SeckillServiceFeignClient seckillService;

    public SeckillController(SeckillServiceFeignClient seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 测试接口
     * GET /seckill/test
     */
    @GetMapping("/test")
    @Operation(summary = "测试接口")
    public Result<String> test() {
        return seckillService.test();
    }

    /**
     * 秒杀抢券
     * POST /seckill/coupon/{couponId}
     */
    @PostMapping("/coupon/{couponId}")
    @Operation(summary = "秒杀抢券")
    public Result<String> seckillCoupon(@RequestAttribute("userId") Long userId,
                                        @PathVariable("couponId") Long couponId) {
        return seckillService.seckillCoupon(userId, couponId);
    }

    /**
     * 秒杀抢券（GET 方式）
     * GET /seckill/doSeckill?couponId=xx
     */
    @GetMapping("/doSeckill")
    @Operation(summary = "秒杀抢券（GET方式）")
    public Result<String> doSeckill(@RequestAttribute("userId") Long userId,
                                    @RequestParam("couponId") Long couponId) {
        return seckillService.doSeckill(userId, couponId);
    }
}
