package com.mall.admin.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.vo.OrderDetailVO;
import com.mall.entity.vo.OrderVO;
import com.mall.feign.OrderServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理控制器（B端）
 * <p>
 * 提供订单查看、取消等管理接口。
 * <p>
 * 注意：发货/退款等管理操作需在 mall-order-service 补充对应接口后在此添加。
 */
@Tag(name = "订单管理", description = "订单管理接口（管理端）")
@RestController
@RequestMapping("/orders")
public class AdminOrderController {

    private final OrderServiceFeignClient orderServiceFeignClient;

    public AdminOrderController(OrderServiceFeignClient orderServiceFeignClient) {
        this.orderServiceFeignClient = orderServiceFeignClient;
    }

    @Operation(summary = "根据订单ID查询订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderByOrderId(@PathVariable Long orderId) {
        return orderServiceFeignClient.getOrderByOrderId(orderId);
    }

    @Operation(summary = "查询订单列表（userId 为空则查全部）")
    @GetMapping
    public Result<PageResult> getOrders(@RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        if (userId != null) {
            List<OrderVO> list = orderServiceFeignClient.getOrdersByUserId(userId).getData();
            if (list == null) {
                list = new java.util.ArrayList<>();
            }
            return Result.success(new PageResult(pageNum, pageSize, (long) list.size(), list));
        }
        return orderServiceFeignClient.getAllOrders(status, pageNum, pageSize);
    }

    @Operation(summary = "分页查询全部订单（管理端）")
    @GetMapping("/all")
    public Result<PageResult> getAllOrders(@RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderServiceFeignClient.getAllOrders(status, pageNum, pageSize);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{orderId}/cancel")
    public Result<String> cancelOrder(@PathVariable Long orderId) {
        return orderServiceFeignClient.cancelOrder(orderId);
    }
}
