package com.mall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.entity.Coupon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统一缓存服务
 * 
 * 提供完整的缓存管理功能，包括：
 * 1. 基础缓存操作（增删改查）
 * 2. 多级缓存支持（本地缓存 + Redis）
 * 3. 缓存穿透、击穿、雪崩防护
 * 4. 批量操作支持
 * 5. 缓存统计功能
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 本地缓存（Caffeine）
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> localCache = 
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()  // 开启统计
            .build();

    private static final Random random = new Random();

    // ==================== 基础缓存操作 ====================

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            localCache.put(key, value);
            logger.debug("缓存设置成功: {}", key);
        } catch (Exception e) {
            logger.error("缓存设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 设置缓存（带过期时间）
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            localCache.put(key, value);
            logger.debug("缓存设置成功: {}, 过期时间: {} {}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("缓存设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 设置缓存（带随机过期时间，防止缓存雪崩）
     * @param key 缓存键
     * @param value 缓存值
     * @param baseTimeout 基础过期时间
     * @param unit 时间单位
     * @param randomRange 随机范围（秒）
     */
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

    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        // 1. 先查本地缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            logger.debug("本地缓存命中: {}", key);
            return value;
        }

        // 2. 查询 Redis 缓存
        try {
            value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                logger.debug("Redis 缓存命中: {}", key);
                // 回写本地缓存
                localCache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            logger.error("Redis 查询失败: {}, 错误: {}", key, e.getMessage());
        }

        logger.debug("缓存未命中: {}", key);
        return null;
    }

    /**
     * 获取缓存（泛型版本）
     * @param key 缓存键
     * @param clazz 返回类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value != null) {
            try {
                if (clazz.isInstance(value)) {
                    return clazz.cast(value);
                }
                // 如果是 String 类型，需要反序列化
                if (value instanceof String) {
                    return objectMapper.readValue((String) value, clazz);
                }
            } catch (JsonProcessingException e) {
                logger.error("缓存反序列化失败: {}, 错误: {}", key, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 删除缓存
     * @param key 缓存键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            localCache.invalidate(key);
            logger.debug("缓存删除成功: {}", key);
            return result;
        } catch (Exception e) {
            logger.error("缓存删除失败: {}, 错误: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 批量删除缓存
     * @param keys 缓存键集合
     * @return 删除数量
     */
    public Long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            keys.forEach(localCache::invalidate);
            logger.debug("批量删除缓存成功: {} 个", count);
            return count;
        } catch (Exception e) {
            logger.error("批量删除缓存失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 根据模式删除缓存
     * @param pattern 缓存键模式（如：product:*）
     * @return 删除数量
     */
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long count = redisTemplate.delete(keys);
                keys.forEach(localCache::invalidate);
                logger.debug("模式删除缓存成功: {}, 删除 {} 个", pattern, count);
                return count;
            }
        } catch (Exception e) {
            logger.error("模式删除缓存失败: {}, 错误: {}", pattern, e.getMessage());
        }
        return 0L;
    }

    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            logger.error("判断缓存存在失败: {}, 错误: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 设置过期时间
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            logger.error("设置过期时间失败: {}, 错误: {}", key, e.getMessage());
            return false;
        }
    }

    // ==================== 高级缓存操作 ====================

    /**
     * 获取缓存（缓存未命中时自动加载）
     * @param key 缓存键
     * @param loader 数据加载器
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 缓存值
     */
    public <T> T getOrLoad(String key, Supplier<T> loader, long timeout, TimeUnit unit) {
        // 1. 尝试从缓存获取
        T value = (T) get(key);
        if (value != null) {
            return value;
        }

        // 2. 缓存未命中，加载数据
        logger.debug("缓存未命中，加载数据: {}", key);
        value = loader.get();

        // 3. 写入缓存
        if (value != null) {
            set(key, value, timeout, unit);
        }

        return value;
    }

    /**
     * 获取缓存（带空值防护，防止缓存穿透）
     * @param key 缓存键
     * @param loader 数据加载器
     * @param timeout 正常数据过期时间
     * @param nullTimeout 空值过期时间
     * @param unit 时间单位
     * @return 缓存值
     */
    public <T> T getWithNullProtection(String key, Supplier<T> loader, long timeout, long nullTimeout, TimeUnit unit) {
        // 1. 检查空值标记
        String nullKey = key + ":null";
        if (Boolean.TRUE.equals(hasKey(nullKey))) {
            logger.debug("命中空值缓存: {}", key);
            return null;
        }

        // 2. 尝试从缓存获取
        T value = (T) get(key);
        if (value != null) {
            return value;
        }

        // 3. 加载数据
        value = loader.get();

        // 4. 写入缓存
        if (value != null) {
            set(key, value, timeout, unit);
        } else {
            // 缓存空值，防止缓存穿透
            set(nullKey, "NULL", nullTimeout, unit);
            logger.debug("缓存空值: {}", nullKey);
        }

        return value;
    }

    /**
     * 获取缓存（带随机过期时间，防止缓存雪崩）
     * @param key 缓存键
     * @param loader 数据加载器
     * @param baseTimeout 基础过期时间
     * @param randomRange 随机范围
     * @param unit 时间单位
     * @return 缓存值
     */
    public <T> T getWithRandomExpire(String key, Supplier<T> loader, long baseTimeout, long randomRange, TimeUnit unit) {
        T value = (T) get(key);
        if (value != null) {
            return value;
        }

        value = loader.get();
        if (value != null) {
            setWithRandomExpire(key, value, baseTimeout, unit, randomRange);
        }

        return value;
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 字段
     */
    public void hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            logger.debug("Hash 设置成功: {} -> {}", key, field);
        } catch (Exception e) {
            logger.error("Hash 设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 获取 Hash 字段
     */
    public Object hGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            logger.error("Hash 获取失败: {}, 错误: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有 Hash 字段
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            logger.error("Hash 获取所有字段失败: {}, 错误: {}", key, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 删除 Hash 字段
     */
    public Long hDelete(String key, Object... fields) {
        try {
            return redisTemplate.opsForHash().delete(key, fields);
        } catch (Exception e) {
            logger.error("Hash 删除失败: {}, 错误: {}", key, e.getMessage());
            return 0L;
        }
    }

    // ==================== 自增自减操作 ====================

    /**
     * 自增
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            logger.error("自增失败: {}, 错误: {}", key, e.getMessage());
            return 0L;
        }
    }

    /**
     * 自减
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, -delta);
        } catch (Exception e) {
            logger.error("自减失败: {}, 错误: {}", key, e.getMessage());
            return 0L;
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量设置缓存
     */
    public void multiSet(Map<String, Object> map, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().multiSet(map);
            map.forEach((key, value) -> {
                redisTemplate.expire(key, timeout, unit);
                localCache.put(key, value);
            });
            logger.debug("批量设置缓存成功: {} 个", map.size());
        } catch (Exception e) {
            logger.error("批量设置缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 批量获取缓存
     */
    public List<Object> multiGet(Collection<String> keys) {
        try {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            logger.debug("批量获取缓存成功: {} 个", keys.size());
            return values;
        } catch (Exception e) {
            logger.error("批量获取缓存失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取本地缓存统计信息
     */
    public String getLocalCacheStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = localCache.stats();
        return String.format(
            "本地缓存统计 - 请求数: %d, 命中数: %d, 命中率: %.2f%%, 平均加载时间: %.2f ns",
            stats.requestCount(),
            stats.hitCount(),
            stats.hitRate() * 100,
            stats.averageLoadPenalty()
        );
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCache.invalidateAll();
        logger.info("本地缓存已清空");
    }

    /**
     * 获取本地缓存大小
     */
    public long getLocalCacheSize() {
        return localCache.estimatedSize();
    }

    // ==================== 业务缓存操作 ====================

    /**
     * 缓存商品信息
     */
    public void cacheProduct(Long productId, Object product) {
        String key = "product:" + productId;
        setWithRandomExpire(key, product, 30, TimeUnit.MINUTES, 300);
    }

    /**
     * 获取商品缓存
     */
    public Object getProduct(Long productId) {
        return get("product:" + productId);
    }

    /**
     * 删除商品缓存
     */
    public void deleteProductCache(Long productId) {
        delete("product:" + productId);
    }

    /**
     * 缓存分类信息
     */
    public void cacheCategory(Long categoryId, Object category) {
        String key = "category:" + categoryId;
        set(key, category, 1, TimeUnit.HOURS);
    }

    /**
     * 获取分类缓存
     */
    public Object getCategory(Long categoryId) {
        return get("category:" + categoryId);
    }

    /**
     * 缓存用户信息
     */
    public void cacheUser(Long userId, Object user) {
        String key = "user:" + userId;
        set(key, user, 30, TimeUnit.MINUTES);
    }

    /**
     * 获取用户缓存
     */
    public Object getUser(Long userId) {
        return get("user:" + userId);
    }

    /**
     * 删除用户缓存
     */
    public void deleteUserCache(Long userId) {
        delete("user:" + userId);
    }

    /**
     * 缓存热点数据（高频访问）
     */
    public void cacheHotData(String key, Object data) {
        // 热点数据使用较长的过期时间
        set(key, data, 2, TimeUnit.HOURS);
    }

    /**
     * 预热缓存（批量加载）
     */
    public void warmupCache(Map<String, Object> dataMap, long timeout, TimeUnit unit) {
        logger.info("开始缓存预热，数据量: {}", dataMap.size());
        multiSet(dataMap, timeout, unit);
        logger.info("缓存预热完成");
    }


}
