package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.feign.CouponServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 优惠券聚合入口
 * <p>
 * 整合架构：本控制器不再实现任何优惠券业务逻辑，
 * 全部通过 Feign 代理转发到 mall-coupon-service 微服务。
 * userId 统一通过 @RequestAttribute("userId") 从 JwtInterceptor 注入的属性获取，
 * 不再接收前端明文传入，防止越权操作。
 */
@Tag(name = "优惠券服务", description = "优惠券相关接口（聚合服务代理）")
@RestController
@RequestMapping("/coupon")
public class CouponController {

    private final CouponServiceFeignClient couponServiceFeignClient;

    public CouponController(CouponServiceFeignClient couponServiceFeignClient) {
        this.couponServiceFeignClient = couponServiceFeignClient;
    }

    /**
     * 获取优惠券列表（转发到优惠券微服务）
     */
    @Operation(summary = "获取优惠券列表")
    @GetMapping("/list")
    public Result<List<Coupon>> getCouponList() {
        return couponServiceFeignClient.getCouponList();
    }

    /**
     * 获取优惠券详情（转发到优惠券微服务）
     */
    @Operation(summary = "获取优惠券详情")
    @GetMapping("/detail/{id}")
    public Result<Coupon> getCouponDetail(@PathVariable Long id) {
        return couponServiceFeignClient.getCouponDetail(id);
    }

    /**
     * 秒杀抢券（转发到优惠券微服务）
     */
    @Operation(summary = "秒杀抢券")
    @PostMapping("/seckill/{couponId}")
    public Result<String> seckillCoupon(@PathVariable Long couponId,
                                        @RequestAttribute("userId") Long userId) {
        return couponServiceFeignClient.seckillCoupon(couponId, userId);
    }

    /**
     * 获取用户优惠券列表（转发到优惠券微服务）
     */
    @Operation(summary = "获取用户优惠券列表")
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyCoupons(@RequestAttribute("userId") Long userId,
                                                          @RequestParam(required = false) Integer status) {
        return couponServiceFeignClient.getMyCoupons(userId, status);
    }
}
