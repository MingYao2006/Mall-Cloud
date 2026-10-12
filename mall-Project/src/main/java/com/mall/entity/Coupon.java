package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type; // 1-满减 2-折扣 3-无门槛
    private BigDecimal value;
    private BigDecimal minAmount;
    private Integer totalStock;
    private Integer availableStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    @Version
    private Integer version; // 乐观锁
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /* ===== 以下为派生字段，不落库；由 mall-coupon-service 用 CouponStatus 计算后返回 ===== */
    @TableField(exist = false)
    private Integer displayStatus;
    @TableField(exist = false)
    private String displayStatusText;
    @TableField(exist = false)
    private String displayStatusType;
}
