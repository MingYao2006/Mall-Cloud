package com.mall.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 管理端 Swagger 配置
 */
@Configuration
public class AdminSwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("商城管理端API文档（B端）")
                        .description("管理端聚合服务接口文档，包含以下管理模块：\n" +
                                "1. 商品管理：商品增删改、上下架、库存管理\n" +
                                "2. 分类管理：分类增删改、状态管理\n" +
                                "3. 评论管理：评论审核\n" +
                                "4. 搜索管理：ES数据导入\n" +
                                "5. 订单管理：订单查看、发货\n" +
                                "6. 用户管理：用户列表、禁用/启用\n" +
                                "7. 优惠券管理：优惠券创建、发放")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("67161")
                                .url("https://www.example.com")));
    }
}
