package com.mall.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 搜索商品实体（Feign DTO）
 * 与 mall-search-service 的 EsProduct 实体保持一致。
 */
@Data
public class EsProduct {
    private Long id;
    private String name;
    private Long categoryId;
    private String brand;
    private String description;
    private BigDecimal price;
    private String mainImage;
    private Integer sales;
}
