package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Order;
import com.mall.feign.OrderServiceFeignClient;
import com.mall.service.AlipayStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 * 处理支付宝沙箱支付
 * <p>
 * 整合架构：订单查询与订单状态更新均通过 Feign 调用 mall-order-service 微服务，
 * 本控制器仅保留支付宝交互逻辑。
 */
@Slf4j
@Tag(name = "支付服务", description = "支付宝沙箱支付相关接口（聚合服务代理）")
@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final AlipayStrategy alipayStrategy;
    private final OrderServiceFeignClient orderServiceFeignClient;

    public PaymentController(AlipayStrategy alipayStrategy, OrderServiceFeignClient orderServiceFeignClient) {
        this.alipayStrategy = alipayStrategy;
        this.orderServiceFeignClient = orderServiceFeignClient;
    }

    /**
     * 发起支付宝支付（返回支付表单 HTML）
     * POST /payment/alipay/pay
     */
    @Operation(summary = "发起支付宝支付")
    @PostMapping("/alipay/pay")
    public Result<Map<String, String>> alipayPay(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        String subject = body.getOrDefault("subject", "商城订单").toString();

        // 通过 Feign 查询订单
        Result<Order> orderResult = orderServiceFeignClient.getOrderEntity(orderId);
        Order order = orderResult != null ? orderResult.getData() : null;
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.fail("订单状态不正确，无法支付");
        }

        String orderNo = order.getOrderNo();
        BigDecimal amount = order.getPayAmount() != null ? order.getPayAmount() : order.getTotalAmount();

        String payForm = alipayStrategy.pagePay(orderNo, amount, subject);

        Map<String, String> result = new HashMap<>();
        result.put("payForm", payForm);
        result.put("orderNo", orderNo);
        return Result.success(result);
    }

    /**
     * 支付宝同步跳转（GET 请求）
     * 支付完成后支付宝会跳转到此地址
     */
    @Operation(summary = "支付宝同步跳转")
    @GetMapping("/alipay/return")
    public String alipayReturn() {
        // 前端 PayResult.vue 会处理，这里直接重定向到前端支付结果页
        return "<script>window.location.href='/pay-result?status=success';</script>";
    }

    /**
     * 支付宝异步通知（POST 请求）
     * 支付宝服务器会异步通知此地址
     */
    @Operation(summary = "支付宝异步通知")
    @PostMapping("/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝回调通知：{}", params);

        // 1. 验签
        if (!alipayStrategy.verifyNotify(params)) {
            log.warn("支付宝回调验签失败");
            return "failure";
        }

        // 2. 处理业务
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            try {
                // 通过 Feign 调用订单微服务更新订单状态为已支付
                orderServiceFeignClient.payOrderByOrderNo(outTradeNo);
                log.info("订单支付成功，订单号：{}", outTradeNo);
            } catch (Exception e) {
                log.error("处理支付回调失败: {}", e.getMessage());
                return "failure";
            }
        }
        return "success";
    }
}