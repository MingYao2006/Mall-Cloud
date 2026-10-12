package com.mall.admin.controller;

import com.mall.common.Result;
import com.mall.common.vo.StatsOverviewVO;
import com.mall.feign.OrderServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台数据统计控制器（B端）
 * <p>
 * 管理端首页的指标卡片、销售趋势图、商品热度榜统一走此接口，
 * 数据由 mall-order-service 聚合后经 Feign 返回。
 */
@Tag(name = "数据统计", description = "控制台数据概览接口（管理端）")
@RestController
@RequestMapping("/stats")
public class AdminStatsController {

    private final OrderServiceFeignClient orderServiceFeignClient;

    public AdminStatsController(OrderServiceFeignClient orderServiceFeignClient) {
        this.orderServiceFeignClient = orderServiceFeignClient;
    }

    @Operation(summary = "控制台数据概览")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> getOverview(@RequestParam(value = "days", defaultValue = "7") Integer days) {
        return orderServiceFeignClient.getStatsOverview(days);
    }
}