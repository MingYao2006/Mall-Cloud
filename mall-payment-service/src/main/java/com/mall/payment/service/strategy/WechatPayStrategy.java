package com.mall.payment.service.strategy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 微信支付策略
@Data
@Component
@ConfigurationProperties(prefix = "payment.wechat")
public class WechatPayStrategy {
    private String appId;

    private String mchId;

    private String apiKey;

    private String notifyUrl;

    private String privateKeyPath;

    private String certPath;
}
