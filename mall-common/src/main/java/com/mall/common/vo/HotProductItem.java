package com.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品热度排行数据项
 */
@Data
public class HotProductItem {

    private Long productId;

    private String productName;

    private String productImage;

    /** 累计销量（件） */
    private Long quantity;

    /** 累计销售额 */
    private BigDecimal amount;
}
