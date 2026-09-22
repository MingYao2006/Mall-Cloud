package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-user-service 微服务，
 * 用于管理员查看用户信息等操作。
 * <p>
 * 注意：管理员的登录走 C 端的登录接口（/api/c/users/login），
 * 登录后拿到含 role=1 的 Token，然后访问管理端接口。
 */
@FeignClient(
        name = "mall-user-service",
        contextId = "userServiceFeignClient",
        path = "/users"
)
public interface UserServiceFeignClient {

    @GetMapping("/info")
    Result<User> getUserInfo(@RequestHeader("Authorization") String token);

    /**
     * 测试接口
     */
    @GetMapping("/test")
    Result<String> test();
}
