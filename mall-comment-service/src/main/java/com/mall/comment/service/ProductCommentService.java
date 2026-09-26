package com.mall.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.comment.entity.ProductComment;
import com.mall.comment.mapper.ProductCommentMapper;
import com.mall.common.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 管理端分页查询：status 为空表示不限状态
     */
    public PageResult pageComments(Integer status, Long productId, Long userId, Integer pageNum, Integer pageSize) {
        long current = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        long size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
        Page<ProductComment> page = new Page<>(current, size);

        LambdaQueryWrapper<ProductComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, ProductComment::getStatus, status)
                .eq(productId != null, ProductComment::getProductId, productId)
                .eq(userId != null, ProductComment::getUserId, userId)
                .orderByDesc(ProductComment::getCreateTime);

        IPage<ProductComment> result = productCommentMapper.selectPage(page, wrapper);
        return new PageResult((int) result.getCurrent(), (int) result.getSize(), result.getTotal(), result.getRecords());
    }

    /**
     * 按状态统计数量，key 为 "0"/"1"/"2"，用于 tab 徽标
     */
    public Map<String, Long> countByStatus(Long productId, Long userId) {
        Map<String, Long> counts = new HashMap<>();
        for (int s = 0; s <= 2; s++) {
            LambdaQueryWrapper<ProductComment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductComment::getStatus, s)
                    .eq(productId != null, ProductComment::getProductId, productId)
                    .eq(userId != null, ProductComment::getUserId, userId);
            counts.put(String.valueOf(s), productCommentMapper.selectCount(wrapper));
        }
        return counts;
    }
}