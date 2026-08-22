# Redis 缓存实现说明

## 一、实现概述

本项目已在 `ProductService` 中完整实现了 Redis 缓存功能，采用 **Cache-Aside Pattern（旁路缓存）** 模式。

## 二、已实现缓存的方法

### 1. 读方法（查询操作 - 使用缓存）

| 方法名 | 缓存Key | 过期时间 | 说明 |
|--------|---------|----------|------|
| `findById` | `product:detail:{id}` | 30分钟 | 商品详情查询 |
| `findAll` | `product:list:all` | 10分钟 | 所有商品列表 |
| `listProductsByCategory` | `product:list:category:{categoryId}` | 10分钟 | 按分类查询 |
| `listProductsByStatus` | `product:list:status:{status}` | 10分钟 | 按状态查询 |
| `listProductsByPage` | `product:page:{pageNum}:{pageSize}` | 5分钟 | 分页查询 |
| `checkProductExists` | `product:exists:{id}` | 30分钟 | 商品存在性校验 |
| `checkStockEnough` | `product:stock:{id}` | 1分钟 | 库存充足性校验 |

### 2. 写方法（增删改操作 - 清除缓存）

| 方法名 | 清除的缓存 | 说明 |
|--------|-----------|------|
| `add` | 列表缓存、分页缓存 | 添加商品 |
| `update` | 详情缓存、列表缓存、分页缓存 | 更新商品 |
| `delete` | 详情缓存、列表缓存、分页缓存 | 删除商品 |
| `updateStock` | 详情缓存、库存缓存 | 更新库存 |
| `decreaseStock` | 详情缓存、库存缓存 | 扣减库存 |
| `increaseSales` | 详情缓存 | 增加销量 |
| `publishProduct` | 详情缓存、列表缓存、分页缓存 | 上架商品 |
| `unpublishProduct` | 详情缓存、列表缓存、分页缓存 | 下架商品 |

## 三、缓存Key命名规范

```
product:detail:{id}           # 商品详情
product:list:all              # 所有商品列表
product:list:category:{id}    # 分类商品列表
product:list:status:{status}  # 状态商品列表
product:page:{pageNum}:{pageSize}  # 分页数据
product:stock:{id}            # 商品库存
product:exists:{id}           # 商品存在性
```

## 四、核心实现代码

### 1. RedisService 工具类

提供了以下核心方法：
- `set(key, value, timeout, unit)` - 设置缓存
- `get(key)` - 获取缓存
- `delete(key)` - 删除缓存
- `deleteByPattern(pattern)` - 批量删除缓存（通配符）
- `hasKey(key)` - 判断key是否存在
- `expire(key, timeout, unit)` - 设置过期时间

### 2. 缓存读取流程（以 findById 为例）

```java
public Product findById(Long id) {
    // 1. 先查缓存
    String key = PRODUCT_CACHE_KEY + id;
    Object cached = redisService.get(key);
    if (cached != null) {
        return (Product) cached;
    }
    
    // 2. 缓存没有，查数据库
    Product product = productMapper.selectById(id);
    if (product == null) {
        throw new RuntimeException("商品不存在");
    }
    
    // 3. 写入缓存
    redisService.set(key, product, CACHE_TIMEOUT, TimeUnit.MINUTES);
    
    return product;
}
```

### 3. 缓存清除流程（以 update 为例）

```java
public void update(Product product) {
    // 1. 更新数据库
    productMapper.update(product);
    
    // 2. 清除缓存
    clearProductDetailCache(product.getId());
    clearProductListCache(product.getCategoryId());
}
```

## 五、缓存清除辅助方法

### 1. clearProductDetailCache
清除商品详情相关的所有缓存：
- `product:detail:{id}`
- `product:{id}`
- `product:stock:{id}`
- `product:exists:{id}`

### 2. clearProductListCache
清除商品列表相关的所有缓存：
- `product:list:all`
- `product:list:category:{categoryId}`
- `product:list:status:*`（通配符批量删除）
- `product:page:*`（通配符批量删除）

## 六、注意事项

### 1. 异常处理
缓存操作失败不会影响业务主流程，会记录错误日志：
```java
try {
    redisService.delete(key);
} catch (Exception e) {
    System.err.println("清除缓存失败: " + e.getMessage());
}
```

### 2. 库存缓存特殊性
库存数据实时性要求高，过期时间设置为 **1分钟**，避免数据不一致。

### 3. 缓存穿透防护
如果商品不存在，直接抛出异常，不缓存空对象（可根据业务需求调整）。

## 七、后续优化建议

### 1. 缓存穿透防护
可以使用布隆过滤器或缓存空对象（短过期时间）防止频繁查询不存在的数据。

### 2. 缓存击穿防护
对于热点商品，可以使用互斥锁或设置永不过期 + 异步更新策略。

### 3. 缓存雪崩防护
可以为不同的缓存Key设置不同的过期时间，避免同时失效。

### 4. 分布式锁
在高并发场景下（如秒杀），可以使用 Redis 分布式锁 + 原子操作（DECRBY）防止超卖。

### 5. 监控告警
建议添加缓存监控：
- 缓存命中率
- 内存使用情况
- 慢查询日志

## 八、测试验证

### 1. 启动 Redis
确保 Redis 服务已启动：
```bash
redis-server
```

### 2. 测试缓存效果
```bash
# 第一次查询 - 会查询数据库
GET /api/product/1

# 第二次查询 - 会从缓存获取（响应更快）
GET /api/product/1

# 更新商品 - 会清除缓存
PUT /api/product/1

# 再次查询 - 会重新查询数据库并缓存
GET /api/product/1
```

### 3. 查看 Redis 中的缓存数据
```bash
redis-cli
> keys product:*
> get "product:detail:1"
```
