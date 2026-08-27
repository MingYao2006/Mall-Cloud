package com.mall.search.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private Long categoryId;
    
    private String brand;
    
    private String mainImage;
    
    @TableField("detail")
    private String description;
    
    private BigDecimal price;
    
    private BigDecimal originalPrice;
    
    private Integer stock;
    
    private Integer sales;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
