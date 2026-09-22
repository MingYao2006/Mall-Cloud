package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Order;
import com.mall.entity.dto.CreateOrderRequest;
import com.mall.entity.vo.OrderDetailVO;
import com.mall.entity.vo.OrderVO;
import com.mall.feign.OrderServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单聚合入口
 * <p>
 * 整合架构：本控制器不再实现任何订单业务逻辑，
 * 全部通过 Feign 代理转发到 mall-order-service 微服务。
 */
@Tag(name = "订单服务", description = "订单相关接口（聚合服务代理）")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderServiceFeignClient orderServiceFeignClient;

    public OrderController(OrderServiceFeignClient orderServiceFeignClient) {
        this.orderServiceFeignClient = orderServiceFeignClient;
    }

    /**
     * 创建订单（转发到订单微服务）
     * 从购物车中选中的商品创建订单
     */
    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Order> createOrder(@RequestBody @Valid CreateOrderRequest request, HttpServletRequest httpRequest) {
        // 直接从拦截器已解析的属性中获取
        Long userId = (Long) httpRequest.getAttribute("userId");
        request.setUserId(userId);
        return orderServiceFeignClient.createOrder(request);
    }

    /**
     * 根据订单ID查询订单详情（转发到订单微服务）
     */
    @Operation(summary = "根据订单ID查询订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderByOrderId(@PathVariable Long orderId) {
        return orderServiceFeignClient.getOrderByOrderId(orderId);
    }

    /**
     * 获取当前用户的订单列表（转发到订单微服务）
     */
    @Operation(summary = "获取当前用户的订单列表")
    @GetMapping
    public Result<List<OrderVO>> getMyOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return orderServiceFeignClient.getOrdersByUserId(userId);
    }

    /**
     * 取消订单（转发到订单微服务）
     */
    @Operation(summary = "取消订单")
    @PutMapping("/{orderId}/cancel")
    public Result<String> cancelOrder(@PathVariable Long orderId) {
        return orderServiceFeignClient.cancelOrder(orderId);
    }

    /**
     * 支付订单（转发到订单微服务）
     */
    @Operation(summary = "支付订单")
    @PutMapping("/{orderId}/pay")
    public Result<String> payOrder(@PathVariable Long orderId) {
        return orderServiceFeignClient.payOrder(orderId);
    }

}