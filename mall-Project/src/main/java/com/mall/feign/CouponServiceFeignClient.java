package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.Coupon;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 优惠券微服务 Feign 客户端
 * <p>
 * 与商品、分类、搜索等功能的整合架构保持一致：
 * 聚合服务（mall-Project）不再直接实现优惠券逻辑，统一通过 Feign 调用 mall-coupon-service 微服务。
 * 优惠券列表、详情、秒杀抢券、我的优惠券等业务全部由优惠券微服务承担。
 */
@FeignClient(
        name = "mall-coupon-service",
        contextId = "couponServiceFeignClient",
        path = "/coupon"
)
public interface CouponServiceFeignClient {

    /**
     * 获取优惠券列表
     * GET /coupon/list
     */
    @GetMapping("/list")
    Result<List<Coupon>> getCouponList();

    /**
     * 获取优惠券详情
     * GET /coupon/detail/{id}
     */
    @GetMapping("/detail/{id}")
    Result<Coupon> getCouponDetail(@PathVariable("id") Long id);

    /**
     * 秒杀抢券
     * POST /coupon/seckill/{couponId}
     * userId 通过 X-User-Id 请求头传递，由微服务从请求头解析
     */
    @PostMapping("/seckill/{couponId}")
    Result<String> seckillCoupon(@PathVariable("couponId") Long couponId,
                                 @RequestHeader("X-User-Id") Long userId);

    /**
     * 获取用户优惠券列表（含关联的优惠券信息）
     * GET /coupon/my?status=0
     * userId 通过 X-User-Id 请求头传递，由微服务从请求头解析
     */
    @GetMapping("/my")
    Result<List<Map<String, Object>>> getMyCoupons(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestParam(value = "status", required = false) Integer status);
}
