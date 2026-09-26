package com.mall.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * Feign 全局错误解码配置
 * <p>
 * 默认的 ErrorDecoder 会把下游微服务返回的 4xx 业务错误包装成 FeignException，
 * 导致 GlobalExceptionHandler 将其当作系统异常（500）处理并打印完整堆栈。
 * <p>
 * 本配置解析下游统一 Result 结构（code/message）中的业务信息，
 * 转成 BusinessException，由 GlobalExceptionHandler 按业务异常以原状态码和原提示返回前端。
 */
@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new ErrorDecoder() {
            private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

            @Override
            public Exception decode(String methodKey, Response response) {
                // 仅处理 4xx 客户端错误，这些通常是下游抛出的业务异常
                if (response.status() >= 400 && response.status() < 500) {
                    try {
                        byte[] bytes = response.body() == null ? null
                                : response.body().asInputStream().readAllBytes();
                        if (bytes != null && bytes.length > 0) {
                            String body = new String(bytes, StandardCharsets.UTF_8);
                            JsonNode node = objectMapper.readTree(body);
                            int code = node.path("code").asInt(400);
                            String message = node.path("message").asText("下游服务调用失败");
                            log.warn("Feign 调用下游服务返回业务错误: methodKey={}, status={}, code={}, message={}",
                                    methodKey, response.status(), code, message);
                            return new BusinessException(code, message);
                        }
                    } catch (Exception ignore) {
                        log.warn("解析下游错误响应失败，回退默认错误解码: methodKey={}, status={}",
                                methodKey, response.status());
                    }
                }
                // 5xx 及解析失败场景保持默认行为
                return defaultDecoder.decode(methodKey, response);
            }
        };
    }
}
