package com.mall.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.payment.common.PaymentStatusEnum;
import com.mall.payment.entity.Payment;
import com.mall.payment.mapper.PaymentMapper;
import com.mall.payment.util.PaymentNoGenerator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {
    
    private static final int PAYMENT_EXPIRE_MINUTES = 30;

    @Override
    public Payment createPayment(Long orderId, String orderNo, Long userId, BigDecimal amount, Integer paymentType) {
        Payment payment = new Payment();
        
        String paymentNo = PaymentNoGenerator.generate();
        payment.setPaymentNo(paymentNo);
        
        payment.setOrderId(orderId);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPaymentType(paymentType);
        payment.setStatus(PaymentStatusEnum.PENDING.getCode());
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());

        this.save(payment);
        return payment;
    }

    @Override
    public boolean handleCallback(String paymentNo, java.util.Map<String, String> params, Integer callbackStatus) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentNo, paymentNo);
        Payment payment = this.getOne(wrapper);

        if (payment == null) {
            return false;
        }

        if (payment.getStatus() != PaymentStatusEnum.PENDING.getCode()) {
            // 幂等处理：已支付成功的支付单再次收到相同回调时，视为“已处理”并返回 true，
            // 让支付宝/微信收到成功响应后停止重试，避免因返回失败而无限重试。
            return payment.getStatus().equals(PaymentStatusEnum.SUCCESS.getCode());
        }

        payment.setStatus(callbackStatus);
        payment.setUpdateTime(LocalDateTime.now());
        
        if (callbackStatus.equals(PaymentStatusEnum.SUCCESS.getCode())) {
            payment.setPaymentTime(LocalDateTime.now());
            
            String transactionId = params.get("transactionId");
            if (transactionId != null) {
                payment.setTransactionId(transactionId);
            }
        } else if (callbackStatus.equals(PaymentStatusEnum.FAILED.getCode())) {
            payment.setNote(params.getOrDefault("message", "支付失败"));
        }
        
        return this.updateById(payment);
    }

    @Override
    public Payment getPaymentByOrderNo(String orderNo) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderNo, orderNo);
        return this.getOne(wrapper);
    }

    @Override
    public boolean closePayment(String paymentNo) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentNo, paymentNo);
        Payment payment = this.getOne(wrapper);

        if (payment == null) {
            return false;
        }

        if (!payment.getStatus().equals(PaymentStatusEnum.PENDING.getCode())) {
            return false;
        }

        payment.setStatus(PaymentStatusEnum.CLOSE.getCode());
        payment.setUpdateTime(LocalDateTime.now());
        return this.updateById(payment);
    }
}
