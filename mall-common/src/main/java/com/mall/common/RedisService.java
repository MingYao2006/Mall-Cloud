package com.mall.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
//
@Service
@Slf4j
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 测试 Redis 是否可用，不可用则降级 */
    private boolean isAvailable() {
        try {
            redisTemplate.hasKey("__health_check__");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try { redisTemplate.opsForValue().set(key, value, timeout, unit); } catch (Exception e) { /* Redis 不可用，跳过缓存 */ }
    }

    public void set(String key, Object value) {
        try { redisTemplate.opsForValue().set(key, value); } catch (Exception e) { }
    }

    public Object get(String key) {
        try { return redisTemplate.opsForValue().get(key); } catch (Exception e) { return null; }
    }

    public Boolean delete(String key) {
        try { return redisTemplate.delete(key); } catch (Exception e) { return false; }
    }

    public Long delete(Collection<String> keys) {
        try { return redisTemplate.delete(keys); } catch (Exception e) { return 0L; }
    }
//    判断 key 是否存在
    public Boolean hasKey(String key) {
        try { return redisTemplate.hasKey(key); } catch (Exception e) { return false; }
    }
//    设置/获取过期时间
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try { return redisTemplate.expire(key, timeout, unit); } catch (Exception e) { return false; }
    }

    public Long getExpire(String key, TimeUnit unit) {
        try { return redisTemplate.getExpire(key, unit); } catch (Exception e) { return -1L; }
    }
//    模糊匹配删除（通配符）
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) return redisTemplate.delete(keys);
        } catch (Exception e) { }
        return 0L;
    }
//    模糊查询 key
    public Set<String> keys(String pattern) {
        try { return redisTemplate.keys(pattern); } catch (Exception e) { return Set.of(); }
    }
//自增自减
//    使用场景：
//    商品浏览量统计
//    接口访问次数限制
//    验证码发送次数限制
    public Long increment(String key, long delta) {
        try { return redisTemplate.opsForValue().increment(key, delta); } catch (Exception e) { return 0L; }
    }

    public Long decrement(String key, long delta) {
        try { return redisTemplate.opsForValue().increment(key, -delta); } catch (Exception e) { return 0L; }
    }
//    Hash 操作（哈希表）
    public void hSet(String key, String field, Object value) {
        try { redisTemplate.opsForHash().put(key, field, value); } catch (Exception e) { }
    }

    public Object hGet(String key, String field) {
        try { return redisTemplate.opsForHash().get(key, field); } catch (Exception e) { return null; }
    }

    public Long hDelete(String key, Object... fields) {
        try { return redisTemplate.opsForHash().delete(key, fields); } catch (Exception e) { return 0L; }
    }

    public Boolean hHasKey(String key, String field) {
        try { return redisTemplate.opsForHash().hasKey(key, field); } catch (Exception e) { return false; }
    }

    /**
     * 设置缓存并添加随机过期时间，防止缓存雪崩
     * @param key 缓存key
     * @param value 缓存值
     * @param timeout 基础过期时间
     * @param unit 时间单位
     * @param randomRange 随机时间范围（秒），比如300表示增加0-300秒随机值
     */
    public void setWithRandomExpire(String key, Object value, long timeout, TimeUnit unit, long randomRange) {
        try {
            long randomTimeout = timeout + (long) (Math.random() * randomRange);
            redisTemplate.opsForValue().set(key, value, randomTimeout, unit);
        } catch (Exception e) {
            log.warn("缓存写入失败: {}", e.getMessage());
        }
    }

    /**
     * 设置缓存并添加随机过期时间（默认随机范围5分钟）
     */
    public void setWithRandomExpire(String key, Object value, long timeout, TimeUnit unit) {
        setWithRandomExpire(key, value, timeout, unit, 300);
    }
}
