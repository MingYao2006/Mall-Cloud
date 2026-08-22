package com.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.mapper.UserCouponMapper;
import com.mall.service.CouponService;
import com.mall.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    @GetMapping("/list")
    public Result<List<Coupon>> getCouponList() {
        List<Coupon> coupons = couponService.getCouponsList();
        return Result.success(coupons);
    }

    /**
     * 获取优惠券详情
     */
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
    @PostMapping("/seckill/{couponId}")
    public Result<String> seckillCoupon(@PathVariable Long couponId, @RequestParam Long userId) {
        // 校验时间
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
     * 获取用户优惠券列表
     */
    @GetMapping("/my")
    public Result<List<UserCoupon>> getMyCoupons(@RequestParam Long userId, @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getObtainTime);

        List<UserCoupon> list = userCouponMapper.selectList(wrapper);
        return Result.success(list);
    }
}
