# Docker Desktop 部署 Redis 主从集群指南

## 一、前置准备

### 1. 确认 Docker Desktop 已安装并运行

打开 PowerShell 或命令提示符，检查 Docker 状态：

```powershell
# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker-compose --version

# 检查 Docker 运行状态
docker ps
```

### 2. 在 Docker Desktop 中设置资源

打开 Docker Desktop → Settings → Resources：

- **CPUs**: 建议 2 核或以上
- **Memory**: 建议 4GB 或以上
- **Disk image location**: 确保有足够磁盘空间

## 二、部署步骤

### 步骤 1：打开 PowerShell

以管理员身份运行 PowerShell：

- 按 `Win + X`
- 选择 "Windows PowerShell (管理员)"

### 步骤 2：进入项目目录

```powershell
# 进入项目目录
cd "D:\Java\project One\docker-redis-cluster"

# 查看文件列表
dir
```

应该看到以下文件：
- docker-compose.yml
- redis-conf/
- redis-cluster.sh
- README.md

### 步骤 3：创建数据目录

```powershell
# 创建数据目录
New-Item -ItemType Directory -Force -Path redis-data\master
New-Item -ItemType Directory -Force -Path redis-data\slave-1
New-Item -ItemType Directory -Force -Path redis-data\slave-2
```

### 步骤 4：启动 Redis 集群

```powershell
# 启动所有容器
docker-compose up -d

# 查看容器状态
docker-compose ps

# 查看容器日志
docker-compose logs
```

**预期输出：**

```
Name              Command               State           Ports
---------------------------------------------------------------------------
redis-master    redis-server /etc/redis/re...   Up      0.0.0.0:6379->6379/tcp
redis-slave-1   redis-server /etc/redis/re...   Up      0.0.0.0:6380->6379/tcp
redis-slave-2   redis-server /etc/redis/re...   Up      0.0.0.0:6381->6379/tcp
```

### 步骤 5：在 Docker Desktop 中查看

打开 Docker Desktop 应用：

1. 点击左侧菜单 "Containers"
2. 可以看到 `docker-redis-cluster` 容器组
3. 展开可以看到 3 个容器：
   - redis-master
   - redis-slave-1
   - redis-slave-2
4. 点击容器名称可以查看日志和状态

## 三、验证集群

### 1. 验证主从复制

#### 方法一：使用 PowerShell

```powershell
# 查看主节点信息
docker exec redis-master redis-cli info replication

# 查看从节点 1 信息
docker exec redis-slave-1 redis-cli info replication

# 查看从节点 2 信息
docker exec redis-slave-2 redis-cli info replication
```

#### 方法二：使用 Redis CLI 交互模式

```powershell
# 连接主节点
docker exec -it redis-master redis-cli

# 在 Redis CLI 中执行
127.0.0.1:6379> info replication
```

预期输出：

```
# Replication
role:master
connected_slaves:2
slave0:ip=172.30.0.3,port=6379,state=online,offset=14,lag=0
slave1:ip=172.30.0.4,port=6379,state=online,offset=14,lag=0
```

### 2. 测试数据同步

#### 在主节点写入数据

```powershell
# 连接主节点
docker exec -it redis-master redis-cli

# 写入测试数据
127.0.0.1:6379> set test_key "Hello Redis Cluster"
OK
127.0.0.1:6379> set user:1 "Alice"
OK
127.0.0.1:6379> exit
```

#### 在从节点读取数据

```powershell
# 连接从节点 1
docker exec -it redis-slave-1 redis-cli

# 读取测试数据
127.0.0.1:6379> get test_key
"Hello Redis Cluster"
127.0.0.1:6379> get user:1
"Alice"
127.0.0.1:6379> exit
```

```powershell
# 连接从节点 2
docker exec -it redis-slave-2 redis-cli

# 读取测试数据
127.0.0.1:6379> get test_key
"Hello Redis Cluster"
127.0.0.1:6379> exit
```

### 3. 验证从节点只读

```powershell
# 连接从节点
docker exec -it redis-slave-1 redis-cli

# 尝试写入（应该失败）
127.0.0.1:6379> set test_write "should fail"
(error) READONLY You can't write against a read only replica.
```

## 四、使用 Redis Insight 可视化管理（可选）

### 1. 拉取 Redis Insight 镜像

```powershell
docker run -d --name redis-insight -p 8001:8001 redislabs/redisinsight:latest
```

### 2. 访问 Redis Insight

打开浏览器访问：http://localhost:8001

### 3. 添加 Redis 实例

在 Redis Insight 中添加 Redis 连接：

- **Host**: `localhost` 或 `127.0.0.1`
- **Port**: `6379`（主节点）
- **Name**: `Redis Master`

重复添加从节点：

- **Port**: `6380`（从节点 1）
- **Port**: `6381`（从节点 2）

## 五、常用操作命令

### 1. 启动集群

```powershell
cd "D:\Java\project One\docker-redis-cluster"
docker-compose up -d
```

### 2. 停止集群

```powershell
docker-compose down
```

### 3. 重启集群

```powershell
docker-compose restart
```

### 4. 查看日志

```powershell
# 查看所有容器日志
docker-compose logs -f

# 查看主节点日志
docker logs -f redis-master

# 查看从节点日志
docker logs -f redis-slave-1
docker logs -f redis-slave-2
```

### 5. 进入容器

```powershell
# 进入主节点容器
docker exec -it redis-master /bin/sh

# 进入从节点容器
docker exec -it redis-slave-1 /bin/sh
docker exec -it redis-slave-2 /bin/sh
```

### 6. 连接 Redis CLI

```powershell
# 连接主节点
docker exec -it redis-master redis-cli

# 连接从节点 1
docker exec -it redis-slave-1 redis-cli

# 连接从节点 2
docker exec -it redis-slave-2 redis-cli
```

### 7. 查看容器状态

```powershell
docker-compose ps
```

### 8. 查看资源使用

```powershell
docker stats redis-master redis-slave-1 redis-slave-2
```

## 六、在 Docker Desktop 中管理

### 1. 查看容器

打开 Docker Desktop：

1. 点击左侧 "Containers"
2. 找到 `docker-redis-cluster` 容器组
3. 可以看到容器的运行状态、CPU、内存使用情况

### 2. 查看日志

在 Docker Desktop 中：

1. 点击容器名称
2. 选择 "Logs" 标签
3. 可以实时查看日志输出

### 3. 进入容器终端

在 Docker Desktop 中：

1. 点击容器名称
2. 选择 "Exec" 标签
3. 点击 "Launch" 进入容器终端

### 4. 停止/启动容器

在 Docker Desktop 中：

1. 点击容器右侧的停止/启动按钮
2. 或者右键容器选择 "Stop" / "Start"

## 七、配置 Spring Boot 连接

### 1. 修改 application.yml

在你的微服务项目中修改 Redis 配置：

```yaml
spring:
  data:
    redis:
      host: localhost  # 或 127.0.0.1
      port: 6379       # 主节点端口
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

### 2. 测试连接

创建测试类：

```java
@SpringBootTest
public class RedisTest {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Test
    public void testConnection() {
        // 写入数据
        redisTemplate.opsForValue().set("test_key", "Hello Redis");
        
        // 读取数据
        String value = redisTemplate.opsForValue().get("test_key");
        System.out.println("Value: " + value);
        
        // 验证主从同步
        // 在从节点也能读取到数据
    }
}
```

## 八、故障排查

### 问题 1：容器无法启动

**检查步骤：**

```powershell
# 查看容器日志
docker-compose logs

# 查看容器状态
docker-compose ps
```

**解决方案：**

- 检查端口是否被占用
- 检查配置文件路径是否正确
- 检查 Docker Desktop 是否正常运行

### 问题 2：端口被占用

**检查端口占用：**

```powershell
# 检查 6379 端口
netstat -ano | findstr :6379

# 检查 6380 端口
netstat -ano | findstr :6380

# 检查 6381 端口
netstat -ano | findstr :6381
```

**解决方案：**

- 关闭占用端口的程序
- 或修改 docker-compose.yml 中的端口映射

### 问题 3：从节点无法连接主节点

**检查步骤：**

```powershell
# 查看从节点日志
docker logs redis-slave-1

# 检查网络连接
docker network ls
docker network inspect docker-redis-cluster_redis-cluster
```

**解决方案：**

- 确认主节点已启动
- 检查网络配置
- 重启容器

### 问题 4：数据不同步

**检查步骤：**

```powershell
# 查看主节点复制信息
docker exec redis-master redis-cli info replication

# 查看从节点复制信息
docker exec redis-slave-1 redis-cli info replication
```

**解决方案：**

- 检查主从配置
- 重启从节点容器

## 九、性能监控

### 1. 使用 Docker Desktop 监控

在 Docker Desktop 中：

1. 点击容器名称
2. 查看 "Stats" 标签
3. 可以看到 CPU、内存、网络、磁盘使用情况

### 2. 使用 Redis 命令监控

```powershell
# 查看内存使用
docker exec redis-master redis-cli info memory

# 查看客户端连接
docker exec redis-master redis-cli client list

# 查看慢查询日志
docker exec redis-master redis-cli slowlog get 10
```

### 3. 实时监控

```powershell
# 实时监控 Redis 命令
docker exec -it redis-master redis-cli monitor
```

## 十、备份与恢复

### 1. 手动备份

```powershell
# 触发 RDB 快照
docker exec redis-master redis-cli BGSAVE

# 复制 RDB 文件到本地
docker cp redis-master:/data/dump.rdb ./backup/dump-$(Get-Date -Format 'yyyyMMdd').rdb
```

### 2. 定时备份脚本

创建 `backup-redis.ps1`：

```powershell
# 备份目录
$BACKUP_DIR = ".\backup"
New-Item -ItemType Directory -Force -Path $BACKUP_DIR

# 日期
$DATE = Get-Date -Format 'yyyyMMdd_HHmmss'

# 触发备份
docker exec redis-master redis-cli BGSAVE

# 等待备份完成
Start-Sleep -Seconds 5

# 复制备份文件
docker cp redis-master:/data/dump.rdb "$BACKUP_DIR\dump-$DATE.rdb"

# 删除 7 天前的备份
Get-ChildItem $BACKUP_DIR -Filter "dump-*.rdb" | 
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } | 
    Remove-Item -Force

Write-Host "Backup completed: dump-$DATE.rdb"
```

运行备份：

```powershell
.\backup-redis.ps1
```

### 3. 数据恢复

```powershell
# 停止 Redis
docker-compose stop redis-master

# 复制备份文件
docker cp .\backup\dump-20240101.rdb redis-master:/data/dump.rdb

# 启动 Redis
docker-compose start redis-master
```

## 十一、清理环境

### 1. 停止并删除容器

```powershell
docker-compose down
```

### 2. 删除数据卷

```powershell
docker-compose down -v
```

### 3. 删除数据目录

```powershell
Remove-Item -Recurse -Force redis-data
```

### 4. 删除所有 Redis 相关资源

```powershell
# 停止所有容器
docker-compose down

# 删除容器
docker rm -f redis-master redis-slave-1 redis-slave-2

# 删除镜像（可选）
docker rmi redis:7.2-alpine

# 删除网络
docker network rm docker-redis-cluster_redis-cluster
```

## 十二、常见问题 FAQ

### Q1: Docker Desktop 启动慢？

**A:** 增加资源分配：
- 打开 Docker Desktop → Settings → Resources
- 增加 CPU 和 Memory

### Q2: 如何在 WSL2 中使用？

**A:** Docker Desktop 默认使用 WSL2 后端，可以直接在 WSL2 中运行命令：

```bash
# 进入 WSL2
wsl

# 进入项目目录
cd /mnt/d/Java/project\ One/docker-redis-cluster

# 启动集群
docker-compose up -d
```

### Q3: 如何从其他机器访问？

**A:** 修改 docker-compose.yml，将端口绑定到所有网络接口：

```yaml
ports:
  - "0.0.0.0:6379:6379"
```

**注意：** 生产环境需要配置防火墙和密码。

### Q4: 如何查看容器 IP？

**A:**

```powershell
# 查看容器网络信息
docker inspect redis-master | findstr IPAddress

# 或查看所有容器 IP
docker network inspect docker-redis-cluster_redis-cluster
```

## 十三、总结

通过 Docker Desktop 部署 Redis 主从集群，你可以：

✅ 使用图形界面管理容器
✅ 实时查看日志和监控
✅ 方便地启动、停止、重启容器
✅ 快速搭建开发和测试环境

**下一步建议：**

1. 配置 Redis 密码提高安全性
2. 使用 Redis Insight 进行可视化管理
3. 配置 Spring Boot 应用连接 Redis 集群
4. 测试主从同步和读写分离

现在你可以开始部署了！如果遇到任何问题，可以查看日志或参考故障排查部分。
