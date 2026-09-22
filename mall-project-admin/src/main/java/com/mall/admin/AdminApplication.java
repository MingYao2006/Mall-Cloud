package com.mall.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 管理端聚合服务启动类（B端 BFF）
 * <p>
 * 仅提供后台管理接口，所有请求通过 Feign 代理转发到各底层微服务。
 * 拦截器校验 JWT Token 中的 role 字段，非管理员（role != 1）一律拒绝。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.mall.feign")
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
