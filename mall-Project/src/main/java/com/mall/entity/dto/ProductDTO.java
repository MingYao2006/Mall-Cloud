package com.mall.entity.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品列表展示对象
 * 用于前端商品列表展示，包含分类名称等关联信息
 */
@Data
public class ProductDTO {
    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 市场价
     */
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 商品状态
     * 0-下架
     * 1-上架
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;
}
