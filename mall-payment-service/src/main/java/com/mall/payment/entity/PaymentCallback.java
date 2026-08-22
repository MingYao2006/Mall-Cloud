package com.mall.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调记录实体类
 */
@Data
@TableName("payment_callback")
public class PaymentCallback {
    /**
     * 回调ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付记录ID
     */
    private Long paymentId;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 第三方支付流水号
     */
    private String transactionId;

    /**
     * 回调状态：1-成功，2-失败
     */
    private Integer callbackStatus;

    /**
     * 回调金额
     */
    private BigDecimal callbackAmount;

    /**
     * 回调时间
     */
    private LocalDateTime callbackTime;

    /**
     * 回调原始数据
     */
    private String callbackData;

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
