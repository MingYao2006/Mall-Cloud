package com.mall.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.payment.entity.Payment;
import com.mall.payment.entity.PaymentCallback;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService extends IService<Payment> {

    /**
     * 创建支付
     */
    Payment createPayment(Long orderId, String orderNo, Long userId, BigDecimal amount, Integer paymentType);

    /**
     * 处理支付回调
     */
    boolean handleCallback(String paymentNo, Map<String, String> params, Integer callbackStatus);

    /**
     * 查询支付状态
     */
    Payment getPaymentByOrderNo(String orderNo);

    /**
     * 关闭支付
     */
    boolean closePayment(String paymentNo);
}
