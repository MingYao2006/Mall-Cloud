package com.mall.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.comment.entity.ProductComment;
import com.mall.comment.mapper.ProductCommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductCommentService {

    @Autowired
    private ProductCommentMapper productCommentMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 添加评论
     */
    public void addComment(ProductComment comment) {
        comment.setStatus(0); // 待审核
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        productCommentMapper.insert(comment);
        
        // 清除该商品的评论缓存
        String cacheKey = "comments:product:" + comment.getProductId();
        redisTemplate.delete(cacheKey);
    }

    /**
     * 获取商品评论列表
     */
    public List<ProductComment> getProductComments(Long productId) {
        String cacheKey = "comments:product:" + productId;
        
        // 先从缓存获取
        List<ProductComment> comments = (List<ProductComment>) redisTemplate.opsForValue().get(cacheKey);
        if (comments != null) {
            return comments;
        }
        
        // 从数据库查询
        LambdaQueryWrapper<ProductComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductComment::getProductId, productId)
                .eq(ProductComment::getStatus, 1) // 只查询已审核的
                .orderByDesc(ProductComment::getCreateTime);
        
        comments = productCommentMapper.selectList(queryWrapper);
        
        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, comments, 30, TimeUnit.MINUTES);
        
        return comments;
    }

    /**
     * 审核评论
     */
    public void auditComment(Long commentId, Integer status) {
        ProductComment comment = productCommentMapper.selectById(commentId);
        if (comment != null) {
            comment.setStatus(status);
            comment.setUpdateTime(LocalDateTime.now());
            productCommentMapper.updateById(comment);
            
            // 清除该商品的评论缓存
            String cacheKey = "comments:product:" + comment.getProductId();
            redisTemplate.delete(cacheKey);
        }
    }

    /**
     * 获取用户评论列表
     */
    public List<ProductComment> getUserComments(Long userId) {
        LambdaQueryWrapper<ProductComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductComment::getUserId, userId)
                .orderByDesc(ProductComment::getCreateTime);
        
        return productCommentMapper.selectList(queryWrapper);
    }
}