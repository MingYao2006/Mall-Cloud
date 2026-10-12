package com.mall.feign;

import com.mall.common.PageResult;
import com.mall.common.vo.StatsOverviewVO;
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
     * 分页查询全部订单（管理端）
     * 支持按订单状态筛选 + 分页
     */
    @GetMapping("/all")
    Result<PageResult> getAllOrders(@RequestParam(value = "status", required = false) Integer status,
                                    @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize);

    /**
     * 控制台数据概览：核心指标 + 销售趋势 + 商品热度排行
     */
    @GetMapping("/stats/overview")
    Result<StatsOverviewVO> getStatsOverview(@RequestParam(value = "days", defaultValue = "7") Integer days);
}
