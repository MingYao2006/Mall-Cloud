package com.mall.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web MVC 配置类
 * 解决响应消息中文乱码问题
 */
@Configuration // 标记为配置类
public class WebMvcConfig implements WebMvcConfigurer { // 实现 WebMvcConfigurer 接口，自定义 MVC 配置
    
    /**
     * 配置消息转换器
     * 设置响应编码为 UTF-8，解决中文乱码问题
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建字符串消息转换器
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        // 将转换器添加到转换器列表的最前面（优先级最高）
        converters.add(0, converter);
    }
}
