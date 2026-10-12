package com.mall.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券实体（Feign DTO）。
 * <p>
 * 与 mall-coupon-service 的 UserCoupon 字段保持一致，仅供管理端展示领取明细。
 * 本模块不连数据库，因此不加 MyBatis-Plus 注解。
 */
@Data
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private Long orderId;
    /** 0-未使用 1-已使用 2-已过期 */
    private Integer status;
    private LocalDateTime obtainTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
}