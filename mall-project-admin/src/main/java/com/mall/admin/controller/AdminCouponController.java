package com.mall.admin.controller;

import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.feign.CouponServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券管理控制器（B端）
 * <p>
 * 提供优惠券列表和详情查看等管理接口。
 * <p>
 * 注意：优惠券的创建/修改/删除等管理操作需在 mall-coupon-service 补充对应接口后在此添加。
 */
@Tag(name = "优惠券管理", description = "优惠券管理接口（管理端）")
@RestController
@RequestMapping("/coupon")
public class AdminCouponController {

    private final CouponServiceFeignClient couponServiceFeignClient;

    public AdminCouponController(CouponServiceFeignClient couponServiceFeignClient) {
        this.couponServiceFeignClient = couponServiceFeignClient;
    }

    @Operation(summary = "获取优惠券列表（管理端视角）")
    @GetMapping("/list")
    public Result<List<Coupon>> getCouponList() {
        return couponServiceFeignClient.getCouponList();
    }

    @Operation(summary = "获取优惠券详情")
    @GetMapping("/detail/{id}")
    public Result<Coupon> getCouponDetail(@PathVariable Long id) {
        return couponServiceFeignClient.getCouponDetail(id);
    }
}
