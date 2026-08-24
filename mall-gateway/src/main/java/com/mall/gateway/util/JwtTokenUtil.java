package com.mall.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 * 用于生成和解析 JWT Token
 */
public class JwtTokenUtil {

    /**
     * 密钥（必须与 AuthGlobalFilter 中的 SECRET 一致）
     */
    private static final String SECRET = "mall-project-secret-key-2026-for-jwt-token-generation";

    /**
     * Token 有效期（毫秒）- 24小时
     */
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    /**
     * 生成 HMAC-SHA 密钥
     */
    private static SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token 字符串
     */
    public static String generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token 字符串
     * @return Claims 对象，包含用户信息
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token 字符串
     * @return 用户ID
     */
    public static String getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token 字符串
     * @return 用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token 字符串
     * @return true-有效，false-无效
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
     * 主方法 - 用于测试生成 Token
     */
    public static void main(String[] args) {
        // 生成测试 Token
        String userId = "1001";
        String username = "testuser";
        
        String token = generateToken(userId, username);
        
        System.out.println("========== JWT Token 生成成功 ==========");
        System.out.println("用户ID: " + userId);
        System.out.println("用户名: " + username);
        System.out.println("========================================");
        System.out.println();
        System.out.println("【完整 Token（请复制此行）】");
        System.out.println(token);
        System.out.println();
        System.out.println("【完整 Authorization 头（请复制此行）】");
        System.out.println("Bearer " + token);
        System.out.println();
        System.out.println("========== 测试命令 ==========");
        System.out.println("1. 不带 Token 测试（应返回 401）:");
        System.out.println("curl http://localhost:9000/api/user/test");
        System.out.println();
        System.out.println("2. 带 Token 测试（应返回 404 或 200）:");
        System.out.println("curl -H \"Authorization: Bearer " + token + "\" http://localhost:9000/api/user/test");
        System.out.println();
        System.out.println("3. Postman 测试:");
        System.out.println("URL: http://localhost:9000/api/user/test");
        System.out.println("Header Key: Authorization");
        System.out.println("Header Value: Bearer " + token);
        System.out.println();
        System.out.println("========== Token 验证 ==========");
        System.out.println("Token 是否有效: " + validateToken(token));
        System.out.println("用户ID: " + getUserId(token));
        System.out.println("用户名: " + getUsername(token));
    }
}
