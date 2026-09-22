package com.mall.coupon.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.coupon.entity.Coupon;
import com.mall.coupon.entity.UserCoupon;
import com.mall.coupon.mapper.UserCouponMapper;
import com.mall.coupon.service.CouponService;
import com.mall.coupon.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全说明：
 * userId 统一通过 @RequestHeader("X-User-Id") 从网关注入的请求头获取，
 * 不再接收前端明文传入，防止越权操作。
 */
@Tag(name = "优惠券服务", description = "优惠券相关接口")
@RestController
@RequestMapping("/coupon")
public class CouponController {
    @Autowired
    private SeckillService seckillService;

    @Autowired
    private CouponService couponService;
    @Autowired
    private UserCouponMapper userCouponMapper;

    /**
     * 获取优惠券列表
     */
    @Operation(summary = "获取优惠券列表")
    @GetMapping("/list")
    public Result<List<Coupon>> getCouponList() {
        List<Coupon> coupons = couponService.getCouponsList();
        return Result.success(coupons);
    }

    /**
     * 获取优惠券详情
     */
    @Operation(summary = "获取优惠券详情")
    @GetMapping("/detail/{id}")
    public Result<Coupon> getCouponDetail(@PathVariable Long id) {
        Coupon coupon = couponService.getCouponDetail(id);
        if (coupon == null) {
            throw new BusinessException(500, "优惠券不存在");
        }
        return Result.success(coupon);
    }

    /**
     * 秒杀抢券
     */
    @Operation(summary = "秒杀抢券")
    @PostMapping("/seckill/{couponId}")
    public Result<String> seckillCoupon(@PathVariable Long couponId,
                                        @RequestHeader("X-User-Id") Long userId) {
        Coupon coupon = couponService.getCouponDetail(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException(500, "活动不存在或已结束");
        }
        if (LocalDateTime.now().isBefore(coupon.getStartTime())) {
            throw new BusinessException(500, "活动未开始");
        }
        if (LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new BusinessException(500, "活动已结束");
        }

        return seckillService.seckillCoupon(userId, couponId);
    }

    /**
     * 获取用户优惠券列表（关联优惠券详情）
     * 聚合服务（mall-Project）通过 Feign 直接代理本接口，
     * 关联查询逻辑统一下沉到优惠券微服务，避免重复实现。
     */
    @Operation(summary = "获取用户优惠券列表")
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyCoupons(@RequestHeader("X-User-Id") Long userId,
                                                          @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getObtainTime);

        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);

        // 关联查询优惠券详细信息
        List<Map<String, Object>> result = userCoupons.stream().map(uc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", uc.getId());
            map.put("userId", uc.getUserId());
            map.put("couponId", uc.getCouponId());
            map.put("status", uc.getStatus());
            map.put("obtainTime", uc.getObtainTime());
            map.put("useTime", uc.getUseTime());
            map.put("expireTime", uc.getExpireTime());

            Coupon coupon = couponService.getCouponDetail(uc.getCouponId());
            if (coupon != null) {
                map.put("name", coupon.getName());
                map.put("couponName", coupon.getName());
                map.put("value", coupon.getValue());
                map.put("couponValue", coupon.getValue());
                map.put("minAmount", coupon.getMinAmount());
                map.put("type", coupon.getType());
            }

            return map;
        }).collect(java.util.stream.Collectors.toList());

        return Result.success(result);
    }
}