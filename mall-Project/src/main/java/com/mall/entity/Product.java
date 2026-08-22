package com.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 对应数据库表：product
 */
@Data
@TableName("product")
public class Product {
    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
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
     * 商品主图
     */
    private String mainImage;

    /**
     * 副图URL，多个用逗号分隔
     */
    private String subImages;

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
     * 商品详情（富文本）
     */
    private String detail;

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
     * 逻辑删除标识
     * 0-未删除
     * 1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}
