package com.mall.payment.common;

import lombok.Getter;

/**
 * 支付状态枚举
 */
@Getter
public enum PaymentStatusEnum {
    /**
     * 待支付
     */
    PENDING(0, "待支付"),

    /**
     * 支付成功
     */
    SUCCESS(1, "支付成功"),

    /**
     * 支付失败
     */
    FAILED(2, "支付失败"),

    /**
     * 已退款
     */
    REFUNDED(3, "已退款"),

    /**
    * 已关闭
    * */
    CLOSE(4,"已关闭");


    private final Integer code;
    private final String desc;

    PaymentStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举
     */
    public static PaymentStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
