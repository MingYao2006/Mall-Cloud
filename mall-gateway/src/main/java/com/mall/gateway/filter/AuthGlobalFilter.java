package com.mall.gateway.filter;

import com.mall.gateway.util.JwtSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 网关全局认证过滤器
 * <p>
 * 职责：
 * 1. C 端白名单放行（登录/注册/验证码/商品浏览/分类浏览/秒杀列表/优惠券列表/支付回调）
 * 2. 所有非白名单请求必须携带有效 JWT Token
 * 3. B 端请求（/api/admin/**）额外校验 role==1（管理员），非管理员返回 403
 * 4. 解析 Token 后将 userId、username、role 注入请求头，下游聚合服务可直接取用
 * 5. 任何请求进入过滤器先剥离客户端自带的身份头，保证这三个头只有网关一个来源
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 只允许由网关注入的身份头，客户端传入的一律剥离 */
    private static final List<String> IDENTITY_HEADERS = List.of("X-User-Id", "X-Username", "X-Role");

    /**
     * 密钥由配置提供（jwt.secret / 环境变量 JWT_SECRET），禁止硬编码；取值逻辑见 JwtSecret。
     */
    @Value("${jwt.secret:}")
    public void setJwtSecret(String jwtSecret) {
        JwtSecret.setConfigured(jwtSecret);
    }

    // C 端白名单（精确路径）：不需要登录就能访问的接口
    // 注意：路径已包含 /api/c 前缀（网关随后 StripPrefix=2 再转发给聚合服务）
    private static final Set<String> C_WHITE_EXACT = Set.of(
            // 用户认证相关
            "/api/c/users/login",
            "/api/c/users/login/code",
            "/api/c/users/register",
            "/api/c/users/code",
            "/api/c/users/wx-login",
            "/api/c/users/health",
            "/api/c/users/test",
            // 商品浏览（只读）
            "/api/c/product/list",
            "/api/c/product/search",
            "/api/c/product/page",
            "/api/c/product/page/query",
            "/api/c/product/category",
            "/api/c/product/status",
            // 分类浏览（只读）
            "/api/c/category/list",
            "/api/c/category/children",
            "/api/c/category/level",
            // 秒杀/优惠券列表（只读）
            "/api/c/seckill/list",
            "/api/c/coupon/list",
            // AI 导购健康检查
            "/api/ai/health"
    );

    // C 端白名单（正则）：带路径变量的只读接口
    // 用 \d+ 限定数字 ID，避免把 /product/stock/decrease/{id}、/product/sales/{id}
    // 这类会改数据的接口一并放行
    private static final List<Pattern> C_WHITE_PATTERN = List.of(
            Pattern.compile("^/api/c/product/\\d+$"),
            Pattern.compile("^/api/c/product/category/\\d+$"),
            Pattern.compile("^/api/c/product/status/\\d+$"),
            Pattern.compile("^/api/c/category/\\d+$"),
            Pattern.compile("^/api/c/category/children/\\d+$"),
            Pattern.compile("^/api/c/category/level/\\d+$"),
            Pattern.compile("^/api/c/comment/product/\\d+$")
    );

    // 公共白名单：支付回调（C 端和 B 端都不需要 Token）
    private static final Set<String> PUBLIC_WHITE_LIST = Set.of(
            "/api/payment/alipay/notify"
    );

    /**
     * 判断路径是否命中 C 端免登录白名单
     */
    private boolean isCustomerWhitePath(String path) {
        if (C_WHITE_EXACT.contains(path)) {
            return true;
        }
        for (Pattern pattern : C_WHITE_PATTERN) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 0. 无条件剥离客户端自带的用户身份头：这三个头只能由网关注入。
        //    白名单路径也必须剥——否则伪造的 X-User-Id 会原样透传给下游直接取信它的接口。
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        String path = request.getURI().getPath();

        // 1. 公共白名单放行（支付回调等）
        if (PUBLIC_WHITE_LIST.contains(path)) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 2. C 端白名单放行
        if (isCustomerWhitePath(path)) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 3. 获取 Token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            log.warn("请求未携带 Token，路径: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 4. 验证 Token
        try {
            SecretKey key = Keys.hmacShaKeyFor(JwtSecret.get().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String roleStr = claims.get("role", String.class);
            String role = roleStr != null ? roleStr : "0";

            // 5. B 端请求校验管理员权限
            if (path.startsWith("/api/admin/")) {
                if (!"1".equals(role)) {
                    log.warn("非管理员访问管理端接口，路径: {}, userId: {}", path, userId);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                    return exchange.getResponse().writeWith(
                            Mono.just(exchange.getResponse().bufferFactory()
                                    .wrap(("{\"code\":403,\"message\":\"无管理员权限\"}").getBytes()))
                    );
                }
            }

            // 6. 注入用户信息到请求头，下游聚合服务可直接取用
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username != null ? username : "")
                    .header("X-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            log.warn("Token 验证失败，路径: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
