package com.mall.entity;

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
    
    /** 用户ID */
    private Long userId;
    
    /** 优惠券ID */
    private Long couponId;
    
    /** 订单ID（如果有） */
    private Long orderId;
    
    /** 秒杀状态：0-失败 1-成功 2-待处理 */
    private Integer status;
    
    /** 失败原因 */
    private String failReason;
    
    /** 优惠券名称 */
    private String couponName;
    
    /** 优惠券金额 */
    private BigDecimal couponAmount;
    
    /** 秒杀开始时间 */
    private LocalDateTime seckillTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}