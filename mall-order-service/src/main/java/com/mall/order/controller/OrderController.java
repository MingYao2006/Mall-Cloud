package com.mall.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mall.common.Result;
import com.mall.order.entity.Order;
import com.mall.order.entity.dto.CreateOrderRequest;
import com.mall.order.entity.vo.OrderDetailVO;
import com.mall.order.entity.vo.OrderVO;
import com.mall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单Controller
 * 提供订单的增删改查、订单取消、订单支付、订单发货、订单完成等接口
 */
@Tag(name = "订单服务", description = "订单相关接口")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单
     * 从购物车中选中的商品创建订单
     */
    @Operation(summary = "创建订单")
    @PostMapping
    @SentinelResource(
            value = "createOrder",
            blockHandler = "handleCreateOrderBlock"
    )
    public Result<Order> createOrder(@RequestBody @Valid CreateOrderRequest request, HttpServletRequest httpRequest) {
        // 网关直连场景：从拦截器解析的属性中获取；聚合服务 Feign 转发场景：信任请求体中的 userId
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId != null) {
            request.setUserId(userId);
        }
        Order order = orderService.createOrder(request);
        return Result.success(order);
    }

    /**
     * 根据订单ID查询订单详情
     */
    @Operation(summary = "根据订单ID查询订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderByOrderId(@PathVariable Long orderId) {
        OrderDetailVO orderByOrderId = orderService.getOrderByOrderId(orderId);
        return Result.success(orderByOrderId);
    }

    /**
     * 获取当前用户的订单列表
     * userId 支持两种来源：网关拦截器属性 或 Feign 请求参数
     */
    @Operation(summary = "获取当前用户的订单列表")
    @GetMapping
    public Result<List<OrderVO>> getMyOrders(@RequestParam(value = "userId", required = false) Long userId,
                                             HttpServletRequest request) {
        if (userId == null) {
            userId = (Long) request.getAttribute("userId");
        }
        List<OrderVO> orders = orderService.getOrdersByUserId(userId);
        return Result.success(orders);
    }

    /**
     * 取消订单
     */
    @Operation(summary = "取消订单")
    @PutMapping("/{orderId}/cancel")
    public Result<String> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return Result.success("订单取消成功");
    }

    /**
     * 支付订单
     */
    @Operation(summary = "支付订单")
    @PutMapping("/{orderId}/pay")
    public Result<String> payOrder(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
        return Result.success("订单支付成功");
    }

    /**
     * 根据订单ID查询订单实体
     * <p>
     * 供聚合服务的支付场景（发起支付宝支付前查询订单信息）通过 Feign 调用。
     */
    @Operation(summary = "根据订单ID查询订单实体")
    @GetMapping("/entity/{orderId}")
    public Result<Order> getOrderEntity(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId);
        return Result.success(order);
    }

    /**
     * 根据订单号支付订单（支付宝异步回调用，幂等）
     * <p>
     * 供聚合服务的支付回调通过 Feign 代理调用，
     * 订单状态更新职责统一归属订单微服务。
     */
    @Operation(summary = "根据订单号支付订单（支付宝异步回调）")
    @PutMapping("/payByOrderNo/{orderNo}")
    public Result<String> payOrderByOrderNo(@PathVariable String orderNo) {
        orderService.payOrderByOrderNo(orderNo);
        return Result.success("支付成功");
    }
    /**
     * 限流处理方法
     * */
    public Result<Long> handleCreateOrderBlock(CreateOrderRequest request, BlockException ex) {
        return Result.fail("订单创建失败，系统繁忙，请稍后再试");
    }
}