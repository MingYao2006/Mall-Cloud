package com.mall.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 搜索商品实体（DTO）
 * <p>
 * 用于聚合服务通过 Feign 调用 mall-search-service 时，
 * 反序列化 Elasticsearch 中返回的商品搜索结果。
 * 字段与搜索微服务 com.mall.search.entity.EsProduct 保持一致。
 */
@Data
public class EsProduct {

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
     * 品牌
     */
    private String brand;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 销量
     */
    private Integer sales;
}
