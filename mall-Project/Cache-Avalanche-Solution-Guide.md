# 缓存雪崩解决方案完整指南

## 一、什么是缓存雪崩？

缓存雪崩是指**大量缓存在同一时间失效（过期）**，导致所有请求都打到数据库，造成数据库瞬间压力过大甚至崩溃的现象。

### 发生场景：
1. **大量缓存同时过期**：设置了相同的过期时间，导致同时失效
2. **Redis 服务宕机**：所有缓存失效，请求直接打到数据库
3. **系统重启**：缓存被清空，大量请求涌入

### 危害：
- 数据库瞬间压力过大，可能导致崩溃
- 系统响应时间大幅增加
- 用户体验严重下降
- 可能引发系统雪崩效应

## 二、解决方案

### 方案一：过期时间随机化（推荐）

**原理**：在基础过期时间上增加随机值，避免大量缓存同时过期。

#### 实现方式：

```java
// 基础过期时间：30 分钟
// 随机增加：0-30 分钟
// 最终过期时间：30-60 分钟

int baseTimeout = 30;
int randomTimeout = new Random().nextInt(30);
int totalTimeout = baseTimeout + randomTimeout;

redisTemplate.opsForValue().set(key, value, totalTimeout, TimeUnit.MINUTES);
```

#### 已实现位置：
- `ProductService.java` - 商品缓存已使用随机过期时间
- `CacheAvalancheService.java` - 提供了完整的随机过期时间工具类

#### 使用示例：

```java
// 方式一：基础时间 + 随机时间
cacheAvalancheService.setWithRandomExpire(key, value, 30, TimeUnit.MINUTES);

// 方式二：指定范围
cacheAvalancheService.setWithRandomExpire(key, value, 20, 40);  // 20-40 分钟
```

### 方案二：缓存预热

**原理**：系统启动时预加载热点数据到缓存，避免系统刚启动时大量请求打到数据库。

#### 实现位置：
- `CacheWarmupService.java` - 实现了 `CommandLineRunner` 接口，启动时自动预热

#### 预热内容：
1. 商品数据
2. 分类数据
3. 热点数据（热门商品、推荐商品等）

#### 手动触发预热：

```java
@Autowired
private CacheWarmupService cacheWarmupService;

// 手动触发预热
cacheWarmupService.manualWarmup();

// 清空所有缓存
cacheWarmupService.clearAllCache();
```

### 方案三：多级缓存

**原理**：使用本地缓存（Caffeine）+ Redis 缓存的两级缓存架构。

#### 优点：
1. **提高性能**：本地缓存命中率高，减少网络开销
2. **防止雪崩**：两级缓存过期时间不同
3. **提高可用性**：Redis 故障时仍可使用本地缓存

#### 实现位置：
- `MultiLevelCacheConfig.java` - 多级缓存配置
- `MultiLevelCacheService.java` - 多级缓存服务

#### 使用示例：

```java
@Autowired
private MultiLevelCacheService multiLevelCacheService;

// 查询数据（自动使用两级缓存）
Product product = multiLevelCacheService.get(
    "product:" + id,
    () -> productMapper.selectById(id),  // 数据加载器
    30  // Redis 过期时间（分钟）
);

// 更新缓存
multiLevelCacheService.put("product:" + id, product, 30);

// 删除缓存
multiLevelCacheService.evict("product:" + id);
```

### 方案四：逻辑过期（永不过期）

**原理**：缓存永不过期，但在数据中存储逻辑过期时间。访问时检查逻辑过期，异步更新缓存。

#### 实现位置：
- `CacheAvalancheService.java` - 提供了逻辑过期功能

#### 使用示例：

```java
// 设置逻辑过期（缓存永不过期）
cacheAvalancheService.setWithLogicalExpire(key, value, 30, TimeUnit.MINUTES);

// 获取数据（自动检查逻辑过期）
Object data = cacheAvalancheService.getWithLogicalExpire(key);
```

### 方案五：限流降级

**原理**：使用限流工具（如 Sentinel、Hystrix）限制并发请求，防止数据库压力过大。

#### 配置建议：
1. 配置 QPS 限流：限制每秒查询数量
2. 配置并发线程数限流：限制并发线程数
3. 配置熔断降级：当数据库压力过大时自动降级

### 方案六：Redis 高可用

**原理**：通过 Redis 主从复制、哨兵、集群等架构，提高 Redis 可用性。

#### 已实现：
- Redis 一主两从架构（本项目已实现）
- 配置文件：`application-dev.yaml`、`application-prod.yaml`

## 三、综合解决方案

### 推荐组合方案：

```
┌─────────────────────────────────────────────────┐
│          缓存雪崩综合防护方案                    │
├─────────────────────────────────────────────────┤
│                                                 │
│  1. 过期时间随机化 ✓                            │
│     └─ ProductService 已实现                    │
│                                                 │
│  2. 缓存预热 ✓                                  │
│     └─ CacheWarmupService 已实现                │
│                                                 │
│  3. 多级缓存 ✓                                  │
│     └─ MultiLevelCacheService 已实现            │
│                                                 │
│  4. Redis 高可用 ✓                              │
│     └─ 一主两从架构已实现                       │
│                                                 │
│  5. 缓存穿透防护 ✓                              │
│     └─ ProductService 已实现                    │
│                                                 │
│  6. 缓存击穿防护 ✓                              │
│     └─ ProductService 已实现                    │
│                                                 │
└─────────────────────────────────────────────────┘
```

## 四、最佳实践

### 1. 商品缓存示例（已实现）

```java
// ProductService.java 中的 findById 方法

// 1. 参数校验
if (id == null || id <= 0) {
    throw new RuntimeException("商品ID不能为空");
}

// 2. 检查空值缓存（防止缓存穿透）
if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
    throw new RuntimeException("商品不存在");
}

// 3. 尝试从缓存获取
Object cached = redisService.get(cacheKey);
if (cached != null) {
    return (Product) cached;
}

// 4. 使用分布式锁（防止缓存击穿）
RLock lock = redissonClient.getLock(lockKey);
lock.tryLock(3, 10, TimeUnit.SECONDS);

// 5. 查询数据库
Product product = productMapper.selectById(id);

// 6. 写入缓存（随机过期时间，防止缓存雪崩）
int randomExpire = 10 + (int)(Math.random() * 30);  // 10-40 分钟
redisService.set(cacheKey, product, randomExpire, TimeUnit.MINUTES);
```

### 2. 分类缓存示例

```java
// 使用多级缓存
@Autowired
private MultiLevelCacheService multiLevelCacheService;

public Category getCategoryById(Long id) {
    return multiLevelCacheService.get(
        "category:" + id,
        () -> categoryMapper.selectById(id),
        60  // 分类数据变化较少，设置较长过期时间
    );
}
```

### 3. 热点数据缓存示例

```java
// 使用缓存预热
@Autowired
private CacheWarmupService cacheWarmupService;

// 系统启动时自动预热
// 或手动触发预热
cacheWarmupService.manualWarmup();
```

## 五、监控与告警

### 1. 监控指标

```java
// 监控缓存命中率
@Autowired
private MultiLevelCacheService multiLevelCacheService;

// 获取本地缓存统计信息
String stats = multiLevelCacheService.getLocalCacheStats();
System.out.println(stats);
```

### 2. 建议监控项：
- 缓存命中率
- 缓存过期数量
- 数据库查询次数
- Redis 连接数
- Redis 内存使用率

## 六、测试验证

### 1. 测试缓存雪崩防护

```java
@Test
public void testCacheAvalanchePrevention() {
    // 模拟大量缓存同时失效
    for (int i = 0; i < 100; i++) {
        String key = "test:" + i;
        // 使用随机过期时间
        cacheAvalancheService.setWithRandomExpire(key, "value", 30, TimeUnit.MINUTES);
    }
    
    // 验证过期时间不同
    // 确保不会同时失效
}
```

### 2. 测试缓存预热

```java
@Test
public void testCacheWarmup() {
    // 手动触发预热
    cacheWarmupService.manualWarmup();
    
    // 验证缓存已预热
    Object cached = redisTemplate.opsForValue().get("product:1");
    assertNotNull(cached);
}
```

## 七、注意事项

1. **随机过期时间范围**：建议在基础时间的 50%-150% 范围内随机
2. **缓存预热时机**：系统启动时、定时任务、手动触发
3. **多级缓存一致性**：更新数据时需同时更新两级缓存
4. **本地缓存容量**：根据内存情况设置合适的容量
5. **Redis 高可用**：建议使用主从复制或哨兵模式

---

**作者**: 67161  
**创建时间**: 2026-05-29  
**项目**: mall-Project