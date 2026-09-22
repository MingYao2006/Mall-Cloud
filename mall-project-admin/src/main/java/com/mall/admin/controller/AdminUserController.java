package com.mall.admin.controller;

import com.mall.common.Result;
import com.mall.entity.User;
import com.mall.feign.UserServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器（B端）
 * <p>
 * 提供管理员查看用户信息等接口。
 * <p>
 * 注意：用户列表/禁用/启用等管理操作需在 mall-user-service 补充对应接口后在此添加。
 * 管理员的登录走 C 端登录接口，此处不提供登录。
 */
@Tag(name = "用户管理", description = "用户管理接口（管理端）")
@RestController
@RequestMapping("/users")
public class AdminUserController {

    private final UserServiceFeignClient userServiceFeignClient;

    public AdminUserController(UserServiceFeignClient userServiceFeignClient) {
        this.userServiceFeignClient = userServiceFeignClient;
    }

    @Operation(summary = "获取用户信息（管理员视角）")
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return userServiceFeignClient.getUserInfo(authHeader != null ? authHeader : "");
    }

    @Operation(summary = "管理端健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("mall-project-admin is running");
    }
}
