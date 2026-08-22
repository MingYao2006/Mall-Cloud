package com.mall.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 */
@Data
@TableName("payment")
public class Payment {
    /**
     * 支付ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式：1-微信，2-支付宝，3-银行卡
     */
    private Integer paymentType;

    /**
     * 支付状态：0-待支付，1-支付成功，2-支付失败，3-已退款
     */
    private Integer status;

    /**
     * 第三方支付流水号
     */
    private String transactionId;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 备注
     */
    private String note;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}
