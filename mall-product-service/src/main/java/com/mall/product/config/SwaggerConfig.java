package com.mall.product.config;

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
                        .title("商品服务API文档")
                        .description("商品服务接口文档，包含以下功能模块：\n" +
                                "1. 商品管理：商品的增删改查\n" +
                                "2. 分类管理：商品分类的查询\n" +
                                "3. 库存管理：商品库存的查询和更新\n" +
                                "4. 图片上传：商品图片的上传和管理")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("明耀")
                                .email("mingyao060924@outlook.com")
                                .url("https://www.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
