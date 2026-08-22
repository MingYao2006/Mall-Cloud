package com.mall.seckill.feign;

import com.mall.common.Result;
import com.mall.seckill.entity.Coupon;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券服务 Feign 客户端
 */
@FeignClient(name = "mall-coupon-service", path = "/coupon")
public interface CouponServiceFeignClient {

    /**
     * 获取优惠券列表
     */
    @GetMapping("/list")
    Result<List<Coupon>> getCouponList();

    /**
     * 获取优惠券详情
     */
    @GetMapping("/detail/{id}")
    Result<Coupon> getCouponDetail(@PathVariable("id") Long id);

    /**
     * 扣减优惠券库存
     */
    @PostMapping("/deduct/{id}")
    Result<Void> deductStock(@PathVariable("id") Long id);

    /**
     * 回滚优惠券库存
     */
    @PostMapping("/rollback/{id}")
    Result<Void> rollbackStock(@PathVariable("id") Long id);
}
