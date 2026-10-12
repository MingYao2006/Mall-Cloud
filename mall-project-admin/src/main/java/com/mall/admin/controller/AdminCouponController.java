package com.mall.admin.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.feign.CouponServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优惠券管理控制器（B 端）。
 * <p>
 * 本类只做转发：业务校验、缓存失效、抢券库存同步都在 mall-coupon-service，
 * 状态语义统一由 com.mall.common.CouponStatus 计算后随 Coupon 一起返回。
 * 鉴权由 AdminJwtInterceptor 统一校验 role=1，这里不用重复写。
 */
@Tag(name = "优惠券管理", description = "优惠券管理接口（管理端）")
@RestController
@RequestMapping("/coupon")
public class AdminCouponController {

    private final CouponServiceFeignClient couponServiceFeignClient;

    public AdminCouponController(CouponServiceFeignClient couponServiceFeignClient) {
        this.couponServiceFeignClient = couponServiceFeignClient;
    }

    @Operation(summary = "获取优惠券列表（仅上架且未过期，兼容旧页面）")
    @GetMapping("/list")
    public Result<List<Coupon>> getCouponList() {
        return couponServiceFeignClient.getCouponList();
    }

    @Operation(summary = "获取优惠券详情")
    @GetMapping("/detail/{id}")
    public Result<Coupon> getCouponDetail(@PathVariable Long id) {
        return couponServiceFeignClient.getCouponDetail(id);
    }

    @Operation(summary = "分页查询优惠券（管理端，含未上架 / 已停用 / 已过期）")
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) Integer type,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        // Feign 用 Map 传 query，只放非空值，避免 null 被拼进请求 URL
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        if (status != null) {
            params.put("status", status);
        }
        if (type != null) {
            params.put("type", type);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            params.put("keyword", keyword.trim());
        }
        return couponServiceFeignClient.pageCoupons(params);
    }

    @Operation(summary = "新增优惠券（默认未上架，确认无误再上架）")
    @PostMapping("/create")
    public Result<Object> create(@RequestBody Coupon coupon) {
        return couponServiceFeignClient.createCoupon(coupon);
    }

    @Operation(summary = "修改优惠券（不含库存，库存走 /stock/{id}）")
    @PutMapping("/update")
    public Result<Object> update(@RequestBody Coupon coupon) {
        return couponServiceFeignClient.updateCoupon(coupon);
    }

    @Operation(summary = "上架 / 下架 / 停用：status 0-未上架 1-已上架 2-已停用")
    @PutMapping("/status/{id}")
    public Result<Object> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return couponServiceFeignClient.updateStatus(id, status);
    }

    @Operation(summary = "调整总库存（会同步清理抢券用的 Redis 库存）")
    @PutMapping("/stock/{id}")
    public Result<Object> adjustStock(@PathVariable Long id, @RequestParam Integer totalStock) {
        return couponServiceFeignClient.adjustStock(id, totalStock);
    }

    @Operation(summary = "删除优惠券（已有领取记录时会被拒绝）")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        return couponServiceFeignClient.deleteCoupon(id);
    }

    @Operation(summary = "领取 / 使用统计")
    @GetMapping("/stats/{id}")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return couponServiceFeignClient.getStats(id);
    }

    @Operation(summary = "领取明细")
    @GetMapping("/records/{id}")
    public Result<List<UserCoupon>> records(@PathVariable Long id) {
        return couponServiceFeignClient.getClaimRecords(id);
    }
}