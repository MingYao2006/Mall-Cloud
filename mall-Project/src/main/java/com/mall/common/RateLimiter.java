package com.mall.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 令牌桶限流
     * @param key 限流key
     * @param capacity 桶容量
     * @param rate 每秒生成令牌数
     */
    public boolean tryAcquire(String key, int capacity, int rate) {
        String luaScript =
                "local bucket = redis.call('hmget', KEYS[1], 'tokens', 'timestamp') " +
                        "local tokens = tonumber(bucket[1]) " +
                        "local last_time = tonumber(bucket[2]) " +
                        "local now = tonumber(ARGV[3]) " +
                        "local rate = tonumber(ARGV[1]) " +
                        "local capacity = tonumber(ARGV[2]) " +

                        "if tokens == nil then " +
                        "    tokens = capacity - 1 " +
                        "    redis.call('hmset', KEYS[1], 'tokens', tokens, 'timestamp', now) " +
                        "    redis.call('expire', KEYS[1], ARGV[4]) " +
                        "    return 1 " +
                        "end " +

                        "local delta = math.max(0, now - last_time) " +
                        "tokens = math.min(capacity, tokens + delta * rate) " +
                        "if tokens < 1 then " +
                        "    return 0 " +
                        "end " +
                        "tokens = tokens - 1 " +
                        "redis.call('hmset', KEYS[1], 'tokens', tokens, 'timestamp', now) " +
                        "return 1";

        Long result = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.eval(luaScript.getBytes(), ReturnType.INTEGER, 1,
                        key.getBytes(),
                        String.valueOf(rate).getBytes(),
                        String.valueOf(capacity).getBytes(),
                        String.valueOf(System.currentTimeMillis() / 1000).getBytes(),
                        String.valueOf(60).getBytes())
        );

        return result != null && result == 1;
    }
}
