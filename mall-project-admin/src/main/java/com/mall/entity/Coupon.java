package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体类（Feign DTO）
 * 与 mall-coupon-service 的 Coupon 实体保持一致。
 */
@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal value;
    private BigDecimal minAmount;
    private Integer totalStock;
    private Integer availableStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /* ===== 以下为派生字段，不落库；由 com.mall.common.CouponStatus 统一计算，避免前后端各写一套状态语义 ===== */
    @TableField(exist = false)
    private Integer displayStatus;
    @TableField(exist = false)
    private String displayStatusText;
    @TableField(exist = false)
    private String displayStatusType;
}
