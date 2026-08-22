package com.mall.user.controller;



import com.mall.common.Result;
import com.mall.user.dto.LoginRequest;
import com.mall.user.dto.UpdatePasswordRequest;
import com.mall.user.entity.User;
import com.mall.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("接口正常工作");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Object> register(@RequestBody User user) {
        return userService.register(user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }

    /**
     * 用户注销（退出登录）
     */
    @PostMapping("/logout")
    public Result<Object> logout() {
        // JWT Token 无法在服务端销毁，客户端删除 Token 即可
        // 这里只是返回成功响应，实际注销由前端删除 Token 完成
        return Result.success("退出登录成功");
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        // 从拦截器设置的请求属性中获取用户 ID
        Long userId = (Long) request.getAttribute("userId");
        return userService.findById(userId);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Object> updateUserInfo(@RequestBody User user, HttpServletRequest request) {
        // 从拦截器设置的请求属性中获取用户 ID
        Long userId = (Long) request.getAttribute("userId");
        user.setId(userId);
        return userService.updateUserInfo(user);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Object> updatePassword(@RequestBody @Valid UpdatePasswordRequest passwordRequest, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.updatePassword(userId, passwordRequest.getOldPassword(), passwordRequest.getNewPassword());

    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.uploadAvatar(userId, file);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/code")
    public Result<String> sendCode(@RequestParam String phone) {
        return userService.sendCode(phone);
    }


    /**
     * 健康检查接口（用于负载均衡器检测）
     */
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
     * 验证码登录
     */
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@RequestParam String phone, @RequestParam String code) {
        return userService.loginByCode(phone, code);
    }

}