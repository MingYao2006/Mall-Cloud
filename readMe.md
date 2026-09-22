# Mall-Cloud 商城微服务系统

基于 Spring Cloud + Spring Cloud Alibaba 构建的微服务商城系统，涵盖商品、订单、支付、秒杀、搜索等核心业务模块。采用 C 端 / B 端双聚合服务架构，通过网关层实现流量隔离与角色鉴权。

## 技术栈

| 类别 | 技术 |
|---|---|
| 基础框架 | Spring Boot 3.3.6 / JDK 21 / Maven |
| 微服务 | Spring Cloud 2023.0.1 + Alibaba 2023.0.1.0 |
| 注册/配置中心 | Nacos |
| API 网关 | Spring Cloud Gateway（C/B 端流量隔离 + JWT 鉴权） |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer |
| ORM | MyBatis-Plus 3.5.5 + MySQL 8 |
| 缓存 | Redis（主从读写分离）+ Redisson + Caffeine |
| 消息队列 | RabbitMQ（延迟队列 + 死信队列）|
| 流控降级 | Sentinel |
| 认证 | JWT + 自定义拦截器（网关层 + 聚合层双重校验） |
| API 文档 | Knife4j 4.4.0 |
| 支付 | 支付宝沙箱（电脑网页支付）|
| 前端 | Vue 3.5 + Element Plus + Pinia + Vue Router + Vite |
| 工具 | Hutool / Lombok / MapStruct |

## 系统架构

```
  C 端前端 (3000)              B 端前端 (3001)
       │                            │
       │ /api/c/**                  │ /api/admin/**
       ▼                            ▼
  ┌──────────────────────────────────────┐
  │         mall-gateway (9000)          │
  │  路由隔离 + JWT 验签 + Role 校验      │
  │  C 端白名单 / B 端 role==1 强制校验   │
  │  下发 X-User-Id / X-Username / X-Role│
  └───────┬──────────────┬───────────────┘
          │              │
  ┌───────▼──────┐  ┌────▼───────────────┐
  │ mall-Project │  │ mall-project-admin  │
  │  C 端聚合    │  │   B 端聚合          │
  │  (9090)      │  │  (9091)             │
  │  JWT拦截     │  │  JWT拦截+role==1    │
  │  Feign转发   │  │  Feign转发          │
  └───────┬──────┘  └────────┬────────────┘
          │                  │
          └────────┬─────────┘
                   │ OpenFeign
     ┌─────────────┼─────────────────────┐
     │             │                     │
  ┌──▼───┐  ┌──────▼──────┐  ┌──────────▼───┐
  │user  │  │  product    │  │   order      │
  │8081  │  │   8083      │  │   8085       │
  └──────┘  └─────────────┘  └──────────────┘
  ┌──────┐  ┌─────────────┐  ┌──────────────┐
  │cart  │  │  payment    │  │   seckill    │
  │8084  │  │   8089      │  │   8088       │
  └──────┘  └─────────────┘  └──────────────┘
  ┌──────┐  ┌─────────────┐  ┌──────────────┐
  │search│  │  coupon     │  │   comment    │
  │8087  │  │   8092      │  │   8090       │
  └──────┘  └─────────────┘  └──────────────┘
  ┌──────┐  ┌─────────────┐
  │categ.│  │   cache     │
  │8082  │  │   8091      │
  └──────┘  └─────────────┘
```

### 架构说明

- **网关层（mall-gateway, 9000）**：统一入口，C 端路径 `/api/c/**` 与 B 端路径 `/api/admin/**` 流量隔离；JWT 验签后将用户信息通过 `X-User-Id` / `X-Username` / `X-Role` 请求头下发；B 端请求强制校验 `role==1`，非管理员返回 403。
- **C 端聚合（mall-Project, 9090）**：面向消费者的 BFF 层，仅保留只读查询接口，通过 Feign 转发到各微服务，不直接访问数据库。
- **B 端聚合（mall-project-admin, 9091）**：面向管理员的 BFF 层，JWT 拦截器二次校验 `role==1`，提供商品/分类/订单/评论/搜索等管理接口，通过 Feign 转发到各微服务。
- **底层微服务（12 个）**：各自独立部署、独立数据库访问，通过 Nacos 注册发现，由聚合服务通过 OpenFeign 调用。

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

- JDK 21+（项目实际使用 JDK 23.0.2）
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+（主从：6379/6380/6381）
- Nacos 2.x
- RabbitMQ 3.x
- Node.js 18+ / npm（前端构建）
- Sentinel Dashboard（可选）

### 启动顺序

> ⚠️ 严格按照以下顺序启动，后置服务依赖前置服务的注册信息。

#### 第一步：基础设施

1. **Nacos**：`nacos/bin/startup.cmd -m standalone`（端口 8848）
2. **Redis**：启动主从实例（6379 主 / 6380 从 / 6381 从）
3. **RabbitMQ**：`rabbitmq-server`（端口 5672，管理界面 15672）

#### 第二步：底层微服务（12 个）

按依赖关系分批启动，每批内可并行：

**批次 1 — 基础数据服务**
- `mall-user-service`（8081）→ `mall-category-service`（8082）→ `mall-product-service`（8083）

**批次 2 — 交易链路服务**
- `mall-cart-service`（8084）→ `mall-order-service`（8085）→ `mall-payment-service`（8089）

**批次 3 — 辅助业务服务**
- `mall-seckill-service`（8088）→ `mall-search-service`（8087）→ `mall-coupon-service`（8092）

**批次 4 — 公共服务**
- `mall-comment-service`（8090）→ `mall-cache-service`（8091）

#### 第三步：网关

- `mall-gateway`（9000）— 等待所有底层微服务注册到 Nacos 后启动

#### 第四步：聚合服务

- `mall-Project`（9090）— C 端聚合服务
- `mall-project-admin`（9091）— B 端聚合服务

> 两个聚合服务可并行启动，但必须在网关之后。

#### 第五步：前端

**C 端前端：**
```bash
cd mall-Project/mall-frontend      # 首次使用需安装依赖
npm run dev        # 端口 3000
```

**B 端前端：**
```bash
cd mall-admin-frontend      # 首次使用需安装依赖
npm install        # 首次使用需安装依赖
npm run dev        # 端口 3001
```

#### 第六步：访问验证

| 入口 | 地址 | 说明 |
|---|---|---|
| C 端前台 | http://localhost:3000 | 消费者商城界面 |
| B 端后台 | http://localhost:3001 | 管理员后台界面 |
| C 端 API 文档 | http://localhost:9090/doc.html | C 端聚合服务 Knife4j |
| B 端 API 文档 | http://localhost:9091/doc.html | B 端聚合服务 Knife4j |
| Nacos 控制台 | http://localhost:8848/nacos | 服务注册发现（nacos/nacos） |
| RabbitMQ 管理 | http://localhost:15672 | 队列监控（guest/guest） |

### 端口一览

| 服务 | 端口 | 说明 |
|---|---|---|
| mall-gateway | 9000 | API 网关，统一入口 |
| mall-Project | 9090 | C 端聚合服务（BFF） |
| mall-project-admin | 9091 | B 端聚合服务（BFF） |
| mall-user-service | 8081 | 用户服务 |
| mall-category-service | 8082 | 分类服务 |
| mall-product-service | 8083 | 商品服务 |
| mall-cart-service | 8084 | 购物车服务 |
| mall-order-service | 8085 | 订单服务 |
| mall-search-service | 8087 | 搜索服务（ES） |
| mall-seckill-service | 8088 | 秒杀服务 |
| mall-payment-service | 8089 | 支付服务（支付宝沙箱） |
| mall-comment-service | 8090 | 评论服务 |
| mall-cache-service | 8091 | 缓存预热服务 |
| mall-coupon-service | 8092 | 优惠券服务 |
| mall-frontend | 3000 | C 端前端（Vite Dev Server） |
| mall-admin-frontend | 3001 | B 端前端（Vite Dev Server） |
| Nacos | 8848 | 注册/配置中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379/6380/6381 | Redis 主从 |
| RabbitMQ | 5672 / 15672 | 消息队列 / 管理界面 |
| Sentinel Dashboard | 8080 | 流控降级控制台（可选） |

### 请求链路说明

**C 端请求链路：**
```
浏览器 → Vite 代理(/api/c) → 网关(9000) → 验签 + 白名单放行
  → StripPrefix=2 → mall-Project(9090) → JwtInterceptor 拦截
  → Feign 转发 → 底层微服务 → 数据库
```

**B 端请求链路：**
```
浏览器 → Vite 代理(/api/admin) → 网关(9000) → 验签 + role==1 校验
  → 下发 X-User-Id/X-Username/X-Role
  → StripPrefix=2 → mall-project-admin(9091) → AdminJwtInterceptor 二次验签 + role==1
  → Feign 转发 → 底层微服务 → 数据库
```

**支付回调链路（不经过鉴权）：**
```
支付宝服务器 → 网关(9000) /api/payment/alipay/notify
  → StripPrefix=1 → mall-Project(9090) /payment/alipay/notify
  → PaymentController.handleCallback → Feign 调用 payment-service
```

### 管理员账号配置

在 MySQL `mall` 数据库的 `user` 表中，将目标用户的 `role` 字段设为 `1`（管理员）。默认 `role=0` 为普通用户。

```sql
UPDATE user SET role = 1 WHERE username = '你的管理员用户名';
```
