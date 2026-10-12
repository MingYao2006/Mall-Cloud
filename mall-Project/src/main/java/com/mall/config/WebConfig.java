package com.mall.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * 文件上传根目录。写入方是 mall-user-service，本服务是读取方（/uploads/** 静态映射），
     * 两者必须指向同一个绝对路径，否则头像会 404。
     */
    @Value("${file.upload-dir}")
    private String fileUploadDir;

    @Bean
    @LoadBalanced  // 开启负载均衡，通过服务名调用
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 配置拦截器
     * 当前架构：Vite 代理直连 mall-Project（未经网关），由 JWT 拦截器自行验签
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/uploads/**",
                        // Swagger 文档路径
                        "/doc.html",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/swagger-resources/**"
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
     *
     * 目录来自 file.upload-dir，与写入方 mall-user-service 的 UserService 共用同一配置项。
     * 这里不能用 System.getProperty("user.dir")：它是各 JVM 进程自己的启动目录，
     * 本服务与 mall-user-service 的启动目录不同，会导致"文件写进去了却映射不到"。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 去掉配置里可能带的结尾斜杠，再统一补一个，避免出现 file:/path// 这种写法
        String uploadPath = fileUploadDir.replaceAll("/+$", "") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}