package com.mall.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CacheAvalancheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheAvalancheService.class);
    private static final Random random = new Random();

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置缓存（带随机过期时间，防止缓存雪崩）
     */
    public void setCacheWithRandomExpire(String key, Object value, long baseExpireMinutes) {
        // 在基础过期时间上增加随机时间（0-20%）
        long randomExpire = baseExpireMinutes + random.nextInt((int) (baseExpireMinutes * 2 / 10));
        redisTemplate.opsForValue().set(key, value, randomExpire, TimeUnit.MINUTES);
        logger.info("设置缓存: key={}, expire={}分钟", key, randomExpire);
    }

    /**
     * 获取缓存（带互斥锁，防止缓存击穿）
     */
    public Object getCacheWithLock(String key, CacheLoader loader) {
        // 先尝试从缓存获取
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return value;
        }

        // 缓存不存在，尝试获取锁
        String lockKey = "lock:" + key;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        
        try {
            if (Boolean.TRUE.equals(locked)) {
                // 获取锁成功，从数据库加载数据
                value = loader.load();
                if (value != null) {
                    // 设置随机过期时间
                    setCacheWithRandomExpire(key, value, 30);
                }
                return value;
            } else {
                // 获取锁失败，等待一段时间后重试
                Thread.sleep(50);
                return getCacheWithLock(key, loader);
            }
        } catch (Exception e) {
            logger.error("获取缓存失败: key={}", key, e);
            return null;
        } finally {
            if (Boolean.TRUE.equals(locked)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 设置空值缓存（防止缓存穿透）
     */
    public void setNullCache(String key) {
        // 缓存空值，设置较短的过期时间
        redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
        logger.info("设置空值缓存: key={}", key);
    }

    /**
     * 判断是否为空值缓存
     */
    public boolean isNullCache(Object value) {
        return "NULL".equals(value);
    }

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    public interface CacheLoader {
        Object load();
    }
}