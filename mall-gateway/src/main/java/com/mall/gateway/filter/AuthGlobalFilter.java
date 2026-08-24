package com.mall.gateway.filter;

import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String SECRET = "mall-project-secret-key-2026-for-jwt-token-generation";

    // 白名单：不需要登录就能访问的路径

    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",
            "/api/users/login",

            "/api/user/register",
            "/api/users/register",

            "/api/user/code",
            "/api/users/code",

            "/api/user/login/code",
            "/api/users/login/code",

            "/api/user/health",
            "/api/users/health",

            "/api/payment/alipay/notify",

            "/api/payment/test/token"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 从 exchange 中获取请求对象
        ServerHttpRequest request = (ServerHttpRequest) exchange.getRequest();
        // 获取请求的路径
        String path = request.getURI().getPath();
        
        // 1. 白名单放行：检查请求路径是否在白名单中
        for (String whitePath : WHITE_LIST) {
            // 如果请求路径包含白名单中的路径
            if (path.contains(whitePath)) {
                // 直接放行，不进行 Token 验证
                return chain.filter(exchange);
            }
        }

        // 2. 获取 Token：从请求头中获取 Authorization 字段
        String token = request.getHeaders().getFirst("Authorization");
        // 如果 Token 为空或空字符串
        if (token == null || token.isEmpty()) {
            // 记录警告日志
            log.warn("请求未携带 Token，路径: {}", path);
            // 设置响应状态码为 401 未授权
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            // 返回响应，结束请求
            return exchange.getResponse().setComplete();
        }
        
        // 去掉 Bearer 前缀：如果 Token 以 "Bearer " 开头
        if (token.startsWith("Bearer ")) {
            // 截取 "Bearer " 之后的部分，得到真正的 Token
            token = token.substring(7);
        }
        
        // 3. 验证 Token：使用 JWT 库验证 Token 的有效性
        try {
            // 使用密钥生成 HMAC-SHA 密钥对象
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
            // 构建 JWT 解析器，解析 Token 并获取 Claims（声明信息）
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)  // 设置签名密钥
                    .build()  // 构建解析器
                    .parseClaimsJws(token)  // 解析 Token
                    .getBody();  // 获取 Token 的主体部分（Claims）

            // 4. 把 userId 放进请求头，下游服务可以直接取
            // 从 Claims 中获取用户 ID（subject 字段）
            String userId = claims.getSubject();
            
            // 构建新的请求对象，添加自定义请求头
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Id", userId)  // 添加用户 ID 到请求头
                    .header("X-Username", claims.get("username", String.class))  // 添加用户名到请求头
                    .build();  // 构建新的请求对象

            // 使用新的请求对象继续执行过滤器链
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            // Token 验证失败，记录警告日志
            log.warn("Token 验证失败，路径: {}", path);
            // 设置响应状态码为 401 未授权
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            // 返回响应，结束请求
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;  // 优先级最高
    }
}
