package com.mall.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车VO类（用于前端展示）
 */
@Data
public class CartVO {
    /**
     * 购物车ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 是否选中（0-未选中 1-选中）
     */
    private Integer selected;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品主图
     */
    private String productImage;

    /**
     * 商品价格
     */
    private BigDecimal productPrice;

    /**
     * 商品库存
     */
    private Integer productStock;

    /**
     * 商品状态
     */
    private Integer productStatus;

    /**
     * 小计金额（单价 × 数量）
     */
    private BigDecimal totalPrice;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
