package com.mall.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/**
 * Redis 主从复制配置类
 * 实现一主两从架构的读写分离
 * 
 * 注意：此配置类会在 Nacos 配置加载后执行
 * 确保 spring.data.redis.host 和 spring.data.redis.port 已从 Nacos 加载
 */
@Configuration
public class RedisMasterSlaveConfig {

    @Value("${app.redis.master.host:localhost}")
    private String masterHost;

    @Value("${app.redis.master.port:6379}")
    private int masterPort;

    @Value("${app.redis.master.password:}")
    private String password;

    @Value("${app.redis.slave1.port:6380}")
    private int slave1Port;

    @Value("${app.redis.slave2.port:6381}")
    private int slave2Port;

    /**
     * 配置 Redisson 客户端（主从模式）
     * 支持读写分离：写操作走主节点，读操作走从节点
     * 
     * 架构说明：同一台服务器上启动三个 Redis 实例
     * - 主节点：host:6379（负责写操作）
     * - 从节点1：host:6380（负责读操作）
     * - 从节点2：host:6381（负责读操作）
     */
    @Bean(destroyMethod = "shutdown")
    @Primary  // 优先使用此配置
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 主从配置
        config.useMasterSlaveServers()
                // 主节点地址
                .setMasterAddress("redis://" + masterHost + ":" + masterPort)
                // 从节点地址列表（一主两从）
                .addSlaveAddress(
                        "redis://" + masterHost + ":" + slave1Port,  // 从节点1
                        "redis://" + masterHost + ":" + slave2Port   // 从节点2
                )
                // 读模式：优先从从节点读取
                .setReadMode(ReadMode.SLAVE)
                // 数据库索引
                .setDatabase(0)
                // 连接池配置
                .setMasterConnectionPoolSize(100)
                .setSlaveConnectionPoolSize(100)
                .setMasterConnectionMinimumIdleSize(10)
                .setSlaveConnectionMinimumIdleSize(10)
                // 超时配置
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setIdleConnectionTimeout(10000);
        
        // 禁用分布式锁的从节点同步检查（单机主从模式下从节点同步延迟可接受）
        config.setCheckLockSyncedSlaves(false);
        
        // 如果设置了密码
        if (password != null && !password.isEmpty()) {
            config.useMasterSlaveServers().setPassword(password);
        }
        
        // 线程池配置
        config.setThreads(16)
              .setNettyThreads(32);
        
        return Redisson.create(config);
    }
}
