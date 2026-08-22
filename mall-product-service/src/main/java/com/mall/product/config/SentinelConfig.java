package com.mall.product.config;

import com.alibaba.cloud.sentinel.annotation.SentinelRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Sentinel 配置类
 * 用于配置 Sentinel 的相关功能
 */
@Configuration
public class SentinelConfig {

    /**
     * 配置 RestTemplate 支持 Sentinel
     * @return RestTemplate
     */
    @Bean
    @SentinelRestTemplate
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
