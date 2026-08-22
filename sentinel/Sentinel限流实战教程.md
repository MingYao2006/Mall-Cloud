# Sentinel 限流实战教程

## 📚 教程概述

本教程将带您从零开始，一步步学习如何使用 Sentinel 进行接口限流保护。

---

## 第一步：启动 Sentinel Dashboard 控制台

### 1.1 下载 Sentinel Dashboard

访问 Sentinel 官方 GitHub 发布页面：
```
https://github.com/alibaba/Sentinel/releases
```

找到最新版本（推荐 1.8.8），下载 `sentinel-dashboard-1.8.8.jar`

### 1.2 启动控制台

打开命令行窗口，进入 jar 包所在目录，执行：

```bash
# Windows
java -jar sentinel-dashboard-1.8.8.jar

# Linux/Mac
java -jar sentinel-dashboard-1.8.8.jar

# 后台启动（Linux/Mac）
nohup java -jar sentinel-dashboard-1.8.8.jar > sentinel.log 2>&1 &

# 指定端口启动
java -jar sentinel-dashboard-1.8.8.jar --server.port=8080
```

### 1.3 访问控制台

启动成功后，打开浏览器访问：
```
http://localhost:8080
```

默认账号密码：
- 用户名：`sentinel`
- 密码：`sentinel`

**✅ 第一步完成标志**：能够成功登录 Sentinel 控制台

---

## 第二步：启动您的微服务

### 2.1 启动顺序

按照以下顺序启动服务：

```
1. Nacos（端口 8848）
   ↓
2. Sentinel Dashboard（端口 8080）
   ↓
3. Redis（端口 6379）
   ↓
4. MySQL（端口 3306）
   ↓
5. RabbitMQ（端口 5672）
   ↓
6. 微服务
   - mall-product-service (8082)
   - mall-order-service (8083)
   - mall-seckill-service (8087)
   - mall-payment-service (8088)
```

### 2.2 验证服务注册

启动服务后，访问 Sentinel 控制台，在左侧菜单中应该能看到已注册的服务列表。

**注意**：Sentinel 是懒加载机制，服务启动后需要访问一次接口，才会在控制台显示。

**✅ 第二步完成标志**：Sentinel 控制台左侧菜单显示服务列表

---

## 第三步：在代码中定义资源

### 3.1 方式一：使用 @SentinelResource 注解（推荐）

在需要限流的接口方法上添加 `@SentinelResource` 注解。

#### 示例 1：秒杀接口限流

在 `mall-seckill-service` 中创建或修改 Controller：

```java
package com.mall.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    /**
     * 秒杀下单接口
     * 
     * @SentinelResource 参数说明：
     * - value: 资源名称（在控制台配置规则时使用）
     * - blockHandler: 限流/熔断时的处理方法
     * - fallback: 发生异常时的处理方法
     */
    @PostMapping("/order")
    @SentinelResource(
        value = "seckillOrder",  // 资源名称
        blockHandler = "handleSeckillBlock",  // 限流处理方法
        fallback = "handleSeckillFallback"  // 异常处理方法
    )
    public Result<String> seckillOrder(
        @RequestParam Long productId,
        @RequestParam Long userId
    ) {
        // 秒杀业务逻辑
        // 1. 检查库存
        // 2. 扣减库存
        // 3. 创建订单
        // 4. 发送消息队列
        
        return Result.success("秒杀成功，订单创建中...");
    }

    /**
     * 限流处理方法
     * 
     * 方法签名要求：
     * 1. 返回值类型必须与原方法一致
     * 2. 参数列表必须与原方法一致
     * 3. 最后必须增加 BlockException 参数
     * 4. 必须是 public 修饰
     * 5. 必须与原方法在同一个类中
     */
    public Result<String> handleSeckillBlock(
        Long productId,
        Long userId,
        BlockException ex
    ) {
        return Result.fail("当前排队人数过多，请稍后再试");
    }

    /**
     * 异常处理方法
     * 
     * 方法签名要求：
     * 1. 返回值类型必须与原方法一致
     * 2. 参数列表必须与原方法一致
     * 3. 最后必须增加 Throwable 参数
     * 4. 必须是 public 修饰
     * 5. 必须与原方法在同一个类中
     */
    public Result<String> handleSeckillFallback(
        Long productId,
        Long userId,
        Throwable ex
    ) {
        return Result.fail("系统繁忙，请稍后再试");
    }
}
```

#### 示例 2：订单创建接口限流

在 `mall-order-service` 中：

```java
package com.mall.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @PostMapping("/create")
    @SentinelResource(
        value = "createOrder",
        blockHandler = "handleCreateOrderBlock"
    )
    public Result<Long> createOrder(@RequestBody OrderDTO orderDTO) {
        // 创建订单逻辑
        Long orderId = orderService.createOrder(orderDTO);
        return Result.success(orderId);
    }

    public Result<Long> handleCreateOrderBlock(OrderDTO orderDTO, BlockException ex) {
        return Result.fail("订单创建失败，系统繁忙，请稍后再试");
    }
}
```

#### 示例 3：商品查询接口限流

在 `mall-product-service` 中：

```java
package com.mall.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {

    @GetMapping("/{id}")
    @SentinelResource(
        value = "getProductDetail",
        blockHandler = "handleGetProductBlock"
    )
    public Result<Product> getProductDetail(@PathVariable Long id) {
        Product product = productService.getById(id);
        return Result.success(product);
    }

    public Result<Product> handleGetProductBlock(Long id, BlockException ex) {
        // 返回默认商品信息
        Product defaultProduct = new Product();
        defaultProduct.setId(id);
        defaultProduct.setName("商品信息加载中，请稍后再试");
        return Result.success(defaultProduct);
    }
}
```

### 3.2 方式二：使用 SphU.entry() 代码方式

```java
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;

public void someMethod() {
    try (Entry entry = SphU.entry("resourceName")) {
        // 被保护的业务逻辑
        // 如果没有触发限流，正常执行
    } catch (BlockException e) {
        // 处理被限流的情况
        // 返回友好提示或执行降级逻辑
    }
}
```

**✅ 第三步完成标志**：代码中已添加 @SentinelResource 注解，项目编译通过

---

## 第四步：在控制台配置限流规则

### 4.1 访问服务列表

1. 登录 Sentinel 控制台
2. 在左侧菜单中点击您的服务名称（如 `mall-seckill-service`）
3. 点击"簇点链路"菜单，查看已定义的资源

**注意**：如果看不到资源，请先访问一次对应的接口。

### 4.2 配置流控规则

#### 方式一：通过控制台界面配置（适合测试）

1. 点击"流控规则"菜单
2. 点击"新增流控规则"按钮
3. 填写规则配置：

**基础配置**：
- **资源名**：`seckillOrder`（对应 @SentinelResource 的 value 值）
- **针对来源**：`default`（表示所有来源）
- **阈值类型**：
  - `QPS`：每秒请求数（推荐）
  - `并发线程数`：同时执行的线程数
- **单机阈值**：`10`（每秒最多 10 个请求）

**高级选项**：
- **流控模式**：
  - `直接`：直接限流（默认）
  - `关联`：当关联资源达到阈值时限流
  - `链路`：只记录从入口资源进来的流量
- **流控效果**：
  - `快速失败`：直接拒绝请求（推荐）
  - `Warm Up`：预热模式，缓慢增加阈值
  - `排队等待`：请求排队，匀速通过

4. 点击"新增"按钮保存

#### 方式二：通过 Nacos 配置（推荐生产环境）

1. 登录 Nacos 控制台：`http://localhost:8848/nacos`
2. 创建命名空间：`SENTINEL_GROUP`
3. 创建配置文件：
   - **Data ID**：`mall-seckill-service-flow-rules`
   - **Group**：`SENTINEL_GROUP`
   - **配置格式**：`JSON`
   - **配置内容**：

```json
[
  {
    "resource": "seckillOrder",
    "limitApp": "default",
    "grade": 1,
    "count": 10,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

**参数说明**：
- `resource`：资源名称
- `limitApp`：来源应用，default 表示所有应用
- `grade`：阈值类型，1=QPS，0=线程数
- `count`：阈值
- `strategy`：流控模式，0=直接，1=关联，2=链路
- `controlBehavior`：流控效果，0=快速失败，1=Warm Up，2=排队等待
- `clusterMode`：是否集群模式

**✅ 第四步完成标志**：控制台中已配置限流规则

---

## 第五步：测试限流效果

### 5.1 准备测试工具

使用以下任一工具进行测试：
- Postman
- JMeter
- Apache Bench (ab)
- 浏览器插件

### 5.2 测试步骤

#### 步骤 1：正常访问接口

使用 Postman 或浏览器访问：
```
POST http://localhost:8087/seckill/order?productId=1&userId=1
```

正常情况下，返回：
```json
{
  "code": 200,
  "message": "success",
  "data": "秒杀成功，订单创建中..."
}
```

#### 步骤 2：触发限流

使用 JMeter 或 ab 工具进行并发测试：

**使用 Apache Bench**：
```bash
# 发送 100 个请求，并发 20
ab -n 100 -c 20 "http://localhost:8087/seckill/order?productId=1&userId=1"
```

**使用 JMeter**：
1. 创建线程组，设置线程数为 50
2. 添加 HTTP 请求
3. 运行测试

#### 步骤 3：观察限流效果

当请求超过阈值（10 QPS）时，部分请求会返回：
```json
{
  "code": 500,
  "message": "当前排队人数过多，请稍后再试",
  "data": null
}
```

#### 步骤 4：查看监控数据

在 Sentinel 控制台中：
1. 点击"实时监控"菜单
2. 查看通过 QPS 和拒绝 QPS
3. 观察限流效果

**✅ 第五步完成标志**：能够触发限流，并看到限流提示

---

## 第六步：高级配置

### 6.1 关联限流

**场景**：当查询订单接口 QPS 过高时，限制创建订单接口。

**配置**：
- **资源名**：`createOrder`
- **流控模式**：`关联`
- **关联资源**：`queryOrder`
- **阈值**：`100`

当 `queryOrder` 的 QPS 超过 100 时，`createOrder` 会被限流。

### 6.2 链路限流

**场景**：只限制从某个入口进来的流量。

**配置**：
- **资源名**：`getProductDetail`
- **流控模式**：`链路`
- **入口资源**：`/product/list`
- **阈值**：`50`

只有从 `/product/list` 进入的请求才会被限流。

### 6.3 Warm Up（预热）

**场景**：秒杀开始时，避免瞬间流量冲垮系统。

**配置**：
- **资源名**：`seckillOrder`
- **流控效果**：`Warm Up`
- **阈值**：`100`
- **预热时长**：`10`（秒）

系统会在 10 秒内缓慢增加阈值，从 3（100/30）逐渐增加到 100。

### 6.4 排队等待

**场景**：削峰填谷，让请求匀速通过。

**配置**：
- **资源名**：`createOrder`
- **流控效果**：`排队等待`
- **阈值**：`50`
- **超时时间**：`2000`（毫秒）

请求会排队等待，最多等待 2 秒，超过则拒绝。

**✅ 第六步完成标志**：掌握高级限流配置

---

## 第七步：热点参数限流

### 7.1 添加热点参数限流依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.alibaba.csp.sentinel</groupId>
    <artifactId>sentinel-parameter-flow-control</artifactId>
    <version>1.8.8</version>
</dependency>
```

### 7.2 使用 @SentinelResource

```java
@GetMapping("/product/{productId}")
@SentinelResource(value = "hot-product", blockHandler = "handleHotProductBlock")
public Result<Product> getSeckillProduct(@PathVariable Long productId) {
    Product product = productService.getById(productId);
    return Result.success(product);
}

public Result<Product> handleHotProductBlock(Long productId, BlockException ex) {
    return Result.fail("商品访问人数过多，请稍后再试");
}
```

### 7.3 在控制台配置热点规则

1. 点击"热点规则"菜单
2. 点击"新增热点规则"
3. 配置参数：
   - **资源名**：`hot-product`
   - **参数索引**：`0`（第一个参数 productId）
   - **参数类型**：`long`
   - **单机阈值**：`10`
   - **参数例外项**：
     - 参数值：`1001`（热门商品 ID）
     - 阈值：`5`（更严格的限流）

**✅ 第七步完成标志**：热点参数限流配置成功

---

## 第八步：熔断降级

### 8.1 慢调用比例熔断

**场景**：当接口响应时间过长时，自动熔断。

**配置**：
- **资源名**：`queryOrder`
- **熔断策略**：`慢调用比例`
- **最大 RT**：`500`（毫秒）
- **比例阈值**：`0.5`（50%）
- **熔断时长**：`10`（秒）
- **最小请求数**：`5`

当 50% 的请求响应时间超过 500ms 时，触发熔断 10 秒。

### 8.2 异常比例熔断

**场景**：当接口异常比例过高时，自动熔断。

**配置**：
- **资源名**：`createOrder`
- **熔断策略**：`异常比例`
- **比例阈值**：`0.3`（30%）
- **熔断时长**：`20`（秒）
- **最小请求数**：`10`

当 30% 的请求出现异常时，触发熔断 20 秒。

### 8.3 异常数熔断

**场景**：当接口异常数量过多时，自动熔断。

**配置**：
- **资源名**：`paymentCallback`
- **熔断策略**：`异常数`
- **异常数**：`5`
- **熔断时长**：`30`（秒）
- **最小请求数**：`10`

当出现 5 个异常时，触发熔断 30 秒。

**✅ 第八步完成标志**：熔断降级配置成功

---

## 第九步：系统自适应保护

### 9.1 配置系统规则

1. 点击"系统规则"菜单
2. 点击"新增系统规则"
3. 配置参数：

**CPU 使用率保护**：
- **阈值类型**：`CPU 使用率`
- **阈值**：`0.8`（80%）

**平均响应时间保护**：
- **阈值类型**：`平均响应时间`
- **阈值**：`1000`（毫秒）

**并发线程数保护**：
- **阈值类型**：`并发线程数`
- **阈值**：`500`

**入口 QPS 保护**：
- **阈值类型**：`入口 QPS`
- **阈值**：`1000`

**✅ 第九步完成标志**：系统保护配置成功

---

## 第十步：实战案例

### 案例 1：秒杀场景完整配置

#### Controller 代码

```java
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @PostMapping("/order")
    @SentinelResource(
        value = "seckillOrder",
        blockHandler = "handleSeckillBlock",
        fallback = "handleSeckillFallback"
    )
    public Result<String> seckillOrder(
        @RequestParam Long productId,
        @RequestParam Long userId
    ) {
        // 1. 检查库存
        Integer stock = productService.getStock(productId);
        if (stock <= 0) {
            return Result.fail("商品已售罄");
        }
        
        // 2. 扣减库存（使用 Redis 原子操作）
        boolean success = productService.deductStock(productId);
        if (!success) {
            return Result.fail("商品已售罄");
        }
        
        // 3. 创建订单（异步）
        orderService.createOrderAsync(productId, userId);
        
        return Result.success("秒杀成功，订单创建中...");
    }

    public Result<String> handleSeckillBlock(Long productId, Long userId, BlockException ex) {
        return Result.fail("当前排队人数过多，请稍后再试");
    }

    public Result<String> handleSeckillFallback(Long productId, Long userId, Throwable ex) {
        log.error("秒杀异常", ex);
        return Result.fail("系统繁忙，请稍后再试");
    }
}
```

#### 限流规则配置

```json
[
  {
    "resource": "seckillOrder",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 2,
    "clusterMode": false
  }
]
```

#### 热点参数限流配置

```json
[
  {
    "resource": "seckillOrder",
    "count": 50,
    "grade": 1,
    "paramIdx": 0,
    "paramFlowItemList": [
      {
        "object": "1001",
        "count": 10,
        "classType": "long"
      }
    ]
  }
]
```

### 案例 2：订单创建完整配置

#### Controller 代码

```java
@RestController
@RequestMapping("/order")
public class OrderController {

    @PostMapping("/create")
    @SentinelResource(
        value = "createOrder",
        blockHandler = "handleCreateOrderBlock"
    )
    public Result<Long> createOrder(@RequestBody OrderDTO orderDTO) {
        Long orderId = orderService.createOrder(orderDTO);
        return Result.success(orderId);
    }

    public Result<Long> handleCreateOrderBlock(OrderDTO orderDTO, BlockException ex) {
        return Result.fail("订单创建失败，系统繁忙，请稍后再试");
    }
}
```

#### 限流规则配置

```json
[
  {
    "resource": "createOrder",
    "limitApp": "default",
    "grade": 1,
    "count": 50,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

#### 熔断规则配置

```json
[
  {
    "resource": "createOrder",
    "grade": 0,
    "count": 1000,
    "timeWindow": 20,
    "minRequestAmount": 10,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.5
  }
]
```

**✅ 第十步完成标志**：能够独立完成实际场景的限流配置

---

## 总结

### ✅ 完成标志检查清单

- [ ] 第一步：成功启动 Sentinel Dashboard 控制台
- [ ] 第二步：微服务成功注册到 Sentinel
- [ ] 第三步：代码中添加 @SentinelResource 注解
- [ ] 第四步：控制台配置限流规则
- [ ] 第五步：测试限流效果成功
- [ ] 第六步：掌握高级配置
- [ ] 第七步：热点参数限流配置成功
- [ ] 第八步：熔断降级配置成功
- [ ] 第九步：系统保护配置成功
- [ ] 第十步：能够独立完成实战案例

### 📚 推荐学习路径

1. **入门**：完成前五步，掌握基本限流
2. **进阶**：完成第六、七步，掌握高级功能
3. **实战**：完成第八、九、十步，掌握生产级配置

### 🎯 最佳实践建议

1. **测试环境**：先在控制台配置规则，测试效果
2. **生产环境**：使用 Nacos 持久化规则，避免重启丢失
3. **监控告警**：集成 Prometheus + Grafana 进行监控
4. **压测验证**：上线前进行压力测试，验证限流效果
5. **逐步调整**：根据实际业务情况，逐步调整阈值

---

**恭喜您完成 Sentinel 限流学习！** 🎉