package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.Order;
import com.mall.entity.dto.CreateOrderRequest;
import com.mall.entity.vo.OrderDetailVO;
import com.mall.entity.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务 Feign 客户端
 */
@FeignClient(
        name = "mall-order-service",
        contextId = "orderServiceFeignClient",
        path = "/orders"
)
public interface OrderServiceFeignClient {

    /**
     * 创建订单
     * POST /api/orders
     */
    @PostMapping
    Result<Order> createOrder(@RequestBody CreateOrderRequest request);

    /**
     * 根据订单ID查询订单详情
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    Result<OrderDetailVO> getOrderByOrderId(@PathVariable("orderId") Long orderId);

    /**
     * 获取用户订单列表
     * GET /api/orders?userId=1
     */
    @GetMapping
    Result<List<OrderVO>> getOrdersByUserId(@RequestParam("userId") Long userId);

    /**
     * 取消订单
     * PUT /api/orders/{orderId}/cancel
     */
    @PutMapping("/{orderId}/cancel")
    Result<String> cancelOrder(@PathVariable("orderId") Long orderId);

    /**
     * 支付订单
     * PUT /api/orders/{orderId}/pay
     */
    @PutMapping("/{orderId}/pay")
    Result<String> payOrder(@PathVariable("orderId") Long orderId);

    /**
     * 根据订单ID查询订单实体（发起支付前查询订单信息）
     * GET /api/orders/entity/{orderId}
     */
    @GetMapping("/entity/{orderId}")
    Result<Order> getOrderEntity(@PathVariable("orderId") Long orderId);

    /**
     * 根据订单号支付订单（支付宝异步回调用，幂等）
     * PUT /api/orders/payByOrderNo/{orderNo}
     */
    @PutMapping("/payByOrderNo/{orderNo}")
    Result<String> payOrderByOrderNo(@PathVariable("orderNo") String orderNo);
}
