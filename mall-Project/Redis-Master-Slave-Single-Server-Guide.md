# Redis 一主两从架构部署指南（单机多实例）

## 一、架构说明

在同一台虚拟机（192.168.152.100）上启动三个 Redis 实例，实现一主两从架构：

```
Redis 主从复制架构（单机多实例）
├── 主节点（Master）：192.168.152.100:6379
│   └── 负责所有写操作
├── 从节点1（Slave1）：192.168.152.100:6380
│   └── 负责读操作，实时同步主节点数据
└── 从节点2（Slave2）：192.168.152.100:6381
    └── 负责读操作，实时同步主节点数据
```

## 二、部署步骤

### 方式一：使用部署脚本（推荐）

#### 1. 上传部署脚本到虚拟机

```bash
# 将 deploy-redis-single-server.sh 上传到虚拟机
scp deploy-redis-single-server.sh root@192.168.152.100:/root/

# 登录虚拟机
ssh root@192.168.152.100

# 添加执行权限
chmod +x deploy-redis-single-server.sh

# 执行脚本
./deploy-redis-single-server.sh
```

#### 2. 选择一键部署

在菜单中选择 `9` - 一键部署，脚本会自动完成：
- 创建配置文件目录
- 生成主节点配置（6379.conf）
- 生成从节点1配置（6380.conf）
- 生成从节点2配置（6381.conf）
- 启动所有 Redis 实例
- 验证主从复制状态

### 方式二：手动部署

#### 1. 创建配置文件目录

```bash
sudo mkdir -p /etc/redis/one-master-two-slave
sudo mkdir -p /var/lib/redis/6379
sudo mkdir -p /var/lib/redis/6380
sudo mkdir -p /var/lib/redis/6381
sudo mkdir -p /var/log/redis
sudo mkdir -p /var/run/redis
```

#### 2. 上传配置文件

将以下配置文件上传到虚拟机：

```bash
# 上传配置文件
scp redis-config/one-master-two-slave/6379.conf root@192.168.152.100:/etc/redis/one-master-two-slave/
scp redis-config/one-master-two-slave/6380.conf root@192.168.152.100:/etc/redis/one-master-two-slave/
scp redis-config/one-master-two-slave/6381.conf root@192.168.152.100:/etc/redis/one-master-two-slave/
```

#### 3. 启动 Redis 实例

```bash
# 启动主节点（6379）
sudo redis-server /etc/redis/one-master-two-slave/6379.conf

# 启动从节点1（6380）
sudo redis-server /etc/redis/one-master-two-slave/6380.conf

# 启动从节点2（6381）
sudo redis-server /etc/redis/one-master-two-slave/6381.conf
```

## 三、验证部署

### 1. 检查进程状态

```bash
ps aux | grep redis-server
```

应该看到三个 Redis 进程：
```
redis-server *:6379
redis-server *:6380
redis-server *:6381
```

### 2. 验证主从复制

```bash
# 查看主节点信息
redis-cli -h 192.168.152.100 -p 6379 INFO replication
```

应该看到：
```
# Replication
role:master
connected_slaves:2
slave0:ip=192.168.152.100,port=6380,state=online,offset=xxx
slave1:ip=192.168.152.100,port=6381,state=online,offset=xxx
```

### 3. 测试主从同步

```bash
# 在主节点写入数据
redis-cli -h 192.168.152.100 -p 6379 SET test_key "Hello Master-Slave"
OK

# 从从节点1读取数据
redis-cli -h 192.168.152.100 -p 6380 GET test_key
"Hello Master-Slave"

# 从从节点2读取数据
redis-cli -h 192.168.152.100 -p 6381 GET test_key
"Hello Master-Slave"
```

### 4. 测试读写分离

```bash
# 在从节点尝试写入（应该失败）
redis-cli -h 192.168.152.100 -p 6380 SET test_write "Should Fail"
(error) READONLY You can't write against a read only replica.

# 在主节点写入成功
redis-cli -h 192.168.152.100 -p 6379 SET test_write "Write Success"
OK
```

## 四、常用命令

### 启动/停止

```bash
# 启动所有实例
sudo redis-server /etc/redis/one-master-two-slave/6379.conf
sudo redis-server /etc/redis/one-master-two-slave/6380.conf
sudo redis-server /etc/redis/one-master-two-slave/6381.conf

# 停止所有实例
redis-cli -p 6379 SHUTDOWN
redis-cli -p 6380 SHUTDOWN
redis-cli -p 6381 SHUTDOWN

# 或者使用 kill 命令
sudo kill $(cat /var/run/redis/redis-6379.pid)
sudo kill $(cat /var/run/redis/redis-6380.pid)
sudo kill $(cat /var/run/redis/redis-6381.pid)
```

### 查看状态

```bash
# 查看主节点状态
redis-cli -p 6379 INFO replication

# 查看从节点1状态
redis-cli -p 6380 INFO replication

# 查看从节点2状态
redis-cli -p 6381 INFO replication

# 查看所有节点的内存使用
redis-cli -p 6379 INFO memory
redis-cli -p 6380 INFO memory
redis-cli -p 6381 INFO memory
```

### 查看日志

```bash
# 查看主节点日志
tail -f /var/log/redis/redis-6379.log

# 查看从节点1日志
tail -f /var/log/redis/redis-6380.log

# 查看从节点2日志
tail -f /var/log/redis/redis-6381.log
```

## 五、应用配置说明

### Spring Boot 配置

`application-dev.yaml` 已更新为支持单机多实例架构：

```yaml
spring:
  data:
    redis:
      host: 192.168.152.100  # 主节点地址
      port: 6379              # 主节点端口
      timeout: 3000ms

redisson:
  master-slave-config:
    master-address: redis://192.168.152.100:6379
    slave-addresses:
      - redis://192.168.152.100:6380  # 从节点1
      - redis://192.168.152.100:6381  # 从节点2
    read-mode: SLAVE  # 优先从从节点读取
```

### Java 代码使用

```java
@Autowired
private RedisMasterSlaveService redisService;

// 写操作自动路由到主节点（6379）
redisService.set("user:1001", userObject);

// 读操作自动路由到从节点（6380 或 6381）
Object user = redisService.get("user:1001");
```

## 六、性能测试

### 1. 基准测试

```bash
# 测试主节点写入性能
redis-benchmark -h 192.168.152.100 -p 6379 -t set -c 100 -n 100000

# 测试从节点读取性能
redis-benchmark -h 192.168.152.100 -p 6380 -t get -c 100 -n 100000
redis-benchmark -h 192.168.152.100 -p 6381 -t get -c 100 -n 100000
```

### 2. 并发测试

```bash
# 并发写入测试
redis-benchmark -h 192.168.152.100 -p 6379 -c 1000 -n 100000 -t set

# 并发读取测试
redis-benchmark -h 192.168.152.100 -p 6380 -c 1000 -n 100000 -t get
```

## 七、监控与运维

### 1. 监控指标

```bash
# 查看连接数
redis-cli -p 6379 INFO clients

# 查看内存使用
redis-cli -p 6379 INFO memory

# 查看统计信息
redis-cli -p 6379 INFO stats

# 查看持久化状态
redis-cli -p 6379 INFO persistence
```

### 2. 实时监控

```bash
# 使用 redis-cli 监控命令
redis-cli -p 6379 MONITOR

# 查看慢查询日志
redis-cli -p 6379 SLOWLOG GET 10
```

## 八、故障处理

### 1. 从节点故障

如果某个从节点故障，不影响其他节点的运行：

```bash
# 检查从节点状态
redis-cli -p 6380 PING
redis-cli -p 6381 PING

# 重启故障的从节点
sudo redis-server /etc/redis/one-master-two-slave/6380.conf
```

### 2. 主节点故障

如果主节点故障，需要手动将从节点提升为主节点：

```bash
# 1. 停止主节点
redis-cli -p 6379 SHUTDOWN NOSAVE

# 2. 将从节点1提升为主节点
redis-cli -p 6380 SLAVEOF NO ONE

# 3. 修改从节点2的配置，指向新的主节点
redis-cli -p 6381 SLAVEOF 192.168.152.100 6380

# 4. 更新应用配置，指向新的主节点（6380）
```

### 3. 数据恢复

如果数据丢失，可以从其他节点恢复：

```bash
# 从主节点导出数据
redis-cli -p 6379 --rdb /tmp/dump.rdb

# 导入数据到其他节点
# （需要先停止目标节点，复制 RDB 文件，再启动）
```

## 九、优化建议

### 1. 内存优化

根据实际数据量调整 `maxmemory`：

```bash
# 编辑配置文件
sudo vi /etc/redis/one-master-two-slave/6379.conf

# 修改内存限制
maxmemory 4gb  # 根据实际情况调整
```

### 2. 持久化优化

根据数据重要性选择持久化策略：

- **高可靠性**：同时开启 RDB 和 AOF
- **高性能**：只开启 RDB，或关闭持久化
- **平衡**：开启 AOF，使用 `appendfsync everysec`

### 3. 网络优化

如果应用和 Redis 不在同一台机器：

```bash
# 修改 bind 地址为内网 IP
bind 192.168.152.100

# 或者使用 0.0.0.0 允许所有连接（需要配合防火墙）
bind 0.0.0.0
```

## 十、注意事项

1. **端口冲突**：确保 6379、6380、6381 端口未被占用
2. **内存限制**：三个实例共享服务器内存，注意总内存使用
3. **数据目录**：每个实例使用独立的数据目录，避免冲突
4. **日志文件**：每个实例使用独立的日志文件
5. **PID 文件**：每个实例使用独立的 PID 文件
6. **防火墙**：如需远程访问，开放相应端口

## 十一、扩展方案

### 1. 添加更多从节点

```bash
# 创建从节点3配置（6382）
cp /etc/redis/one-master-two-slave/6380.conf /etc/redis/one-master-two-slave/6382.conf

# 修改端口和数据目录
sed -i 's/6380/6382/g' /etc/redis/one-master-two-slave/6382.conf

# 启动从节点3
sudo redis-server /etc/redis/one-master-two-slave/6382.conf
```

### 2. 升级为 Sentinel 哨兵模式

添加 Sentinel 实现自动故障转移，提高可用性。

---

**作者**: 67161  
**创建时间**: 2026-05-29  
**项目**: mall-Project
