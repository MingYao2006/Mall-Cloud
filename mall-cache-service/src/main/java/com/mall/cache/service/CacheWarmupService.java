package com.mall.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CacheWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 缓存预热 - 商品信息
     */
    public void warmupProductCache(Long productId, Object productData) {
        String cacheKey = "product:" + productId;
        redisTemplate.opsForValue().set(cacheKey, productData, 1, TimeUnit.HOURS);
        logger.info("商品缓存预热完成: productId={}", productId);
    }

    /**
     * 缓存预热 - 分类信息
     */
    public void warmupCategoryCache(Long categoryId, Object categoryData) {
        String cacheKey = "category:" + categoryId;
        redisTemplate.opsForValue().set(cacheKey, categoryData, 2, TimeUnit.HOURS);
        logger.info("分类缓存预热完成: categoryId={}", categoryId);
    }

    /**
     * 批量缓存预热
     */
    public void warmupBatch(String keyPrefix, java.util.Map<Long, Object> dataMap, long expireHours) {
        for (java.util.Map.Entry<Long, Object> entry : dataMap.entrySet()) {
            String cacheKey = keyPrefix + entry.getKey();
            redisTemplate.opsForValue().set(cacheKey, entry.getValue(), expireHours, TimeUnit.HOURS);
        }
        logger.info("批量缓存预热完成: keyPrefix={}, count={}", keyPrefix, dataMap.size());
    }

    /**
     * 清除缓存
     */
    public void clearCache(String keyPattern) {
        java.util.Set<String> keys = redisTemplate.keys(keyPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("缓存清除完成: keyPattern={}, count={}", keyPattern, keys.size());
        }
    }
}