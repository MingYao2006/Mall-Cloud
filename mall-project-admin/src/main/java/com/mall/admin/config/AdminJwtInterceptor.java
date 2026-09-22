package com.mall.admin.config;

import com.mall.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理端 JWT 拦截器
 * <p>
 * 所有请求必须携带有效 Token，且 Token 中的 role 必须为 1（管理员）。
 * 普通用户（role=0）的 Token 一律返回 403 Forbidden。
 */
@Component
public class AdminJwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 从请求头获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "未登录或Token缺失");
            return false;
        }

        String token = authHeader.substring(7);

        // 验证 Token 有效性
        try {
            if (!JwtUtil.validateToken(token)) {
                sendError(response, 401, "Token无效或已过期");
                return false;
            }

            // 解析角色信息，校验是否为管理员
            var claims = JwtUtil.parseToken(token);
            String roleStr = claims.get("role", String.class);
            int role = roleStr != null ? Integer.parseInt(roleStr) : 0;

            if (role != 1) {
                sendError(response, 403, "无管理员权限");
                return false;
            }

            // 将用户信息放入 request 属性供控制器使用
            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            return true;

        } catch (Exception e) {
            sendError(response, 401, "Token解析失败");
            return false;
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
