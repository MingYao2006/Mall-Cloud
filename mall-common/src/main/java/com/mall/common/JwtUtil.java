package com.mall.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
// token创建与识别
public class JwtUtil {
//

    /**
     * 密钥由配置提供，禁止写死在源码里（历史版本曾硬编码，随公开仓库泄露过）。
     * 取值优先级：Spring 配置 jwt.secret > JVM 参数 -Djwt.secret > 环境变量 JWT_SECRET。
     * <p>
     * 注意：本类全部为静态方法，调用方不会经过 Spring 代理，因此不能使用 @RefreshScope
     * （懒加载代理会让 @Value 注入不执行），密钥在应用启动时由 Spring 注入一次。
     * 三处都取不到时直接抛异常，避免拿默认密钥静默运行。
     */
    private static volatile String configuredSecret;

    private static final long EXPIRATION = 86400000; // 24小时

    /**
     * Spring 启动时注入；若该模块未扫描 com.mall.common，则走环境变量兜底。
     */
    @Value("${jwt.secret:}")
    public void setSecret(String secret) {
        JwtUtil.configuredSecret = secret;
    }

    private static String resolveSecret() {
        String secret = configuredSecret;
        if (isBlank(secret)) {
            secret = System.getProperty("jwt.secret");
        }
        if (isBlank(secret)) {
            secret = System.getenv("JWT_SECRET");
        }
        if (isBlank(secret)) {
            throw new IllegalStateException(
                    "未配置 JWT 密钥：请设置环境变量 JWT_SECRET（或 JVM 参数 -Djwt.secret=...），"
                            + "也可在配置中心 / application-secret.yml 中设置 jwt.secret。"
                            + "该密钥必须与网关、用户服务保持一致。");
        }
        return secret;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(resolveSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token(生成包含用户ID和用户名的Token，有效期24小时)
     */
    public static String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token(解析Token获取payload数据)
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 Token 是否有效(未过期、签名正确)
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
}
