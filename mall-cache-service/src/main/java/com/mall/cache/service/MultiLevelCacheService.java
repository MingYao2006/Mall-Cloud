package com.mall.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MultiLevelCacheService {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheService.class);

    // 本地缓存（Caffeine）
    private final Cache<String, Object> localCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 多级缓存获取
     * L1: 本地缓存（Caffeine）
     * L2: 分布式缓存（Redis）
     * L3: 数据库（通过loader加载）
     */
    public Object get(String key, CacheLoader loader) {
        // 1. 先从本地缓存获取
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            logger.debug("从本地缓存获取: key={}", key);
            return value;
        }

        // 2. 从Redis获取
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            logger.debug("从Redis缓存获取: key={}", key);
            // 回填本地缓存
            localCache.put(key, value);
            return value;
        }

        // 3. 从数据库加载
        if (loader != null) {
            value = loader.load();
            if (value != null) {
                logger.debug("从数据库加载: key={}", key);
                // 写入多级缓存
                put(key, value);
            }
        }

        return value;
    }

    /**
     * 多级缓存写入
     */
    public void put(String key, Object value) {
        // 写入本地缓存
        localCache.put(key, value);
        
        // 写入Redis
        redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
        
        logger.debug("写入多级缓存: key={}", key);
    }

    /**
     * 多级缓存删除
     */
    public void evict(String key) {
        // 删除本地缓存
        localCache.invalidate(key);
        
        // 删除Redis缓存
        redisTemplate.delete(key);
        
        logger.debug("删除多级缓存: key={}", key);
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCache.invalidateAll();
        logger.info("清空本地缓存");
    }

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    public interface CacheLoader {
        Object load();
    }
}