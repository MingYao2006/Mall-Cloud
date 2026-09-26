package com.mall.feign;

import com.mall.common.Result;
import com.mall.config.FeignMultipartSupportConfig;
import com.mall.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(
        name = "mall-user-service",
        contextId = "userServiceFeignClient",
        path = "/users",
        configuration = FeignMultipartSupportConfig.class
)
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
     * 注意：文件参数必须用 @RequestPart（而非 @RequestParam），
     * 否则会被当作 URL 查询参数序列化，导致下游服务收不到文件
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> uploadAvatar(@RequestPart("file") MultipartFile file, @RequestHeader("Authorization") String token);

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
     * 绑定手机号（需短信验证码，已登录用户专用）
     */
    @PostMapping("/phone/bind")
    Result<Object> bindPhone(@RequestParam("phone") String phone,
                             @RequestParam("code") String code,
                             @RequestHeader("Authorization") String token);

    /**
     * 验证码登录
     */
    @PostMapping("/login/code")
    Result<Map<String, Object>> loginByCode(@RequestParam("phone") String phone, @RequestParam("code") String code);

    /**
     * 微信小程序登录
     * 请求体字段：code（必填）、nickname、avatarUrl
     */
    @PostMapping("/wx-login")
    Result<Map<String, Object>> wxLogin(@RequestBody Map<String, String> params);
}
