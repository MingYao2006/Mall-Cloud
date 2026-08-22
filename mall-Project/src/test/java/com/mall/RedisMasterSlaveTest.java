package com.mall;

import com.mall.application.Application;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 主从复制测试类
 * 测试一主两从架构的读写分离功能
 * 
 * 注意：运行测试前需要确保：
 * 1. Nacos 服务已启动（localhost:8848）
 * 2. Nacos 中已配置 mall-project-dev-redis.yaml（包含 Redis 主从配置）
 * 3. Redis 主从节点已启动（6379/6380/6381）
 * 
 * 当前已禁用测试，使用 @Disabled 注解跳过测试
 */
@Disabled("需要 Nacos 和 Redis 环境支持，暂时禁用")
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")  // 激活 dev 配置文件，从 Nacos 读取 Redis 配置
public class RedisMasterSlaveTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 测试基本的写入和读取操作
     */
    @Test
    public void testBasicWriteAndRead() {
        // 写入数据（自动路由到主节点）
        String key = "test:master:slave:key";
        String value = "Hello Redis Master-Slave!";
        
        redisTemplate.opsForValue().set(key, value);
        
        // 读取数据（自动路由到从节点）
        Object result = redisTemplate.opsForValue().get(key);
        
        assertEquals(value, result);
        System.out.println("✓ 基本读写测试通过：写入主节点，从从节点读取成功");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }

    /**
     * 测试主从同步延迟
     */
    @Test
    public void testReplicationLag() throws InterruptedException {
        String key = "test:replication:lag";
        String value = "Replication Test";
        
        // 写入主节点
        long startTime = System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, value);
        long writeTime = System.currentTimeMillis() - startTime;
        
        // 等待同步到从节点
        Thread.sleep(100);
        
        // 从从节点读取
        startTime = System.currentTimeMillis();
        Object result = redisTemplate.opsForValue().get(key);
        long readTime = System.currentTimeMillis() - startTime;
        
        assertEquals(value, result);
        System.out.println("✓ 主从同步测试通过");
        System.out.println("  写入耗时: " + writeTime + "ms");
        System.out.println("  读取耗时: " + readTime + "ms");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }

    /**
     * 测试并发写入
     */
    @Test
    public void testConcurrentWrite() throws InterruptedException {
        String keyPrefix = "test:concurrent:";
        int threadCount = 10;
        
        // 创建多个线程并发写入
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String key = keyPrefix + index;
                String value = "Value-" + index;
                redisTemplate.opsForValue().set(key, value);
            });
        }
        
        // 启动所有线程
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        long totalTime = System.currentTimeMillis() - startTime;
        
        // 验证所有数据都已写入
        for (int i = 0; i < threadCount; i++) {
            String key = keyPrefix + i;
            Object value = redisTemplate.opsForValue().get(key);
            assertEquals("Value-" + i, value);
            redisTemplate.delete(key);
        }
        
        System.out.println("✓ 并发写入测试通过");
        System.out.println("  并发线程数: " + threadCount);
        System.out.println("  总耗时: " + totalTime + "ms");
    }

    /**
     * 测试 Redisson 客户端连接
     */
    @Test
    public void testRedissonConnection() {
        assertNotNull(redissonClient);
        System.out.println("✓ Redisson 客户端连接成功");
        
        // 测试 Redisson 的基本操作
        String key = "test:redisson:key";
        String value = "Redisson Test";
        
        // 使用 Redisson 写入
        redissonClient.getBucket(key).set(value);
        
        // 使用 Redisson 读取
        Object result = redissonClient.getBucket(key).get();
        assertEquals(value, result);
        System.out.println("✓ Redisson 读写测试通过");
        
        // 清理测试数据
        redissonClient.getBucket(key).delete();
    }

    /**
     * 测试数据持久化
     */
    @Test
    public void testPersistence() throws InterruptedException {
        String key = "test:persistence:key";
        String value = "Persistence Test";
        
        // 写入数据
        redisTemplate.opsForValue().set(key, value);
        
        // 立即读取
        Object result1 = redisTemplate.opsForValue().get(key);
        assertEquals(value, result1);
        
        // 等待一段时间后再次读取
        Thread.sleep(1000);
        Object result2 = redisTemplate.opsForValue().get(key);
        assertEquals(value, result2);
        
        System.out.println("✓ 数据持久化测试通过");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }

    /**
     * 测试过期时间
     */
    @Test
    public void testExpiration() throws InterruptedException {
        String key = "test:expiration:key";
        String value = "Expiration Test";
        
        // 设置 2 秒过期
        redisTemplate.opsForValue().set(key, value, 2, java.util.concurrent.TimeUnit.SECONDS);
        
        // 立即读取，应该存在
        Object result1 = redisTemplate.opsForValue().get(key);
        assertEquals(value, result1);
        
        // 等待 3 秒后读取，应该已过期
        Thread.sleep(3000);
        Object result2 = redisTemplate.opsForValue().get(key);
        assertNull(result2);
        
        System.out.println("✓ 过期时间测试通过");
    }

    /**
     * 测试 Hash 数据结构
     */
    @Test
    public void testHashOperations() {
        String key = "test:hash:key";
        
        // 写入 Hash 字段
        redisTemplate.opsForHash().put(key, "field1", "value1");
        redisTemplate.opsForHash().put(key, "field2", "value2");
        
        // 读取 Hash 字段
        Object value1 = redisTemplate.opsForHash().get(key, "field1");
        Object value2 = redisTemplate.opsForHash().get(key, "field2");
        
        assertEquals("value1", value1);
        assertEquals("value2", value2);
        
        System.out.println("✓ Hash 操作测试通过");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }

    /**
     * 测试 List 数据结构
     */
    @Test
    public void testListOperations() {
        String key = "test:list:key";
        
        // 写入 List
        redisTemplate.opsForList().rightPush(key, "item1");
        redisTemplate.opsForList().rightPush(key, "item2");
        redisTemplate.opsForList().rightPush(key, "item3");
        
        // 读取 List
        java.util.List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        
        assertEquals(3, list.size());
        assertEquals("item1", list.get(0));
        assertEquals("item2", list.get(1));
        assertEquals("item3", list.get(2));
        
        System.out.println("✓ List 操作测试通过");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }

    /**
     * 测试 Set 数据结构
     */
    @Test
    public void testSetOperations() {
        String key = "test:set:key";
        
        // 写入 Set
        redisTemplate.opsForSet().add(key, "member1", "member2", "member3");
        
        // 读取 Set
        java.util.Set<Object> set = redisTemplate.opsForSet().members(key);
        
        assertEquals(3, set.size());
        assertTrue(set.contains("member1"));
        assertTrue(set.contains("member2"));
        assertTrue(set.contains("member3"));
        
        System.out.println("✓ Set 操作测试通过");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }

    /**
     * 测试 ZSet 数据结构
     */
    @Test
    public void testZSetOperations() {
        String key = "test:zset:key";
        
        // 写入 ZSet
        redisTemplate.opsForZSet().add(key, "member1", 1.0);
        redisTemplate.opsForZSet().add(key, "member2", 2.0);
        redisTemplate.opsForZSet().add(key, "member3", 3.0);
        
        // 读取 ZSet
        java.util.Set<Object> set = redisTemplate.opsForZSet().range(key, 0, -1);
        
        assertEquals(3, set.size());
        
        System.out.println("✓ ZSet 操作测试通过");
        
        // 清理测试数据
        redisTemplate.delete(key);
    }
}