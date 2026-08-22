package com.mall.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.search.entity.Product;
import com.mall.search.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductSearchService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 搜索商品
     */
    public List<Product> searchProducts(String keyword) {
        // 从数据库搜索
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Product::getName, keyword)
                .or()
                .like(Product::getBrand, keyword)
                .or()
                .like(Product::getDescription, keyword);
        
        List<Product> products = productMapper.selectList(queryWrapper);
        
        // 缓存搜索结果
        String cacheKey = "search:" + keyword;
        redisTemplate.opsForValue().set(cacheKey, products, 30, TimeUnit.MINUTES);
        
        return products;
    }

    /**
     * 按分类搜索商品
     */
    public List<Product> searchByCategory(Long categoryId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCategoryId, categoryId);
        
        return productMapper.selectList(queryWrapper);
    }

    /**
     * 按品牌搜索商品
     */
    public List<Product> searchByBrand(String brand) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Product::getBrand, brand);
        
        return productMapper.selectList(queryWrapper);
    }
}