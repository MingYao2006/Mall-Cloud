package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀日志实体类
 */
@Data
@TableName("seckill_log")
public class SeckillLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long couponId;
    private Long orderId;
    private Integer status; // 0-失败 1-成功 2-待处理
    private String failReason;
    private String couponName;
    private BigDecimal couponAmount;
    private LocalDateTime seckillTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}