package com.mall.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置类
 * 用于配置 API 文档的基本信息
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("商城系统API文档")
                        .description("商城系统接口文档，包含以下功能模块：\n" +
                                "1. 用户服务：用户注册、登录、个人信息管理\n" +
                                "2. 商品服务：商品的增删改查、分类管理\n" +
                                "3. 订单服务：订单创建、查询、支付\n" +
                                "4. 购物车：购物车管理\n" +
                                "5. 支付服务：支付宝支付集成\n" +
                                "6. 评论服务：商品评论管理\n" +
                                "7. 搜索服务：商品搜索\n" +
                                "8. 秒杀服务：限时秒杀活动")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("mingayao")
                                .email("mingyao060924@outlook.com")
                                .url("https://www.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
