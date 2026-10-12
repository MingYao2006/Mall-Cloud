package com.mall.gateway.util;

/**
 * 网关侧 JWT 密钥统一入口。
 * <p>
 * 禁止把密钥写死在源码里 —— 历史版本曾硬编码，并随公开仓库泄露过一次。
 * 取值优先级：Spring 配置 jwt.secret &gt; JVM 参数 -Djwt.secret &gt; 环境变量 JWT_SECRET。
 */
public final class JwtSecret {

    private static volatile String configured;

    private JwtSecret() {
    }

    /** 由 Spring 启动时注入（见 AuthGlobalFilter#setJwtSecret）。 */
    public static void setConfigured(String secret) {
        JwtSecret.configured = secret;
    }

    public static String get() {
        String secret = configured;
        if (isBlank(secret)) {
            secret = System.getProperty("jwt.secret");
        }
        if (isBlank(secret)) {
            secret = System.getenv("JWT_SECRET");
        }
        if (isBlank(secret)) {
            throw new IllegalStateException(
                    "未配置 JWT 密钥：请设置环境变量 JWT_SECRET，或在网关配置中设置 jwt.secret。"
                            + "该密钥必须与用户服务、聚合服务保持一致。");
        }
        return secret;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
