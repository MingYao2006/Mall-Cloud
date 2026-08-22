package com.mall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存服务
 * 
 * 实现本地缓存 + Redis 缓存的两级缓存架构
 * 
 * 工作流程：
 * 1. 先查本地缓存（Caffeine）
 * 2. 本地缓存未命中，查 Redis 缓存
 * 3. Redis 缓存未命中，查数据库
 * 4. 查询结果依次写入 Redis 和本地缓存
 * 
 * 优点：
 * 1. 提高读取性能：本地缓存命中率高，减少网络开销
 * 2. 防止缓存雪崩：两级缓存过期时间不同
 * 3. 提高可用性：Redis 故障时仍可使用本地缓存
 */
@Service
public class MultiLevelCacheService {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 本地缓存（使用 Caffeine 或简单的 ConcurrentHashMap）
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache = 
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)  // 本地缓存 10 分钟过期
            .build();

    private static final Random random = new Random();

    /**
     * 多级缓存查询
     * 
     * @param key 缓存键
     * @param loader 数据加载器（当缓存未命中时调用，lambda表达式）
     * @param redisTimeout Redis 缓存过期时间（分钟）
     * @return 缓存数据
     */
    public <T> T get(String key, Supplier<T> loader, int redisTimeout) {
        // 1. 查询本地缓存
        T value = (T) localCache.getIfPresent(key);
        if (value != null) {
            logger.debug("本地缓存命中: {}", key);
            return value;
        }

        // 2. 查询 Redis 缓存
        try {
            value = (T) redisTemplate.opsForValue().get(key);
            if (value != null) {
                logger.debug("Redis 缓存命中: {}", key);
                // 写入本地缓存
                localCache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            logger.error("Redis 查询失败: {}", e.getMessage());
        }

        // 3. 缓存未命中，加载数据
        logger.debug("缓存未命中，加载数据: {}", key);
        value = loader.get();

        if (value != null) {
            // 4. 写入 Redis 缓存（带随机过期时间）
            try {
                int randomExpire = redisTimeout + random.nextInt(10);  // 增加随机时间
                redisTemplate.opsForValue().set(key, value, randomExpire, TimeUnit.MINUTES);
            } catch (Exception e) {
                logger.error("Redis 写入失败: {}", e.getMessage());
            }

            // 5. 写入本地缓存
            localCache.put(key, value);
        }

        return value;
    }

    /**
     * 多级缓存查询（带空值防护，防止缓存击穿）
     * 
     * @param key 缓存键
     * @param loader 数据加载器
     * @param redisTimeout Redis 缓存过期时间（分钟）
     * @param nullTimeout 空值缓存过期时间（分钟）
     * @return 缓存数据
     */
    public <T> T getWithNullProtection(String key, Supplier<T> loader, int redisTimeout, int nullTimeout) {
        // 1. 查询本地缓存
        T value = (T) localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }

        // 2. 检查空值缓存（防止缓存穿透）
        String nullKey = key + ":null";
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
                return null;
            }
        } catch (Exception e) {
            logger.error("Redis 查询失败: {}", e.getMessage());
        }

        // 3. 查询 Redis 缓存
        try {
            value = (T) redisTemplate.opsForValue().get(key);
            if (value != null) {
                localCache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            logger.error("Redis 查询失败: {}", e.getMessage());
        }

        // 4. 缓存未命中，加载数据
        value = loader.get();

        if (value != null) {
            // 5. 写入 Redis 缓存
            try {
                int randomExpire = redisTimeout + random.nextInt(10);
                redisTemplate.opsForValue().set(key, value, randomExpire, TimeUnit.MINUTES);
            } catch (Exception e) {
                logger.error("Redis 写入失败: {}", e.getMessage());
            }

            // 6. 写入本地缓存
            localCache.put(key, value);
        } else {
            // 7. 缓存空值（防止缓存穿透）
            try {
                redisTemplate.opsForValue().set(nullKey, "null", nullTimeout, TimeUnit.MINUTES);
            } catch (Exception e) {
                logger.error("Redis 写入空值失败: {}", e.getMessage());
            }
        }

        return value;
    }

    /**
     * 更新缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param redisTimeout Redis 缓存过期时间（分钟）
     */
    public <T> void put(String key, T value, int redisTimeout) {
        if (value != null) {
            // 1. 更新 Redis 缓存
            try {
                int randomExpire = redisTimeout + random.nextInt(10);
                redisTemplate.opsForValue().set(key, value, randomExpire, TimeUnit.MINUTES);
            } catch (Exception e) {
                logger.error("Redis 写入失败: {}", e.getMessage());
            }

            // 2. 更新本地缓存
            localCache.put(key, value);
        }
    }

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    public void evict(String key) {
        // 1. 删除本地缓存
        localCache.invalidate(key);

        // 2. 删除 Redis 缓存
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Redis 删除失败: {}", e.getMessage());
        }
    }

    /**
     * 清空所有本地缓存
     */
    public void clearLocalCache() {
        localCache.invalidateAll();
        logger.info("本地缓存已清空");
    }

    /**
     * 获取本地缓存统计信息
     */
    public String getLocalCacheStats() {
        return localCache.stats().toString();
    }
}
