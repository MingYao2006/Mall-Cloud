package com.mall.application;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = "com.mall",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = com.mall.common.config.RedisConfig.class
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = com.mall.common.RedisService.class
        )
    }
)
@MapperScan("com.mall.mapper")
@EnableTransactionManagement
@EnableDiscoveryClient  // 启用服务发现
@EnableFeignClients(basePackages = "com.mall.feign")  // 启用 OpenFeign，指定扫描包
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
