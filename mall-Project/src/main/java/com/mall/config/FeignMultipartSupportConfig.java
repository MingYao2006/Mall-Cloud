package com.mall.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;

/**
 * Feign Multipart 编码器配置
 * 聚合服务通过 Feign 转发文件上传请求（如头像上传）到微服务时，
 * 默认编码器无法编码 MultipartFile，需要 SpringFormEncoder 支持 multipart/form-data。
 */
public class FeignMultipartSupportConfig {

    @Bean
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}
