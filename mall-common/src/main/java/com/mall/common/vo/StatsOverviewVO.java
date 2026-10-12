package com.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端控制台数据概览
 */
@Data
public class StatsOverviewVO {

    /** 有效订单总数（已支付/已发货/已完成） */

    /** 订单总数（含全部状态，便于观察新下单） */
    private Long totalAllOrders;
    private Long totalOrders;

    /** 累计销售额 */
    private BigDecimal totalSales;

    /** 今日订单数 */
    private Long todayOrders;

    /** 今日销售额 */
    private BigDecimal todaySales;

    /** 待支付订单数 */
    private Long pendingOrders;

    /** 待发货订单数 */
    private Long unshippedOrders;

    /** 销售趋势（按天） */
    private List<SalesTrendItem> trend;

    /** 商品热度排行 */
    private List<HotProductItem> hotProducts;
}
