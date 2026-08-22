package com.mall.coupon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统一缓存服务
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache = 
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private static final Random random = new Random();

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            localCache.put(key, value);
            logger.debug("缓存设置成功: {}", key);
        } catch (Exception e) {
            logger.error("缓存设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            localCache.put(key, value);
            logger.debug("缓存设置成功: {}, 过期时间: {} {}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("缓存设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    public void setWithRandomExpire(String key, Object value, long baseTimeout, TimeUnit unit, long randomRange) {
        try {
            long randomTimeout = baseTimeout + (long) (Math.random() * randomRange);
            redisTemplate.opsForValue().set(key, value, randomTimeout, unit);
            localCache.put(key, value);
            logger.debug("缓存设置成功（随机过期）: {}, 过期时间: {} {}", key, randomTimeout, unit);
        } catch (Exception e) {
            logger.error("缓存设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            Object value = localCache.getIfPresent(key);
            if (value != null) {
                logger.debug("从本地缓存获取: {}", key);
                return value;
            }
            value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                localCache.put(key, value);
                logger.debug("从Redis缓存获取: {}", key);
            }
            return value;
        } catch (Exception e) {
            logger.error("缓存获取失败: {}, 错误: {}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            localCache.invalidate(key);
            logger.debug("缓存删除成功: {}", key);
        } catch (Exception e) {
            logger.error("缓存删除失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    public <T> T getOrLoad(String key, Supplier<T> loader, long timeout, TimeUnit unit) {
        try {
            Object cachedValue = get(key);
            if (cachedValue != null) {
                @SuppressWarnings("unchecked")
                T result = (T) cachedValue;
                return result;
            }
            T value = loader.get();
            if (value != null) {
                set(key, value, timeout, unit);
            }
            return value;
        } catch (Exception e) {
            logger.error("缓存加载失败: {}, 错误: {}", key, e.getMessage());
            return loader.get();
        }
    }
}