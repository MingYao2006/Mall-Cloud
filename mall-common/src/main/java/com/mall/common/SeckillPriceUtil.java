package com.mall.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 秒杀价权威计算工具（聚合服务展示 与 订单服务建单 共用同一份规则）。
 *
 * <p>历史上秒杀页用 Math.random() 在前端现编一个折扣价，下单时又没传给后端，
 * 订单服务只能按商品原价建单，导致"页面秒杀价 ≠ 订单商品价 ≠ 实付金额"。
 * 这里把定价收敛到后端：按商品 ID 做稳定哈希得出 [0.50, 0.80) 的折扣，
 * 同一商品任何时候算出的秒杀价都一致，且前端无法通过传参篡改价格。
 */
public final class SeckillPriceUtil {

    /** 折扣区间：最低 5 折，最高 8 折（不含） */
    private static final BigDecimal MIN_RATE = new BigDecimal("0.50");
    private static final BigDecimal RATE_SPAN = new BigDecimal("0.30");
    private static final int HASH_BUCKETS = 10000;

    private SeckillPriceUtil() {
    }

    /**
     * 按商品 ID 计算秒杀单价（保留 2 位小数，四舍五入）。
     *
     * @param productId 商品 ID
     * @param originPrice 商品原价
     * @return 秒杀价；originPrice 为空时返回 null
     */
    public static BigDecimal compute(Long productId, BigDecimal originPrice) {
        if (originPrice == null) {
            return null;
        }
        // 稳定哈希：同一 productId 永远得到同一个桶，避免刷新页面价格乱跳
        long id = productId == null ? 0L : productId;
        int bucket = (int) Math.floorMod(id * 31L + 7L, HASH_BUCKETS);
        BigDecimal rate = MIN_RATE.add(
                RATE_SPAN.multiply(BigDecimal.valueOf(bucket))
                        .divide(BigDecimal.valueOf(HASH_BUCKETS), 4, RoundingMode.HALF_UP));
        return originPrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
