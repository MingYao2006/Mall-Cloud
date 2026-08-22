package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "mall-user-service", path = "/users")
public interface UserServiceFeignClient {

    /**
     * 测试接口
     */
    @GetMapping("/test")
    Result<String> test();

    /**
     * 用户注册
     */
    @PostMapping("/register")
    Result<Object> register(@RequestBody User user);

    /**
     * 用户登录
     */
    @PostMapping("/login")
    Result<Map<String, Object>> login(@RequestBody Map<String, String> params);

    /**
     * 用户注销（退出登录）
     */
    @PostMapping("/logout")
    Result<Object> logout();

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    Result<User> getUserInfo(@RequestHeader("Authorization") String token);

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    Result<Object> updateUserInfo(@RequestBody User user, @RequestHeader("Authorization") String token);

    /**
     * 修改密码
     */
    @PutMapping("/password")
    Result<Object> updatePassword(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token);

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token);

    /**
     * 发送验证码
     */
    @PostMapping("/code")
    Result<String> sendCode(@RequestParam("phone") String phone);

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    Result<Map<String, Object>> health();

    /**
     * 验证码登录
     */
    @PostMapping("/login/code")
    Result<Map<String, Object>> loginByCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
