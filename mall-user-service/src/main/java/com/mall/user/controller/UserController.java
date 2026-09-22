package com.mall.user.controller;



import com.mall.common.JwtUtil;
import com.mall.common.Result;
import com.mall.user.dto.LoginRequest;
import com.mall.user.dto.UpdatePasswordRequest;
import com.mall.user.dto.WxLoginRequest;
import com.mall.user.entity.User;
import com.mall.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户服务", description = "用户注册登录相关接口")
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 测试接口
     */
    @Operation(summary = "测试接口")
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("接口正常工作");
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Object> register(@RequestBody User user) {
        return userService.register(user);
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }

    /**
     * 用户注销（退出登录）
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Object> logout() {
        // JWT Token 无法在服务端销毁，客户端删除 Token 即可
        // 这里只是返回成功响应，实际注销由前端删除 Token 完成
        return Result.success("退出登录成功");
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        // 从拦截器设置的请求属性中获取用户 ID
        Long userId = resolveUserId(request);
        return userService.findById(userId);
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public Result<Object> updateUserInfo(@RequestBody User user, HttpServletRequest request) {
        // 从拦截器设置的请求属性中获取用户 ID，并直接调用本服务更新
        user.setId(resolveUserId(request));
        return userService.updateUserInfo(user);
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Object> updatePassword(@RequestBody @Valid UpdatePasswordRequest passwordRequest, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return userService.updatePassword(userId, passwordRequest.getOldPassword(), passwordRequest.getNewPassword());

    }

    /**
     * 上传头像
     */
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return userService.uploadAvatar(userId, file);
    }

    /**
     * 发送验证码
     */
    @Operation(summary = "发送验证码")
    @PostMapping("/code")
    public Result<String> sendCode(@RequestParam String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 验证码登录
     */
    @Operation(summary = "验证码登录")
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@RequestParam String phone, @RequestParam String code) {
        return userService.loginByCode(phone, code);
    }

    /**
     * 微信小程序登录
     * <p>
     * 小程序端 uni.login() 拿到 code 后调用本接口，
     * 服务端用 code 换取 openid，按 openid 匹配或自动注册账号，最后签发 JWT。
     */
    @Operation(summary = "微信小程序登录")
    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody @Valid WxLoginRequest request) {
        return userService.wxLogin(request);
    }

    /**
     * 从请求中解析当前用户 ID
     */
    private Long resolveUserId(HttpServletRequest request) {
        // 网关透传的用户 ID
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // Authorization Bearer Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (JwtUtil.validateToken(token)) {
                    return JwtUtil.getUserIdFromToken(token);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

}

