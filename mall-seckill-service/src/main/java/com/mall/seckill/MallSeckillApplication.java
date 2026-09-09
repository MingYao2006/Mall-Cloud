package com.mall.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.mall.seckill", "com.mall.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.mall.seckill.feign")
@MapperScan("com.mall.seckill.mapper")
public class MallSeckillApplication {
    public static void main(String[] args) {

        SpringApplication.run(MallSeckillApplication.class, args);
    }
}
