package com.mall.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 缓存雪崩防护服务
 */
@Service
public class CacheAvalancheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 随机数生成器
    private static final Random random = new Random();

    /**
     * 方案一：过期时间随机化（推荐）
     * 
     * 在基础过期时间上增加随机值，避免大量缓存同时过期
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param baseTimeout 基硎过期时间
     * @param unit 时间单位
     */
    public void setWithRandomExpire(String key, Object value, long baseTimeout, TimeUnit unit) {
        // 随机增加 0-30 分钟的过期时间
        long randomMinutes = random.nextInt(30);
        long totalTimeout = baseTimeout + randomMinutes;
        
        redisTemplate.opsForValue().set(key, value, totalTimeout, unit);
    }


    /**
     * 方案二：逻辑过期（永不过期）
     * 
     * 缓存永不过期，但在数据中存储逻辑过期时间
     * 当访问时发现逻辑过期，异步更新缓存
     */
    public void setWithLogicalExpire(String key, Object value, long logicalTimeout, TimeUnit unit) {
        // 构建包含逻辑过期时间的数据
        LogicalExpireData data = new LogicalExpireData();
        data.setData(value);
        data.setExpireTime(System.currentTimeMillis() + unit.toMillis(logicalTimeout));
        
        // 缓存永不过期（或设置很长的过期时间，如 1 天）
        redisTemplate.opsForValue().set(key, data, 24, TimeUnit.HOURS);
    }

    /**
     * 获取逻辑过期的数据
     * 
     * @param key 缓存键
     * @return 缓存数据，如果逻辑过期返回 null
     */
    public Object getWithLogicalExpire(String key) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof LogicalExpireData) {
            LogicalExpireData data = (LogicalExpireData) obj;
            
            // 检查是否逻辑过期
            if (System.currentTimeMillis() > data.getExpireTime()) {
                // 逻辑过期，返回 null，触发异步更新
                return null;
            }
            
            return data.getData();
        }
        
        return obj;
    }

    /**
     * 方案三：批量设置缓存时，分散过期时间
     * 
     * @param keys 缓存键数组
     * @param values 缓存值数组
     * @param baseTimeout 基硎过期时间
     * @param unit 时间单位
     */
    public void batchSetWithRandomExpire(String[] keys, Object[] values, long baseTimeout, TimeUnit unit) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("keys 和 values 长度不一致");
        }
        
        for (int i = 0; i < keys.length; i++) {
            // 为每个 key 设置不同的随机过期时间
            setWithRandomExpire(keys[i], values[i], baseTimeout, unit);
        }
    }



    /**
     * 数据加载器接口
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        T load();
    }

    /**
     * 逻辑过期数据包装类
     */
    public static class LogicalExpireData {
        private Object data;
        private long expireTime;

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }
    }

    /**
     * 计算随机过期时间
     * 
     * @param baseMinutes 基硎分钟数
     * @return 随机过期时间（分钟）
     */
    public static long calculateRandomExpire(int baseMinutes) {
        // 基硎时间 + 随机时间（0-30分钟）
        return baseMinutes + random.nextInt(30);
    }


}
