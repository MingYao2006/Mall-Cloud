# Mall-Cloud 商城微服务系统

基于 Spring Cloud + Spring Cloud Alibaba 构建的微服务商城系统，涵盖商品、订单、支付、秒杀、搜索、优惠券、评论等核心业务，内置 AI 智能导购服务，并提供 Docker Compose 一键部署方案。采用 C 端 / B 端双聚合（BFF）架构，通过网关层实现流量隔离与角色鉴权。

> 本项目为学习实践项目，支付对接的是支付宝沙箱环境，不涉及真实资金。

## 技术栈

| 类别 | 技术 |
|---|---|
| 基础框架 | Spring Boot 3.3.6 / JDK 21+ / Maven 多模块 |
| 微服务 | Spring Cloud 2023.0.1 + Alibaba 2023.0.1.0 |
| 注册/配置中心 | Nacos 2.x |
| API 网关 | Spring Cloud Gateway（C/B 端流量隔离 + JWT 鉴权） |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer |
| ORM | MyBatis-Plus 3.5.5 + MySQL 8 |
| 缓存 | Redis 主从读写分离 + Redisson 分布式锁 + Caffeine 本地缓存（优惠券模块两级缓存） |
| 消息队列 | RabbitMQ（延迟队列 + 死信队列 + 消息确认） |
| 流控降级 | Sentinel |
| 全文搜索 | Elasticsearch 7.17 |
| 认证 | JWT（网关层 + 聚合层双重校验） |
| AI 导购 | Python FastAPI + DeepSeek LLM + Milvus 向量库 |
| 支付 | 支付宝沙箱（RSA2 密钥签名） |
| API 文档 | Knife4j 4.4.0 |
| 前端 | Vue 3 + Element Plus + Pinia + Vue Router + Vite |
| 链路追踪 | SkyWalking 9.7（Java Agent 无侵入埋点） |
| 部署 | Docker Compose + Nginx + Cloudflare Tunnel |

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
  └───────┬──────────────┬───────────────┘
          │              │
  ┌───────▼──────┐  ┌────▼───────────────┐
  │  mall-Project │  │ mall-project-admin │
  │  C 端聚合 BFF │  │  B 端聚合 BFF      │
  │    (9090)     │  │     (9091)         │
  └───────┬──────┘  └────────┬───────────┘
          │     OpenFeign    │
     ┌────┴─────┬───────┬────┴─────┐
     │          │       │          │
  ┌──▼───┐  ┌───▼───┐ ┌──▼────┐  ┌─▼──────┐
  │ user │  │product│ │ order │  │  ...   │   11 个业务微服务
  │ 8081 │  │  8083 │  │ 8085  │  │        │   （Nacos 注册发现）
  └──────┘  └───────┘  └───────┘  └────────┘
```

### 架构说明

- **网关层（mall-gateway, 9000）**：统一入口，C 端 `/api/c/**` 与 B 端 `/api/admin/**` 流量隔离；JWT 验签后通过 `X-User-Id` / `X-Username` / `X-Role` 请求头下发用户信息；B 端强制校验 `role==1`，非管理员返回 403。
- **C 端聚合（mall-Project, 9090）**：面向消费者的 BFF 层，通过 Feign 转发到各微服务，不直接访问数据库。
- **B 端聚合（mall-project-admin, 9091）**：面向管理员的 BFF 层，JWT 拦截器二次校验 `role==1`，提供商品/分类/订单/评论/搜索等管理接口。
- **业务微服务（11 个）**：各自独立部署、独立访问数据库，通过 Nacos 注册发现。
- **mall-common**：统一封装 `Result` / `PageResult` / `JwtUtil` / `RedisService` / `GlobalExceptionHandler`，消除服务间重复代码。
- **mall-ai-agent**：独立 Python 服务，基于 LLM + 向量检索实现智能导购问答，经网关接入 C 端。

## 核心设计亮点

### 1. 秒杀系统 — 三层防超卖

```
请求 → 用户级 Redis 计数器限流 → Redisson 分布式锁 → Lua 预减库存 → DB 乐观锁扣减 → MQ 异步落单
```

- **限流**：用户级 Redis 计数器（INCR + EXPIRE）拦截恶意刷接口
- **防超卖**：Lua 脚本原子预减库存 + DB 乐观锁 `WHERE stock > 0` 两级扣减兜底
- **削峰**：MQ 异步处理秒杀订单，保护数据库
- **幂等**：基于 messageId 的 Redis 幂等控制，防止重复下单与重复补偿

### 2. 缓存三剑客

| 问题 | 方案 | 实现 |
|---|---|---|
| 缓存穿透 | 参数校验 + 空值缓存 | 查不到的数据缓存 null，TTL=5min |
| 缓存击穿 | Redisson 分布式锁重建 | 热点 Key 过期时加锁重建，其他线程等待 |
| 缓存雪崩 | 随机过期 + 预热 | 商品缓存 10-40min 随机 TTL + 启动预热（mall-Project 的 CacheWarmupService）；优惠券模块另用 Caffeine + Redis 两级缓存 |

### 3. 订单超时自动取消 — RabbitMQ 死信队列

```
下单 → 延迟交换机（x-message-ttl）→ 到期进入死信队列 → 消费者取消订单 + 回补库存 + 退回优惠券
```

- 生产者确认 + 消费者手动 ACK，消息不丢失
- 消费端二次校验订单状态，保证状态机一致性

### 4. Redis 主从 + 读写分离

- 一主两从架构（6379 / 6380 / 6381），另有哨兵与 Cluster 配置可选
- Redisson 读请求走从节点，写走主节点
- Redis 不可用时自动降级到数据库查询

### 5. AI 智能导购（mall-ai-agent）

- FastAPI + DeepSeek 对话，工具调用（Function Calling）查询商品/订单数据
- Milvus 向量检索 + 全文搜索的混合召回

## 快速启动

### 环境依赖

- JDK 21+，Maven 3.8+
- MySQL 8.0+、Redis 6.0+、RabbitMQ 3.x、Nacos 2.x
- Node.js 18+（前端）
- Elasticsearch 7.17（搜索服务，可选）
- Python 3.11+（AI 导购服务，可选）

### 环境变量

所有密钥均通过环境变量注入，仓库内不含任何明文密钥。本地开发可复制 `deploy/.env.example` 按需填写：

| 变量 | 用途 |
|---|---|
| `MYSQL_PASSWORD` | MySQL 密码 |
| `JWT_SECRET` | JWT 签名密钥（所有服务必须一致） |
| `NACOS_PASSWORD` | Nacos 鉴权密码 |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ 账号 |
| `ALIPAY_PRIVATE_KEY` | 支付宝应用私钥（沙箱控制台获取） |
| `ALIPAY_PUBLIC_KEY` | 支付宝应用公钥（上传到控制台的那串） |
| `ALIPAY_ALIPAY_PUBLIC_KEY` | 支付宝公钥（控制台展示，用于回调验签） |

### 初始化数据库

依次执行 `mall-Project/sql/` 下的脚本：

```
mall_database.sql → coupon_tables.sql → add_wx_login.sql → fix_seckill_log_table.sql
```

### 启动顺序

> ⚠️ 严格按顺序启动，后置服务依赖前置服务的注册信息。

1. **基础设施**：Nacos（8848）→ Redis 主从（6379/6380/6381）→ RabbitMQ（5672）→ MySQL（3306）
2. **业务微服务**：user(8081) / category(8082) / product(8083) / cart(8084) / order(8085) / search(8087) / seckill(8088) / payment(8089) / comment(8090) / cache(8091) / coupon(8092)
3. **网关**：mall-gateway（9000），等待微服务全部注册后启动
4. **聚合服务**：mall-Project（9090）、mall-project-admin（9091）
5. **AI 导购（可选）**：`cd mall-ai-agent && python -m uvicorn app.main:app --port 8095`
6. **前端**：

```bash
# C 端
cd mall-Project/mall-frontend && npm install && npm run dev   # http://localhost:3000
# B 端
cd mall-admin-frontend && npm install && npm run dev         # http://localhost:3001
```

### 访问入口

| 入口 | 地址 | 说明 |
|---|---|---|
| C 端前台 | http://localhost:3000 | 消费者商城 |
| B 端后台 | http://localhost:3001 | 管理员后台 |
| C 端 API 文档 | http://localhost:9090/doc.html | Knife4j |
| B 端 API 文档 | http://localhost:9091/doc.html | Knife4j |
| Nacos 控制台 | http://localhost:8848/nacos | 服务注册发现 |
| RabbitMQ 管理 | http://localhost:15672 | 队列监控 |

### 服务器部署（可选）

`deploy/` 目录提供完整的 Docker Compose 生产部署方案：MySQL / Redis / RabbitMQ / Nacos / Elasticsearch / SkyWalking 全容器化，Nginx 托管前端，Cloudflare Tunnel 免公网 IP 暴露：

```bash
cd deploy
cp .env.example .env    # 填写密钥
bash install.sh         # 一键构建并启动
```

### 管理员账号

在 MySQL `mall` 库的 `user` 表中，将目标用户的 `role` 设为 `1`：

```sql
UPDATE user SET role = 1 WHERE username = '你的管理员用户名';
```

## 端口一览

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
| mall-search-service | 8087 | 搜索服务（Elasticsearch） |
| mall-seckill-service | 8088 | 秒杀服务 |
| mall-payment-service | 8089 | 支付服务（支付宝沙箱） |
| mall-comment-service | 8090 | 评论服务 |
| mall-cache-service | 8091 | 缓存预热服务 |
| mall-coupon-service | 8092 | 优惠券服务 |
| mall-ai-agent | 8095 | AI 导购服务（FastAPI） |
| mall-frontend | 3000 | C 端前端（Vite） |
| mall-admin-frontend | 3001 | B 端前端（Vite） |
| Nacos | 8848 | 注册/配置中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379/6380/6381 | Redis 主从 |
| RabbitMQ | 5672 / 15672 | 消息队列 / 管理界面 |
| Sentinel Dashboard | 8080 | 流控控制台（可选） |
| SkyWalking UI / OAP | 8080 / 11800 / 12800 | 链路追踪（可选，compose profile: skywalking） |
