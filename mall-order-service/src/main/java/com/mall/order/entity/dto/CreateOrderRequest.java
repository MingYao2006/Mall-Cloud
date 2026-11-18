package com.mall.order.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求DTO
 */
@Data
public class CreateOrderRequest {
    /**
     * 购物车项ID列表（从购物车下单）
     */
    private List<Long> cartItemIds;

    /**
     * 商品ID（直接购买）
     */
    private Long productId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 购买数量（直接购买）
     */
    private Integer quantity;

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;

    /**
     * 订单备注
     */
    private String note;

    /**
     * 支付方式：1-微信，2-支付宝，3-银行卡
     */
    private Integer paymentType;

    /**
     * 是否秒杀订单（直接购买时生效）。
     * 为 true 时订单服务用 SeckillPriceUtil 按商品 ID 重算秒杀价，
     * 前端不传价格，避免篡改；false / null 走普通原价。
     */
    private Boolean seckill;
}
