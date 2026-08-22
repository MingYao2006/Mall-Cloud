package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
}