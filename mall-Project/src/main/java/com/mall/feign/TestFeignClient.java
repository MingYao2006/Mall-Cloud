package com.mall.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 测试用 Feign 客户端
 * 用于演示 OpenFeign 的负载均衡效果
 */
@FeignClient(
        name = "mall-project",          // 服务名称（用于服务发现）
        contextId = "testFeignClient",  // Bean 唯一标识（避免冲突）
        path = "/api/lb-test"           // 统一前缀，匹配 LoadBalancerTestController
)
public interface TestFeignClient {

    /**
     * 测试接口 - 返回当前服务实例信息
     * 调用路径：/api/lb-test/test/instance
     */
    @GetMapping("/test/instance")
    String getInstanceInfo(@RequestParam(value = "name", defaultValue = "FeignClient") String name);
}
