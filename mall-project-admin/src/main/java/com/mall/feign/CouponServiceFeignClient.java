package com.mall.feign;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 优惠券服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-coupon-service 微服务，
 * 覆盖优惠券的查询、创建、修改、上下架、库存调整、删除及领取统计等管理操作。
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

    @GetMapping("/page")
    Result<PageResult> pageCoupons(@RequestParam Map<String, Object> params);

    @PostMapping("/create")
    Result<Object> createCoupon(@RequestBody Coupon coupon);

    @PutMapping("/update")
    Result<Object> updateCoupon(@RequestBody Coupon coupon);

    @PutMapping("/status/{id}")
    Result<Object> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status);

    @PutMapping("/stock/{id}")
    Result<Object> adjustStock(@PathVariable("id") Long id, @RequestParam("totalStock") Integer totalStock);

    @DeleteMapping("/{id}")
    Result<Object> deleteCoupon(@PathVariable("id") Long id);

    @GetMapping("/stats/{id}")
    Result<Map<String, Object>> getStats(@PathVariable("id") Long id);

    @GetMapping("/records/{id}")
    Result<List<UserCoupon>> getClaimRecords(@PathVariable("id") Long id);
}
