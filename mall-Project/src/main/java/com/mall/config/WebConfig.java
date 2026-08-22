package com.mall.config;

import com.mall.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 配置拦截器和跨域
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;


    @Bean
    @LoadBalanced  // 开启负载均衡，通过服务名调用
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 只拦截需要登录的路径
                .addPathPatterns(
                        "/users/**",          // 用户操作（info、password、avatar）
                        "/orders/**",         // 订单操作
                        "/cart/**",           // 购物车操作
                        "/product/stock/**",  // 库存修改（需登录）
                        "/product/publish/**",// 上下架（需登录）
                        "/product/sales/**"   // 销量修改（需登录）
                )
                // 放行不需要登录的接口
                .excludePathPatterns(
                        // 用户模块 — 公开
                        "/users/register",
                        "/users/login",
                        "/users/login/code",    // 验证码登录
                        "/users/code",          // 发送验证码
                        "/users/test",
                        // 产品模块 — 公开浏览
                        "/product/list",
                        "/product/page",
                        "/product/page/query",
                        "/product/search",
                        "/product/category/**",
                        "/product/status/**",
                        "/product/check/**",
                        "/product/checkstock/**",
                        "/product/{id:[0-9]+}",      // GET /product/123
                        "/product/upload/image",
                        // 分类模块 — 完全公开
                        "/category/**"
                );
    }

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源映射
     * 让上传的文件可以通过 URL 访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** 到项目根目录下的 uploads 文件夹
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}