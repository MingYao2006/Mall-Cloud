package com.mall.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.mall.payment.mapper")
public class MallPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallPaymentApplication.class, args);
    }
}
