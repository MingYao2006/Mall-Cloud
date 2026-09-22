package com.mall.config;

import com.mall.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * JWT 拦截器（C端聚合服务）
 * <p>
 * 当前架构：前端 → 网关(9000) → 聚合服务(9090)
 * 网关已验签并下发 X-User-Id 头，此处作为二次校验（防止绕过网关直连聚合服务）。
 * 白名单路径不含 /api 前缀（网关 StripPrefix=2 已剥离 /api/c）。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    // 白名单（精确路径）：网关剥离前缀后，路径形如 /users/login
    private static final Set<String> WHITE_EXACT = Set.of(
            "/users/login",
            "/users/login/code",
            "/users/register",
            "/users/code",
            "/users/wx-login",
            "/users/health",
            "/users/test",
            "/payment/alipay/notify",
            "/product/list",
            "/product/search",
            "/product/page",
            "/product/page/query",
            "/product/category",
            "/product/status",
            "/category/list",
            "/category/children",
            "/category/level",
            "/seckill/list",
            "/coupon/list"
    );

    // 白名单（正则）：带路径变量的只读接口，放行范围与网关侧保持一致
    private static final List<Pattern> WHITE_PATTERN = List.of(
            Pattern.compile("^/product/\\d+$"),
            Pattern.compile("^/product/category/\\d+$"),
            Pattern.compile("^/product/status/\\d+$"),
            Pattern.compile("^/category/\\d+$"),
            Pattern.compile("^/category/children/\\d+$"),
            Pattern.compile("^/category/level/\\d+$"),
            Pattern.compile("^/comment/product/\\d+$")
    );

    /**
     * 判断路径是否命中免登录白名单
     */
    private boolean isWhitePath(String path) {
        if (WHITE_EXACT.contains(path)) {
            return true;
        }
        for (Pattern pattern : WHITE_PATTERN) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // 白名单路径直接放行
        if (isWhitePath(path)) {
            return true;
        }

        // 从请求头获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        String token = authHeader.substring(7);

        // 验证 Token 并提取用户 ID
        try {
            if (!JwtUtil.validateToken(token)) {
                response.setStatus(401);
                return false;
            }
            Long userId = JwtUtil.getUserIdFromToken(token);
            request.setAttribute("userId", userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }
}
