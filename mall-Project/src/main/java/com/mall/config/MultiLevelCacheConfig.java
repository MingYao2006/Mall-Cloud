package com.mall.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置
 * 
 * 实现：本地缓存（Caffeine）+ 分布式缓存（Redis）
 * 
 * 优点：
 * 1. 本地缓存：访问速度快，减少 Redis 压力
 * 2. Redis 缓存：数据一致性，支持分布式
 * 3. 两级缓存：提高系统可用性，防止缓存雪崩
 */
@Configuration
@EnableCaching
public class MultiLevelCacheConfig {

    /**
     * Caffeine 本地缓存配置
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000)
                // 过期时间：写入后 10 分钟过期
                .expireAfterWrite(10, TimeUnit.MINUTES)
                // 过期时间：访问后 5 分钟过期
                .expireAfterAccess(5, TimeUnit.MINUTES)
                // 开启统计
                .recordStats();
    }

    /**
     * 本地缓存管理器（Caffeine）
     */
    @Bean("localCacheManager")
    public CacheManager localCacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    /**
     * Redis 缓存配置
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // 设置默认过期时间：30 分钟
                .entryTtl(Duration.ofMinutes(30))
                // 禁止缓存 null 值
                .disableCachingNullValues()
                // 设置 key 序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 设置 value 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    /**
     * Redis 缓存管理器
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration cacheConfiguration) {
        // 为不同的缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 商品缓存：20-40 分钟随机过期（防止缓存雪崩）
        cacheConfigurations.put("product", cacheConfiguration.entryTtl(Duration.ofMinutes(20 + (long)(Math.random() * 20))));
        
        // 分类缓存：1 小时过期
        cacheConfigurations.put("category", cacheConfiguration.entryTtl(Duration.ofHours(1)));
        
        // 用户缓存：30 分钟过期
        cacheConfigurations.put("user", cacheConfiguration.entryTtl(Duration.ofMinutes(30)));
        
        // 订单缓存：10 分钟过期
        cacheConfigurations.put("order", cacheConfiguration.entryTtl(Duration.ofMinutes(10)));
        
        // 购物车缓存：15 分钟过期
        cacheConfigurations.put("cart", cacheConfiguration.entryTtl(Duration.ofMinutes(15)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
