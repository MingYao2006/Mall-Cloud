package com.mall.comment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_comment")
public class ProductComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 商品ID */
    private Long productId;
    
    /** 用户ID */
    private Long userId;
    
    /** 订单ID */
    private Long orderId;
    
    /** 评论内容 */
    private String content;
    
    /** 评分：1-5星 */
    private Integer rating;
    
    /** 评论图片（JSON数组） */
    private String images;
    
    /** 状态：0-待审核 1-已审核 2-已拒绝 */
    private Integer status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
