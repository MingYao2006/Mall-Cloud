package com.mall.controller;

import com.mall.common.BusinessException;
import com.mall.feign.TestFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign 负载均衡测试控制器
 */
@Tag(name = "Feign负载均衡测试", description = "Feign 调用与负载均衡测试接口")
@RestController
@RequestMapping("/feign-test")
public class FeignTestController {

    @Autowired
    private TestFeignClient testFeignClient;

    /**
     * 使用 Feign 调用服务（自动负载均衡）
     */
    @Operation(summary = "使用Feign调用服务")
    @GetMapping("/call")
    public Map<String, Object> callService() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 通过 Feign 调用 mall-project 服务
            // OpenFeign 会自动负载均衡到不同实例
            String response = testFeignClient.getInstanceInfo("FeignTest");
            result.put("success", true);
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 多次调用统计负载均衡分布
     */
    @Operation(summary = "统计负载均衡分布")
    @GetMapping("/stats")
    public Map<String, Object> testLoadBalancerStats(Integer count) {
        if (count == null || count <= 0) {
            count = 10;
        }

        Map<String, Integer> instanceCount = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < count; i++) {
            try {
                String response = testFeignClient.getInstanceInfo("Stats-" + i);
                // 解析响应获取实例信息
                String instanceKey = parseInstanceFromResponse(response);
                instanceCount.merge(instanceKey, 1, Integer::sum);
            } catch (Exception e) {
                instanceCount.merge("error", 1, Integer::sum);
            }
        }

        result.put("totalCalls", count);
        result.put("instanceDistribution", instanceCount);
        return result;
    }

    /**
     * 从响应中解析实例信息
     */
    private String parseInstanceFromResponse(String response) {
        // 简单解析，实际项目中可以用 JSON 解析
        if (response == null) return "unknown";
        if (response.contains("8082")) return "8082";
        if (response.contains("8083")) return "8083";
        if (response.contains("8084")) return "8084";
        return "unknown";
    }
}
