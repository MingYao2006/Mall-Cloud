# Alibaba Sentinel 集成使用指南

## 一、Sentinel 控制台安装与启动

### 1. 下载 Sentinel Dashboard

访问 Sentinel 官方 GitHub 下载页面：
```
https://github.com/alibaba/Sentinel/releases
```

下载 `sentinel-dashboard-x.x.x.jar` 文件（推荐版本：1.8.8）

### 2. 启动 Sentinel Dashboard

在命令行中执行以下命令启动控制台：

```bash
# 基础启动（默认端口 8080）
java -jar sentinel-dashboard-1.8.8.jar

# 指定端口启动
java -jar sentinel-dashboard-1.8.8.jar --server.port=8080

# 后台启动（Linux）
nohup java -jar sentinel-dashboard-1.8.8.jar > sentinel.log 2>&1 &

# Windows 后台启动
start /b java -jar sentinel-dashboard-1.8.8.jar
```

### 3. 访问控制台

启动成功后，访问：
```
http://localhost:8080
```

默认账号密码：
- 用户名：`sentinel`
- 密码：`sentinel`

---

## 二、项目配置说明

### 1. 已集成的服务

已在以下关键服务中集成 Sentinel：

| 服务名称 | 端口 | Sentinel 端口 | 说明 |
|---------|------|--------------|------|
| mall-seckill-service | 8087 | 8719 | 秒杀服务（最重要） |
| mall-order-service | 8083 | 8720 | 订单服务 |
| mall-product-service | 8082 | 8721 | 商品服务 |
| mall-payment-service | 8088 | 8722 | 支付服务 |

### 2. 配置项说明

每个服务的 `application.yml` 中已添加以下配置：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080  # Sentinel 控制台地址
        port: 8719                 # 与控制台通信的端口（每个服务需不同）
      eager: true                  # 应用启动时立即初始化 Sentinel
      datasource:
        flow:                      # 流控规则数据源
          nacos:
            server-addr: localhost:8848
            username: nacos
            password: nacos
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: flow
```

---

## 三、启动顺序

### 1. 正确的启动顺序

```
1. 启动 Nacos（端口 8848）
   ↓
2. 启动 Sentinel Dashboard（端口 8080）
   ↓
3. 启动 Redis（端口 6379）
   ↓
4. 启动 MySQL（端口 3306）
   ↓
5. 启动 RabbitMQ（端口 5672）
   ↓
6. 启动各个微服务
   - mall-product-service (8082)
   - mall-order-service (8083)
   - mall-seckill-service (8087)
   - mall-payment-service (8088)
```

### 2. 验证集成成功

启动服务后，访问 Sentinel 控制台，在左侧菜单中应该能看到已注册的服务列表。

---

## 四、使用 Sentinel 进行流量控制

### 1. 在代码中使用 @SentinelResource 注解

#### 示例 1：秒杀接口限流

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
     * value: 资源名称
     * blockHandler: 限流/降级处理方法
     * fallback: 异常处理方法
     */
    @PostMapping("/order")
    @SentinelResource(
        value = "seckillOrder",
        blockHandler = "handleSeckillBlock",
        fallback = "handleSeckillFallback"
    )
    public Result<String> seckillOrder(@RequestParam Long productId, @RequestParam Long userId) {
        // 秒杀业务逻辑
        return Result.success("秒杀成功");
    }

    /**
     * 限流/熔断处理方法
     * 方法签名必须与原方法一致，最后增加 BlockException 参数
     */
    public Result<String> handleSeckillBlock(Long productId, Long userId, BlockException ex) {
        return Result.fail("当前排队人数过多，请稍后再试");
    }

    /**
     * 异常处理方法
     * 方法签名必须与原方法一致，最后增加 Throwable 参数
     */
    public Result<String> handleSeckillFallback(Long productId, Long userId, Throwable ex) {
        return Result.fail("系统繁忙，请稍后再试");
    }
}
```

#### 示例 2：订单创建接口限流

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
        return Result.fail("订单创建失败，系统繁忙");
    }
}
```

#### 示例 3：商品查询接口限流

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
        return Result.fail("商品查询失败，请稍后再试");
    }
}
```

### 2. 在控制台配置限流规则

#### 方式一：通过控制台界面配置

1. 访问 Sentinel 控制台：http://localhost:8080
2. 点击左侧菜单中的服务名称
3. 点击"流控规则" → "新增流控规则"
4. 配置规则参数：
   - **资源名**：对应 @SentinelResource 的 value 值
   - **阈值类型**：QPS 或并发线程数
   - **单机阈值**：每秒最多通过的请求数
   - **流控模式**：直接、关联、链路
   - **流控效果**：快速失败、Warm Up、排队等待

#### 方式二：通过 Nacos 配置（推荐生产环境）

1. 登录 Nacos 控制台：http://localhost:8848/nacos
2. 创建命名空间：`SENTINEL_GROUP`
3. 创建配置文件：
   - **Data ID**：`mall-seckill-service-flow-rules`
   - **Group**：`SENTINEL_GROUP`
   - **配置格式**：JSON
   - **配置内容**：

```json
[
  {
    "resource": "seckillOrder",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

**参数说明**：
- `resource`：资源名称
- `limitApp`：来源应用，default 表示所有应用
- `grade`：阈值类型，1 表示 QPS，0 表示线程数
- `count`：阈值
- `strategy`：流控模式，0 表示直接，1 表示关联，2 表示链路
- `controlBehavior`：流控效果，0 表示快速失败，1 表示 Warm Up，2 表示排队等待
- `clusterMode`：是否集群模式

---

## 五、熔断降级配置

### 1. 代码示例

```java
package com.mall.order.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    /**
     * 查询订单详情
     * 熔断策略：慢调用比例
     */
    @SentinelResource(
        value = "getOrderDetail",
        blockHandler = "handleGetOrderBlock",
        fallback = "handleGetOrderFallback"
    )
    public Order getOrderDetail(Long orderId) {
        // 查询订单详情
        return orderMapper.selectById(orderId);
    }

    public Order handleGetOrderBlock(Long orderId, BlockException ex) {
        // 熔断后的降级处理
        return new Order(); // 返回默认订单
    }

    public Order handleGetOrderFallback(Long orderId, Throwable ex) {
        // 异常后的降级处理
        return new Order();
    }
}
```

### 2. 在控制台配置熔断规则

1. 访问 Sentinel 控制台
2. 点击"熔断规则" → "新增熔断规则"
3. 配置参数：
   - **资源名**：对应 @SentinelResource 的 value 值
   - **熔断策略**：
     - 慢调用比例：响应时间超过阈值的比例
     - 异常比例：异常比例达到阈值
     - 异常数：异常数量达到阈值
   - **比例阈值**：触发熔断的比例
   - **熔断时长**：熔断持续时间（秒）
   - **最小请求数**：触发熔断的最小请求数

---

## 六、OpenFeign 整合 Sentinel

### 1. 配置文件启用

在 `application.yml` 中添加：

```yaml
feign:
  sentinel:
    enabled: true  # 启用 Feign 对 Sentinel 的支持
```

### 2. Feign 客户端示例

```java
package com.mall.order.feign;

import com.mall.order.fallback.ProductFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    value = "mall-product-service",
    fallback = ProductFeignFallback.class  // 指定降级处理类
)
public interface ProductFeignClient {

    @GetMapping("/product/{id}")
    Result<Product> getProductById(@PathVariable Long id);
}
```

### 3. 降级处理类

```java
package com.mall.order.fallback;

import com.mall.order.feign.ProductFeignClient;
import org.springframework.stereotype.Component;

@Component
public class ProductFeignFallback implements ProductFeignClient {

    @Override
    public Result<Product> getProductById(Long id) {
        // 服务降级：返回默认商品信息
        Product defaultProduct = new Product();
        defaultProduct.setId(id);
        defaultProduct.setName("商品信息加载失败");
        return Result.success(defaultProduct);
    }
}
```

---

## 七、热点参数限流

### 1. 适用场景

针对请求中的热点参数进行细粒度限流，例如：
- 秒杀商品 ID
- 用户 ID
- 商品分类 ID

### 2. 代码示例

```java
package com.mall.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    /**
     * 热点参数限流
     * 针对商品 ID 进行限流
     */
    @GetMapping("/product/{productId}")
    @SentinelResource(
        value = "hot-product",
        blockHandler = "handleHotProductBlock"
    )
    public Result<Product> getSeckillProduct(@PathVariable Long productId) {
        Product product = productService.getById(productId);
        return Result.success(product);
    }

    public Result<Product> handleHotProductBlock(Long productId, BlockException ex) {
        return Result.fail("商品访问人数过多，请稍后再试");
    }
}
```

### 3. 在控制台配置热点规则

1. 访问 Sentinel 控制台
2. 点击"热点规则" → "新增热点规则"
3. 配置参数：
   - **资源名**：hot-product
   - **参数索引**：0（第一个参数）
   - **参数类型**：long
   - **单机阈值**：10
   - **参数例外项**：针对特定参数值设置不同的阈值

---

## 八、系统自适应保护

### 1. 适用场景

当系统负载过高时，自动进行流量控制，保护系统不被压垮。

### 2. 在控制台配置系统规则

1. 访问 Sentinel 控制台
2. 点击"系统规则" → "新增系统规则"
3. 配置参数：
   - **阈值类型**：
     - CPU 使用率：当 CPU 使用率超过阈值时触发
     - 平均响应时间：当平均响应时间超过阈值时触发
     - 并发线程数：当并发线程数超过阈值时触发
     - 入口 QPS：当入口 QPS 超过阈值时触发
   - **阈值**：触发保护的阈值

### 3. 推荐配置

```yaml
# 系统保护规则（在控制台配置）
- CPU 使用率阈值：0.8（80%）
- 平均响应时间阈值：1000ms
- 并发线程数阈值：500
- 入口 QPS 阈值：1000
```

---

## 九、监控与告警

### 1. 实时监控

在 Sentinel 控制台中，可以实时查看：
- 通过 QPS
- 拒绝 QPS
- 异常数
- 平均响应时间
- 并发线程数

### 2. 集群限流（生产环境推荐）

对于分布式环境，建议配置集群限流：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        flow:
          nacos:
            server-addr: localhost:8848
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: flow
```

---

## 十、最佳实践建议

### 1. 秒杀服务（mall-seckill-service）

**推荐配置**：
- 秒杀接口 QPS 限流：100-500（根据系统承载能力）
- 热点参数限流：针对热门商品 ID 进行细粒度限流
- 系统保护：CPU 使用率 > 80% 时触发保护

### 2. 订单服务（mall-order-service）

**推荐配置**：
- 订单创建接口 QPS 限流：50-100
- 熔断降级：依赖服务故障时快速失败
- 并发线程数限流：防止线程池耗尽

### 3. 商品服务（mall-product-service）

**推荐配置**：
- 商品查询接口 QPS 限流：200-500
- 关联限流：下单时限制商品查询频率
- 缓存预热：使用 Warm Up 模式

### 4. 支付服务（mall-payment-service）

**推荐配置**：
- 支付回调接口 QPS 限流：30-50
- 熔断降级：支付渠道故障时降级处理
- 异常比例熔断：异常比例 > 50% 时熔断

---

## 十一、常见问题

### 1. 控制台看不到服务

**原因**：服务未正确注册到 Sentinel 控制台

**解决方案**：
- 检查 `spring.cloud.sentinel.transport.dashboard` 配置
- 确保服务已启动并访问过接口（Sentinel 是懒加载）
- 检查防火墙是否开放端口

### 2. 规则不生效

**原因**：资源名称不匹配或配置错误

**解决方案**：
- 确保 `@SentinelResource` 的 value 值与控制台配置的资源名一致
- 检查 Nacos 配置格式是否正确
- 查看日志是否有错误信息

### 3. 限流后没有返回自定义提示

**原因**：未正确配置 blockHandler

**解决方案**：
- 确保 blockHandler 方法签名正确
- blockHandler 方法必须与原方法在同一类中
- 参数列表必须一致，最后增加 BlockException 参数

---

## 十二、生产环境注意事项

1. **规则持久化**：使用 Nacos 进行规则持久化，避免重启后规则丢失
2. **监控告警**：集成 Prometheus + Grafana 进行监控告警
3. **集群限流**：对于高并发场景，使用集群限流模式
4. **容量规划**：根据系统承载能力合理设置阈值
5. **压测验证**：上线前进行压力测试，验证限流配置的有效性

---

## 十三、参考资源

- Sentinel 官方文档：https://sentinelguard.io/zh-cn/
- Spring Cloud Alibaba 文档：https://spring-cloud-alibaba-group.github.io/
- Sentinel GitHub：https://github.com/alibaba/Sentinel

---

**祝您使用愉快！如有问题，请参考官方文档或社区支持。**