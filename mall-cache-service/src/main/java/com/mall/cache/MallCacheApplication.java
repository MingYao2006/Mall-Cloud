package com.mall.cache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.mall.cache")
@EnableDiscoveryClient
public class MallCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallCacheApplication.class, args);
    }
}