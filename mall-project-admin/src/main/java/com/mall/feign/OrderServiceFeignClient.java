package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.Order;
import com.mall.entity.vo.OrderDetailVO;
import com.mall.entity.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-order-service 微服务，
 * 用于订单查看、发货等管理操作。
 */
@FeignClient(
        name = "mall-order-service",
        contextId = "orderServiceFeignClient",
        path = "/orders"
)
public interface OrderServiceFeignClient {

    @GetMapping("/{orderId}")
    Result<OrderDetailVO> getOrderByOrderId(@PathVariable("orderId") Long orderId);

    @GetMapping
    Result<List<OrderVO>> getOrdersByUserId(@RequestParam("userId") Long userId);

    @PutMapping("/{orderId}/cancel")
    Result<String> cancelOrder(@PathVariable("orderId") Long orderId);

    @GetMapping("/entity/{orderId}")
    Result<Order> getOrderEntity(@PathVariable("orderId") Long orderId);

    /**
     * 获取全部订单列表（管理端用，通过 userId=0 或不传参表示查全部）
     * 复用现有接口，管理端可按需传 userId 查指定用户订单
     */
    @GetMapping
    Result<List<OrderVO>> getAllOrders();
}
