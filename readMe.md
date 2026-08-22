# Mall-Cloud 商城微服务系统

基于 Spring Cloud + Spring Cloud Alibaba 构建的微服务商城系统，涵盖商品、订单、支付、秒杀、搜索等核心业务模块。

## 技术栈

| 类别 | 技术 |
|---|---|
| 基础框架 | Spring Boot 3.3.6 / JDK 21 / Maven |
| 微服务 | Spring Cloud 2023.0.1 + Alibaba 2023.0.1.0 |
| 注册/配置中心 | Nacos |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer |
| ORM | MyBatis-Plus 3.5.5 + MySQL 8 |
| 缓存 | Redis（主从读写分离）+ Redisson + Caffeine |
| 消息队列 | RabbitMQ（延迟队列 + 死信队列）|
| 流控降级 | Sentinel |
| 认证 | JWT + 自定义拦截器 |
| API 文档 | Knife4j 4.4.0 |
| 支付 | 支付宝沙箱（电脑网页支付）|
| 工具 | Hutool / Lombok / MapStruct |

## 系统架构

```
                    ┌──────────────┐
                    │   客户端      │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ mall-Project  │  聚合服务 (8080)
                    │  (BFF/网关)   │  JWT 拦截 + Feign 转发
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
  ┌─────▼─────┐    ┌──────▼──────┐    ┌──────▼──────┐
  │ user-svc  │    │ product-svc │    │  order-svc  │
  │  (8081)   │    │   (8083)    │    │   (8085)    │
  └───────────┘    └─────────────┘    └─────────────┘
  ┌───────────┐    ┌─────────────┐    ┌─────────────┐
  │ cart-svc  │    │ payment-svc │    │ seckill-svc │
  │  (8084)   │    │   (8089)    │    │   (8088)    │
  └───────────┘    └─────────────┘    └─────────────┘
  ┌───────────┐    ┌─────────────┐    ┌─────────────┐
  │search-svc │    │ coupon-svc  │    │ comment-svc │
  │  (8087)   │    │   (8089)    │    │   (8090)    │
  └───────────┘    └─────────────┘    └─────────────┘
  ┌───────────┐    ┌─────────────┐
  │category-svc│   │  cache-svc  │
  │  (8082)   │    │   (8091)    │
  └───────────┘    └─────────────┘
```

## 核心设计亮点

### 1. 秒杀系统 — 三层防超卖

```
请求 → 用户级Redis计数器限流 → Redisson分布式锁 → Lua预减库存 → DB乐观锁扣减 → MQ异步落单
```

- **限流**：用户级 Redis 计数器（60s/10次）+ 令牌桶
- **防超卖**：Lua 脚本原子预减库存 + DB 乐观锁 `WHERE stock > 0`
- **削峰**：MQ 异步处理秒杀订单，保护数据库
- **幂等**：`seckill:user:{userId}:{seckillId}` 防重复下单

### 2. 缓存三剑客

| 问题 | 方案 | 实现 |
|---|---|---|
| 缓存穿透 | 参数校验 + 空值缓存 | 查不到的数据缓存 null，TTL=5min |
| 缓存击穿 | Redisson 分布式锁重建 | 热点 Key 过期时加锁重建，其他线程等待 |
| 缓存雪崩 | 随机过期 + 预热 + 多级缓存 | 10-40min 随机 TTL + Caffeine 本地缓存 + 启动预热 |

### 3. 订单超时自动取消 — RabbitMQ 死信队列

```
下单 → 发送到延迟交换机(order.delay.exchange) → TTL到期 → 进入死信队列 → 消费者取消订单 + 恢复库存
```

- 延迟队列配置 `x-dead-letter-exchange` + `x-message-ttl`
- 超时后自动取消订单、释放库存、退回优惠券
- 支付成功回调也通过 MQ 通知订单服务增加销量

### 4. Redis 主从 + 读写分离

- 一主两从架构（6379/6380/6381）
- Redisson 配置 `ReadMode.SLAVE`，读请求走从节点
- Redis 不可用时自动降级到数据库查询

### 5. 公共模块抽取 — mall-common

统一封装 Result、PageResult、JwtUtil、RedisService、GlobalExceptionHandler、BusinessException，消除 12 个服务间的代码重复。

## 快速启动

### 环境依赖

- JDK 21+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x
- RabbitMQ 3.x
- Sentinel Dashboard（可选）

### 启动顺序

1. 启动 Nacos：`nacos/bin/startup.cmd -m standalone`
2. 启动 Redis
3. 启动 RabbitMQ
4. 启动各微服务（按依赖顺序）：
   - mall-user-service → mall-category-service → mall-product-service
   - mall-cart-service → mall-order-service → mall-payment-service
   - mall-seckill-service → mall-search-service → mall-coupon-service
   - mall-comment-service → mall-cache-service
   - mall-Project（聚合服务最后启动）
5. 访问 API 文档：`http://localhost:8080/doc.html`

### 端口一览

| 服务 | 端口 |
|---|---|
| mall-Project（聚合） | 8080 |
| mall-user-service | 8081 |
| mall-category-service | 8082 |
| mall-product-service | 8083 |
| mall-cart-service | 8084 |
| mall-order-service | 8085 |
| mall-search-service | 8087 |
| mall-seckill-service | 8088 |
| mall-payment-service | 8089 |
| mall-coupon-service | 8089 |
| mall-comment-service | 8090 |
| mall-cache-service | 8091 |
| Nacos | 8848 |
| Sentinel Dashboard | 8080 |
| RabbitMQ | 5672 / 15672 |
