package com.mall.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情VO（Feign DTO）
 */
@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private Integer paymentType;
    private String paymentTypeDesc;
    private Integer status;
    private String statusDesc;
    private LocalDateTime paymentTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String note;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrderItemVO> orderItems;

    @Data
    public static class OrderItemVO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal productPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
    }
}
