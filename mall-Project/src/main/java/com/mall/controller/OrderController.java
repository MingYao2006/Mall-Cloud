package com.mall.controller;

import com.mall.common.JwtUtil;
import com.mall.common.Result;
import com.mall.entity.Order;
import com.mall.entity.dto.CreateOrderRequest;
import com.mall.entity.vo.OrderDetailVO;
import com.mall.entity.vo.OrderVO;
import com.mall.service.OrderService;
import com.mall.service.UserService;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单Controller
 * 提供订单的增删改查、订单取消、订单支付、订单发货、订单完成等接口
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    /**
     * 创建订单
     * 从购物车中选中的商品创建订单
     */
    @PostMapping
    public Result<Order> createOrder(@RequestBody @Valid CreateOrderRequest request, HttpServletRequest httpRequest) {
        // 直接从拦截器已解析的属性中获取
        Long userId = (Long) httpRequest.getAttribute("userId");
        request.setUserId(userId);
        Order order = orderService.createOrder(request);
        return Result.success(order);
    }

    /**
     * 根据订单ID查询订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderByOrderId(@PathVariable Long orderId) {
        OrderDetailVO orderByOrderId = orderService.getOrderByOrderId(orderId);
        return Result.success(orderByOrderId);
    }

    /**
     * 获取当前用户的订单列表
     */
    @GetMapping
    public Result<List<OrderVO>> getMyOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<OrderVO> orders = orderService.getOrdersByUserId(userId);
        return Result.success(orders);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{orderId}/cancel")
    public Result<String> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return Result.success("订单取消成功");
    }

    /**
     * 支付订单
     */
    @PutMapping("/{orderId}/pay")
    public Result<String> payOrder(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
        return Result.success("订单支付成功");
    }

}