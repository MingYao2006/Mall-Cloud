package com.mall.order.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情VO
 */
@Data
public class OrderDetailVO {
    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 运费
     */
    private BigDecimal freightAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 支付方式：1-微信，2-支付宝，3-银行卡
     */
    private Integer paymentType;

    /**
     * 支付方式描述
     */
    private String paymentTypeDesc;

    /**
     * 订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消，5-已退款
     */
    private Integer status;

    /**
     * 订单状态描述
     */
    private String statusDesc;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 收货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 收货地址
     */
    private String receiverAddress;

    /**
     * 订单备注
     */
    private String note;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 订单项列表
     */
    private List<OrderItemVO> orderItems;

    /**
     * 订单项VO
     */
    @Data
    public static class OrderItemVO {
        /**
         * 订单详情ID
         */
        private Long id;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 商品主图
         */
        private String productImage;

        /**
         * 商品单价
         */
        private BigDecimal productPrice;

        /**
         * 购买数量
         */
        private Integer quantity;

        /**
         * 小计金额
         */
        private BigDecimal totalPrice;
    }
}