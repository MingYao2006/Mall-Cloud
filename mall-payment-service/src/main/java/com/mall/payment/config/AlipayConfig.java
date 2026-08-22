package com.mall.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.mall.payment.service.strategy.AlipayStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//支付宝配置
@Data
@Configuration
@ConfigurationProperties(prefix = "payment.alipay")
public class AlipayConfig {
    private String appId;
    private String privateKey;// 应用私钥
    private String alipayPublicKey;  // 支付宝公
    private String gatewayUrl;       // 沙箱网关
    private String notifyUrl;        // 异步回调地址
    private String returnUrl;        // 同步跳转地址

    @Bean
    public AlipayClient alipayClient(){
        return new DefaultAlipayClient(
                gatewayUrl, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2"
        );
    }
}
