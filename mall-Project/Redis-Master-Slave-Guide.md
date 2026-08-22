# Redis 一主两从架构实现指南

## 一、架构说明

本项目实现了 Redis 一主两从的主从复制架构，支持读写分离：

```
Redis 主从复制架构（一主两从）
├── 主节点（Master）：192.168.152.100:6379
│   └── 负责所有写操作
├── 从节点1（Slave1）：192.168.152.101:6379
│   └── 负责读操作，实时同步主节点数据
└── 从节点2（Slave2）：192.168.152.102:6379
    └── 负责读操作，实时同步主节点数据
```

## 二、部署方式

### 方式一：Docker Compose 部署（推荐）

#### 1. 启动 Redis 主从集群

```bash
# 进入项目目录
cd "D:\Java\project One\mall-Project"

# 启动所有节点
docker-compose -f docker-redis-master-slave.yml up -d

# 查看容器状态
docker ps | grep redis

# 查看日志
docker logs redis-master
docker logs redis-slave1
docker logs redis-slave2
```

#### 2. 验证主从复制状态

```bash
# 连接到主节点
docker exec -it redis-master redis-cli

# 查看主从复制信息
127.0.0.1:6379> INFO replication

# 应该看到类似以下输出：
# role:master
# connected_slaves:2
# slave0:ip=172.30.0.11,port=6379,state=online,offset=xxx
# slave1:ip=172.30.0.12,port=6379,state=online,offset=xxx
```

#### 3. 测试主从同步

```bash
# 在主节点写入数据
docker exec -it redis-master redis-cli
127.0.0.1:6379> SET test_key "Hello Master-Slave"
OK

# 从从节点1读取数据
docker exec -it redis-slave1 redis-cli
127.0.0.1:6379> GET test_key
"Hello Master-Slave"

# 从从节点2读取数据
docker exec -it redis-slave2 redis-cli
127.0.0.1:6379> GET test_key
"Hello Master-Slave"
```

#### 4. 访问 Redis 管理界面

访问 http://localhost:8081 使用 Redis Commander 管理工具

### 方式二：Linux 服务器部署

#### 1. 在三台服务器上分别执行部署脚本

```bash
# 下载部署脚本
chmod +x deploy-redis-master-slave.sh

# 在主节点服务器执行
./deploy-redis-master-slave.sh
# 选择选项 1（配置主节点）和 4（启动所有节点）

# 在从节点1服务器执行
./deploy-redis-master-slave.sh
# 选择选项 2（配置从节点1）和 4（启动所有节点）

# 在从节点2服务器执行
./deploy-redis-master-slave.sh
# 选择选项 3（配置从节点2）和 4（启动所有节点）
```

#### 2. 验证主从复制

```bash
# 在主节点服务器
redis-cli -h 192.168.152.100 -p 6379 INFO replication

# 在从节点1服务器
redis-cli -h 192.168.152.101 -p 6379 INFO replication

# 在从节点2服务器
redis-cli -h 192.168.152.102 -p 6379 INFO replication
```

## 三、应用配置

### 1. Spring Boot 配置

配置文件已更新为支持一主两从架构：

**application-dev.yaml**
```yaml
spring:
  data:
    redis:
      # 主节点地址
      host: 192.168.152.100
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
      
      # Lettuce 连接池配置
      lettuce:
        pool:
          max-active: 100
          max-idle: 50
          min-idle: 10
          max-wait: 3000ms

redisson:
  # 主从模式配置
  master-slave-config:
    master-address: redis://192.168.152.100:6379
    slave-addresses:
      - redis://192.168.152.101:6379
      - redis://192.168.152.102:6379
    read-mode: SLAVE  # 优先从从节点读取
```

### 2. 使用 Redis 服务

```java
@Autowired
private RedisMasterSlaveService redisService;

// 写入数据（自动路由到主节点）
redisService.set("user:1001", userObject);

// 读取数据（自动路由到从节点）
Object user = redisService.get("user:1001");

// 设置过期时间
redisService.set("session:token", token, 30, TimeUnit.MINUTES);
```

## 四、读写分离原理

### 1. 写操作
- 所有写操作（SET、HSET、LPUSH 等）自动路由到主节点
- 主节点将写操作异步复制到两个从节点

### 2. 读操作
- 所有读操作（GET、HGET、LRANGE 等）自动路由到从节点
- Redisson 配置 `read-mode: SLAVE` 实现读负载均衡
- 两个从节点分担读请求，提升读取性能

### 3. 数据同步
- 主节点将写操作记录到复制缓冲区
- 从节点连接主节点，实时获取复制数据
- 默认使用异步复制，延迟通常在毫秒级

## 五、测试验证

### 1. 运行测试类

```bash
# 运行所有测试
mvn test -Dtest=RedisMasterSlaveTest

# 运行单个测试方法
mvn test -Dtest=RedisMasterSlaveTest#testBasicWriteAndRead
```

### 2. 测试内容

- ✅ 基本读写测试
- ✅ 主从同步延迟测试
- ✅ 并发写入测试
- ✅ Redisson 客户端测试
- ✅ 数据持久化测试
- ✅ 过期时间测试
- ✅ 各种数据结构测试（Hash、List、Set、ZSet）

## 六、监控与运维

### 1. 查看主从状态

```bash
# 查看主节点状态
redis-cli -h 192.168.152.100 -p 6379 INFO replication

# 查看从节点状态
redis-cli -h 192.168.152.101 -p 6379 INFO replication
redis-cli -h 192.168.152.102 -p 6379 INFO replication
```

### 2. 查看性能指标

```bash
# 查看内存使用
redis-cli -h 192.168.152.100 -p 6379 INFO memory

# 查看连接数
redis-cli -h 192.168.152.100 -p 6379 INFO clients

# 查看统计信息
redis-cli -h 192.168.152.100 -p 6379 INFO stats
```

### 3. 性能测试

```bash
# 基准测试（主节点）
redis-benchmark -h 192.168.152.100 -p 6379 -c 100 -n 100000

# 基准测试（从节点）
redis-benchmark -h 192.168.152.101 -p 6379 -c 100 -n 100000
```

## 七、故障处理

### 1. 从节点故障
- 不影响主节点和另一个从节点的正常运行
- 重启从节点后会自动重新同步数据

### 2. 主节点故障
- 需要手动将从节点提升为主节点
- 或者使用 Sentinel 哨兵模式实现自动故障转移

#### 手动故障转移步骤：

```bash
# 1. 停止主节点
redis-cli -h 192.168.152.100 -p 6379 SHUTDOWN NOSAVE

# 2. 选择一个从节点提升为主节点
redis-cli -h 192.168.152.101 -p 6379 SLAVEOF NO ONE

# 3. 修改另一个从节点的配置，指向新的主节点
redis-cli -h 192.168.152.102 -p 6379 SLAVEOF 192.168.152.101 6379

# 4. 更新应用配置，指向新的主节点
```

## 八、性能优化建议

### 1. 内存优化
- 根据实际数据量调整 `maxmemory` 配置
- 使用合适的淘汰策略 `maxmemory-policy`

### 2. 持久化优化
- 根据数据重要性选择 RDB 或 AOF
- 生产环境建议同时开启 RDB 和 AOF

### 3. 网络优化
- 使用内网连接，减少网络延迟
- 调整 `tcp-keepalive` 和 `timeout` 参数

### 4. 连接池优化
- 根据并发量调整连接池大小
- 监控连接池使用情况，及时调整

## 九、注意事项

1. **数据一致性**：主从复制是异步的，存在短暂延迟
2. **从节点只读**：从节点默认只读，不应直接写入数据
3. **故障转移**：主节点故障需要手动处理或使用 Sentinel
4. **网络延迟**：确保主从节点之间网络稳定，延迟低
5. **资源监控**：定期监控内存、CPU、网络等资源使用情况

## 十、扩展方案

### 1. 添加更多从节点
- 修改配置文件，添加新的从节点地址
- 重启应用，新从节点会自动同步数据

### 2. 升级为 Sentinel 哨兵模式
- 配置 Sentinel 实现自动故障转移
- 提高系统可用性

### 3. 升级为 Redis Cluster 集群
- 支持数据分片，突破单机内存限制
- 提供更高的性能和可用性

---

**作者**: 67161  
**创建时间**: 2026-05-29  
**项目**: mall-Project
