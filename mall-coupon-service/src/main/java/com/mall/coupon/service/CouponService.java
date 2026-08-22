package com.mall.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.coupon.entity.Coupon;
import com.mall.coupon.mapper.CouponMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CouponService {

    private final RedisService redisService;
    private final CacheService cacheService;
    private final CouponMapper couponMapper;

    public CouponService(RedisService redisService, CacheService cacheService, CouponMapper couponMapper) {
        this.redisService = redisService;
        this.cacheService = cacheService;
        this.couponMapper = couponMapper;
    }

    /**
     * 获取优惠券列表（带缓存）
     */
    public List<Coupon> getCouponsList() {
        String cacheKey = "coupon:active:list";
        return cacheService.getOrLoad(cacheKey, () ->
                        couponMapper.selectList(
                                new LambdaQueryWrapper<Coupon>()
                                        .eq(Coupon::getStatus, 1)
                                        .gt(Coupon::getEndTime, LocalDateTime.now())
                                        .orderByDesc(Coupon::getCreateTime)
                        ),
                30, TimeUnit.MINUTES);
    }

    /**
     * 获取优惠券详情（带缓存）
     */
    public Coupon getCouponDetail(Long couponId) {
        String cacheKey = "coupon:detail:" + couponId;
        return cacheService.getOrLoad(cacheKey, () -> {
            return couponMapper.selectById(couponId);
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 清除优惠券缓存
     */
    public void deleteCouponCache(Long couponId) {
        String cacheKey = "coupon:detail:" + couponId;
        cacheService.delete(cacheKey);
    }
}