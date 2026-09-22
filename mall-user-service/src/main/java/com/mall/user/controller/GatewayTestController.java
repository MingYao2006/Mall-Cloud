package com.mall.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关认证测试控制器
 * 用于验证网关的 Token 认证和用户信息传递功能
 */
@Tag(name = "网关认证测试", description = "验证网关认证和用户信息传递")
@RestController
@RequestMapping("/user")
public class GatewayTestController {

    /**
     * 测试接口 - 验证网关认证和用户信息传递
     * 访问路径：/user/test
     * 需要通过网关认证才能访问
     */
    @Operation(summary = "网关认证测试")
    @GetMapping("/test")
    public Map<String, Object> test(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "成功");
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "网关认证测试成功");
        data.put("userId", userId);
        data.put("username", username);
        data.put("timestamp", LocalDateTime.now());
        data.put("description", "如果您看到此消息，说明网关认证功能正常工作");
        
        result.put("data", data);
        
        return result;
    }
}
