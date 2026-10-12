package com.mall.admin.config;

import com.mall.common.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 管理端 JWT 密钥装配。
 * <p>
 * AdminApplication 的组件扫描范围只有 com.mall.admin，不会创建 com.mall.common 下的 Bean，
 * 而 JwtUtil 的密钥是启动时由 Spring 注入的静态字段，因此这里显式注册 JwtUtil，
 * 保证管理端与网关、用户服务使用同一份密钥（application-local.yaml 的 jwt.secret 或环境变量 JWT_SECRET）。
 */
@Configuration
@Import(JwtUtil.class)
public class AdminJwtSupportConfig {
}
