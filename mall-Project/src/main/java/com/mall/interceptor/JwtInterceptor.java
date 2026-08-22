package com.mall.interceptor;

import com.mall.common.JwtUtil;
import com.mall.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT Token 验证拦截器
 * 统一处理需要登录的接口的 Token 验证
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求（跨域预检请求，OPTIONS 请求是浏览器在发送"真正请求"之前，先发一个"试探请求）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Token
        String token = request.getHeader("Authorization");
        
        // Token 为空
        if (token == null || token.isEmpty()) {
            sendErrorResponse(response, 401, "请先登录");
            return false;
        }

        // 去掉 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 Token 是否有效
        if (!JwtUtil.validateToken(token)) {
            sendErrorResponse(response, 401, "Token 已过期或无效，请重新登录");
            return false;
        }

        // Token 验证通过，将用户 ID 存入请求属性，供后续使用
        Long userId = JwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        
        return true;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        Result<Object> result = Result.error(code, message);
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(result));
    }
}
