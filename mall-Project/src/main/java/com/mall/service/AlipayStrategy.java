package com.mall.service;

import cn.hutool.json.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.mall.config.AlipayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付策略
 */
@Slf4j
@Component
public class AlipayStrategy {

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 电脑网页支付 - 生成支付表单 HTML
     */
    public String pagePay(String outTradeNo, BigDecimal totalAmount, String subject) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(alipayConfig.getReturnUrl());
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", totalAmount);
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        try {
            log.info("========== 支付宝支付请求开始 ==========");
            log.info("支付流水号: {}", outTradeNo);
            log.info("支付金额: {}", totalAmount);
            log.info("订单标题: {}", subject);

            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

            log.info("支付宝支付表单生成成功");
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝支付请求失败: {}", e.getErrMsg());
            throw new RuntimeException("支付宝下单失败: " + e.getErrMsg(), e);
        }
    }

    /**
     * 验证支付宝回调签名
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签失败", e);
            return false;
        }
    }
}