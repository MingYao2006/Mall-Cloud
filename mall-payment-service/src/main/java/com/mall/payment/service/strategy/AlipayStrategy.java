package com.mall.payment.service.strategy;

import cn.hutool.json.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.mall.payment.config.AlipayConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付策略
 */
@Data
@Slf4j
@Component
public class AlipayStrategy {

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 电脑网页支付 - 生成支付表单
     */
    public String pagePay(String ourTradeNo, BigDecimal totalAmount, String subject) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(alipayConfig.getReturnUrl());
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", ourTradeNo);
        bizContent.put("total_amount", totalAmount);
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        try {
            // 添加请求参数日志
            log.info("========== 支付宝支付请求开始 ==========");
            log.info("支付流水号: {}", ourTradeNo);
            log.info("支付金额: {}", totalAmount);
            log.info("订单标题: {}", subject);
            log.info("同步跳转URL: {}", alipayConfig.getReturnUrl());
            log.info("异步回调URL: {}", alipayConfig.getNotifyUrl());
            log.info("业务参数: {}", bizContent.toString());
            
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            
            log.info("支付宝支付表单生成成功");
            log.info("========== 支付宝支付请求成功 ==========");
            return response.getBody();
        } catch (com.alipay.api.AlipayApiException e) {
            log.error("========== 支付宝支付请求失败 ==========");
            log.error("错误码: {}", e.getErrCode());
            log.error("错误消息: {}", e.getErrMsg());
            log.error("完整异常信息", e);
            throw new RuntimeException("支付宝下单失败: " + e.getErrMsg(), e);
        } catch (Exception e) {
            log.error("========== 支付宝支付请求异常 ==========");
            log.error("未知异常", e);
            throw new RuntimeException("支付宝下单失败: " + e.getMessage(), e);
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
            log.error("验签失败", e);
            return false;
        }
    }
}
