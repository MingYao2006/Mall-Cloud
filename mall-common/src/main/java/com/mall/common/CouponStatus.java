package com.mall.common;

import java.time.LocalDateTime;

/**
 * 优惠券状态语义的唯一来源。
 * <p>
 * coupon.status 只表达「是否上架」：
 * <ul>
 *   <li>{@link #OFF} 0 未上架（草稿 / 已下架）</li>
 *   <li>{@link #ON} 1 已上架</li>
 *   <li>{@link #STOPPED} 2 已停用（终态，不允许再上架）</li>
 * </ul>
 * 「未开始 / 进行中 / 已抢光 / 已过期」一律由 startTime、endTime、availableStock 派生，
 * 不再往 status 里塞时间语义（历史种子数据的 0/1/2 刚好与上面一致，无需改数据）。
 */
public final class CouponStatus {

    /** 未上架（草稿 / 已下架） */
    public static final int OFF = 0;
    /** 已上架 */
    public static final int ON = 1;
    /** 已停用（终态） */
    public static final int STOPPED = 2;

    /** 派生状态：未上架 */
    public static final int DISPLAY_OFF = 0;
    /** 派生状态：已上架但未到开始时间 */
    public static final int DISPLAY_NOT_START = 1;
    /** 派生状态：进行中，可以抢 */
    public static final int DISPLAY_ONGOING = 2;
    /** 派生状态：库存为 0 */
    public static final int DISPLAY_SOLD_OUT = 3;
    /** 派生状态：已过结束时间 */
    public static final int DISPLAY_EXPIRED = 4;
    /** 派生状态：已停用 */
    public static final int DISPLAY_STOPPED = 5;

    private CouponStatus() {
    }

    public static boolean isValid(Integer status) {
        return status != null && (status == OFF || status == ON || status == STOPPED);
    }

    /**
     * 派生展示状态。判定顺序固定为：未上架 / 已停用 -> 已过期 -> 未开始 -> 已抢光 -> 进行中，
     * 前端（管理端 Coupons.vue、C 端 Coupon.vue）必须保持一致，不要各写一套。
     */
    public static int derive(Integer status, LocalDateTime startTime, LocalDateTime endTime,
                             Integer availableStock) {
        if (status == null || status == OFF) {
            return DISPLAY_OFF;
        }
        if (status == STOPPED) {
            return DISPLAY_STOPPED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (endTime != null && now.isAfter(endTime)) {
            return DISPLAY_EXPIRED;
        }
        if (startTime != null && now.isBefore(startTime)) {
            return DISPLAY_NOT_START;
        }
        if (availableStock != null && availableStock <= 0) {
            return DISPLAY_SOLD_OUT;
        }
        return DISPLAY_ONGOING;
    }

    public static String text(int displayStatus) {
        return switch (displayStatus) {
            case DISPLAY_NOT_START -> "未开始";
            case DISPLAY_ONGOING -> "进行中";
            case DISPLAY_SOLD_OUT -> "已抢光";
            case DISPLAY_EXPIRED -> "已过期";
            case DISPLAY_STOPPED -> "已停用";
            default -> "未上架";
        };
    }

    /** 前端标签语气色：success / warning / danger / info */
    public static String tagType(int displayStatus) {
        return switch (displayStatus) {
            case DISPLAY_ONGOING -> "success";
            case DISPLAY_NOT_START -> "warning";
            case DISPLAY_SOLD_OUT, DISPLAY_EXPIRED, DISPLAY_STOPPED -> "danger";
            default -> "info";
        };
    }
}