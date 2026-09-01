package com.mall.payment.controller;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.payment.dto.CreatePaymentRequest;
import com.mall.payment.entity.Payment;
import com.mall.payment.service.PaymentService;
import com.mall.payment.service.strategy.AlipayStrategy;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final AlipayStrategy alipayStrategy;

    public PaymentController(PaymentService paymentService, AlipayStrategy alipayStrategy) {
        this.paymentService = paymentService;
        this.alipayStrategy = alipayStrategy;
    }

    /**
     * 创建支付
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createPayment(@RequestBody @Valid CreatePaymentRequest request,
                                                     @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // 从 request 对象获取参数
        String finalOrderNo = request.getOrderNo();
        BigDecimal finalAmount = request.getAmount();
        Integer finalPaymentType = request.getPaymentType();

        // 从网关传递的请求头获取 userId
        Long userId = null;
        if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
            try {
                userId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.warn("X-User-Id 格式错误: {}", userIdHeader);
            }
        }
        
        // 请求头中没有用户ID，拒绝请求
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        
        Long finalOrderId = request.getOrderId();

        Payment payment = paymentService.createPayment(finalOrderId, finalOrderNo, userId, finalAmount, finalPaymentType);

        if (finalPaymentType == 2) {
            String payForm = alipayStrategy.pagePay(payment.getPaymentNo(), finalAmount, finalOrderNo);
            Map<String, Object> message = new HashMap<>();
            message.put("paymentNo", payment.getPaymentNo());
            message.put("payForm", payForm);
            return Result.success(message);
        }

        Map<String, Object> messages = new HashMap<>();
        messages.put("paymentNo", payment.getPaymentNo());


        log.info("创建支付成功，支付单号：{}", payment.getPaymentNo());
        return Result.success(messages);
    }

    /**
     * 处理支付回调
     */
    @PostMapping("/handleCallBack/{channel}")
    public Result<String> handleCallback(@RequestBody Map<String, String> params,
                                         @PathVariable("channel") String channel) {
        if (params.get("paymentNo") == null) {
            throw new BusinessException(400, "支付单号不能为空");
        }

        String paymentNo = params.get("paymentNo");

        Integer callbackStatus;
        if ("wechat".equals(channel)) {
            String resultCode = params.get("result_code");
            callbackStatus = "SUCCESS".equals(resultCode) ? 1 : 2;
        } else if ("alipay".equals(channel)) {
            String tradeStatus = params.get("trade_status");
            callbackStatus = "TRADE_SUCCESS".equals(tradeStatus) ? 1 : 2;
        } else {
            callbackStatus = 1;
        }

        boolean success = paymentService.handleCallback(paymentNo, params, callbackStatus);
        if (!success) {
            log.warn("处理回调失败，支付单号：{}", paymentNo);
            throw new BusinessException(400, "处理回调失败");
        }

        log.info("处理回调成功，支付单号：{}，渠道：{}", paymentNo, channel);
        return Result.success("处理回调成功");
    }


    /**
     * 查询支付状态
     */
    @GetMapping("/getPayment/{orderNo}")
    public Result<Payment> getPaymentByOrderNo(@PathVariable("orderNo") String orderNo) {
        Payment paymentByOrderNo = paymentService.getPaymentByOrderNo(orderNo);
        if (paymentByOrderNo == null) {
            throw new BusinessException(400, "支付记录不存在");
        }
        return Result.success(paymentByOrderNo);
    }

    /**
     * 关闭支付
     */
    @PostMapping("/close/{paymentNo}")
    public Result<String> closePayment(@PathVariable("paymentNo") String paymentNo) {
        boolean success = paymentService.closePayment(paymentNo);
        if (!success) {
            log.warn("关闭支付失败，支付单号：{}", paymentNo);
            throw new BusinessException(400, "关闭支付失败");
        }
        log.info("关闭支付成功，支付单号：{}", paymentNo);
        return Result.success("支付已关闭");
    }

    @PostMapping("/pay")
    public Result<?> pay(@RequestParam String orderId,
                         @RequestParam String payType,
                         @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // 验证用户ID是否存在
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        
        Long userId = Long.parseLong(userIdHeader);
        return Result.success("支付请求已接收，用户ID：" + userId);
    }

    @PostMapping("/alipay/notify")
    public String alipaynotify(@RequestParam Map<String, String> params) {
        log.info("支付宝回调通知：{}", params);
//        1. 验签
        if (!alipayStrategy.verifyNotify(params)) {
            log.warn("支付宝回调验证签名失败");
            return "failure";
        }
//        2.处理业务
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 处理业务逻辑
         paymentService.handleCallback(outTradeNo,params,1);
        }
        return "success";
    }
    
}