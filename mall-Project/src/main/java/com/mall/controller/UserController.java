package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.User;
import com.mall.feign.UserServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户聚合入口
 * <p>
 * 整合架构：本控制器不再实现任何用户业务逻辑，
 * 全部通过 Feign 代理转发到 mall-user-service 微服务。
 */
@Tag(name = "用户服务", description = "用户相关接口（聚合服务代理）")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserServiceFeignClient userServiceFeignClient;

    public UserController(UserServiceFeignClient userServiceFeignClient) {
        this.userServiceFeignClient = userServiceFeignClient;
    }

    /**
     * 测试接口（转发到用户微服务）
     */
    @Operation(summary = "测试接口")
    @GetMapping("/test")
    public Result<String> test() {
        return userServiceFeignClient.test();
    }

    /**
     * 用户注册（转发到用户微服务）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Object> register(@RequestBody User user) {
        return userServiceFeignClient.register(user);
    }

    /**
     * 用户登录（转发到用户微服务）
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return userServiceFeignClient.login(params);
    }

    /**
     * 用户注销（退出登录，转发到用户微服务）
     */
    @Operation(summary = "用户注销")
    @PostMapping("/logout")
    public Result<Object> logout() {
        return userServiceFeignClient.logout();
    }

    /**
     * 获取当前用户信息（转发到用户微服务）
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        return userServiceFeignClient.getUserInfo(resolveToken(request));
    }

    /**
     * 更新用户信息（转发到用户微服务）
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public Result<Object> updateUserInfo(@RequestBody User user, HttpServletRequest request) {
        return userServiceFeignClient.updateUserInfo(user, resolveToken(request));
    }

    /**
     * 修改密码（转发到用户微服务）
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Object> updatePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        return userServiceFeignClient.updatePassword(params, resolveToken(request));
    }

    /**
     * 上传头像（转发到用户微服务）
     */
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return userServiceFeignClient.uploadAvatar(file, resolveToken(request));
    }

    /**
     * 绑定手机号（需短信验证码，转发到用户微服务）
     */
    @Operation(summary = "绑定手机号")
    @PostMapping("/phone/bind")
    public Result<Object> bindPhone(@RequestParam String phone, @RequestParam String code, HttpServletRequest request) {
        return userServiceFeignClient.bindPhone(phone, code, resolveToken(request));
    }

    /**
     * 发送验证码（转发到用户微服务）
     */
    @Operation(summary = "发送验证码")
    @PostMapping("/code")
    public Result<String> sendCode(@RequestParam String phone) {
        return userServiceFeignClient.sendCode(phone);
    }


    /**
     * 健康检查接口（用于负载均衡器检测）
     */
    @Operation(summary = "健康检查接口")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new java.util.HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "mall-project");
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("javaVersion", System.getProperty("java.version"));
        healthInfo.put("osName", System.getProperty("os.name"));
        return Result.success(healthInfo);
    }

    /**
     * 验证码登录（转发到用户微服务）
     */
    @Operation(summary = "验证码登录")
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@RequestParam String phone, @RequestParam String code) {
        return userServiceFeignClient.loginByCode(phone, code);
    }

    /**
     * 微信小程序登录（转发到用户微服务）
     * <p>
     * 请求体：{ code, nickname, avatarUrl }
     * 免登录接口，已在网关与聚合层白名单中放行。
     */
    @Operation(summary = "微信小程序登录")
    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, String> params) {
        return userServiceFeignClient.wxLogin(params);
    }

    /**
     * 从请求中提取 Authorization 头，透传给用户微服务做身份解析
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null ? authHeader : "";
    }

}
