package com.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单日销售趋势数据点
 */
@Data
public class SalesTrendItem {

    /** 日期，格式 yyyy-MM-dd */
    private String statDate;

    /** 当日有效订单数 */
    private Long orderCount;

    /** 当日销售额 */
    private BigDecimal salesAmount;

    public SalesTrendItem() {
    }

    public SalesTrendItem(String statDate, Long orderCount, BigDecimal salesAmount) {
        this.statDate = statDate;
        this.orderCount = orderCount;
        this.salesAmount = salesAmount;
    }
}
