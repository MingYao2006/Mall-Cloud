package com.mall.entity.Item;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long productId;

    /** 商品名称（下单时冗余存储，防止后续商品改名） */
    private String productName;

    /** 商品主图（下单时冗余存储） */
    private String productImage;

    /** 商品单价（下单时的价格快照） */
    private BigDecimal productPrice;

    private Integer quantity;

    /** 小计金额（单价 × 数量） */
    private BigDecimal totalPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
