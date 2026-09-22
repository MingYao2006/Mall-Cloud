package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.Coupon;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-coupon-service 微服务，
 * 用于查看优惠券列表和详情（管理端视角）。
 * <p>
 * 注意：优惠券的创建/修改/删除等管理操作需在 mall-coupon-service 补充对应接口后，
 * 在此客户端中添加对应方法。
 */
@FeignClient(
        name = "mall-coupon-service",
        contextId = "couponServiceFeignClient",
        path = "/coupon"
)
public interface CouponServiceFeignClient {

    @GetMapping("/list")
    Result<List<Coupon>> getCouponList();

    @GetMapping("/detail/{id}")
    Result<Coupon> getCouponDetail(@PathVariable("id") Long id);
}
