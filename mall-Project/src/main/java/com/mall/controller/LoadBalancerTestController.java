package com.mall.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负载均衡测试控制器
 * 用于测试 Spring Cloud LoadBalancer 的负载分发效果
 */
@RestController
@RequestMapping("/lb-test")
public class LoadBalancerTestController {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 测试负载均衡 - 获取服务实例信息
     * 
     * @param serviceName 服务名称（默认为当前服务）
     * @return 选中的服务实例信息
     */
    @GetMapping("/instance")
    public Map<String, Object> testLoadBalancer(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = "mall-project";
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 通过负载均衡器选择一个服务实例
            ServiceInstance instance = loadBalancerClient.choose(serviceName);
            
            if (instance != null) {
                result.put("success", true);
                result.put("serviceId", instance.getServiceId());
                result.put("host", instance.getHost());
                result.put("port", instance.getPort());
                result.put("uri", instance.getUri().toString());
                result.put("metadata", instance.getMetadata());
            } else {
                result.put("success", false);
                result.put("message", "未找到服务实例: " + serviceName);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 模拟多次负载均衡调用，统计分发情况
     * 
     * @param serviceName 服务名称
     * @param count 调用次数（默认10次）
     * @return 各实例被选中的次数统计
     */
    @GetMapping("/stats")
    public Map<String, Object> testLoadBalancerStats(String serviceName, Integer count) {
        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = "mall-project";
        }
        if (count == null || count <= 0) {
            count = 10;
        }
        
        Map<String, Integer> instanceCount = new HashMap<>();
        List<String> selectionOrder = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            ServiceInstance instance = loadBalancerClient.choose(serviceName);
            if (instance != null) {
                String key = instance.getHost() + ":" + instance.getPort();
                instanceCount.merge(key, 1, Integer::sum);
                selectionOrder.add(key);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("serviceName", serviceName);
        result.put("totalCalls", count);
        result.put("instanceDistribution", instanceCount);
        result.put("selectionOrder", selectionOrder);
        
        return result;
    }

    /**
     * 通过 RestTemplate 调用服务（演示 @LoadBalanced 效果）
     * 
     * @param serviceName 服务名称
     * @return 调用结果
     */
    @GetMapping("/call")
    public Map<String, Object> callService(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = "mall-project";
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 使用服务名调用（而非具体IP:Port）
            String url = "http://" + serviceName + "/api/actuator/health";
            String response = restTemplate.getForObject(url, String.class);
            
            result.put("success", true);
            result.put("url", url);
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试接口 - 返回当前实例信息
     * 供 Feign 客户端调用，用于测试负载均衡
     */
    @GetMapping("/test/instance")
    public String getInstanceInfo(String name) {
        return "Hello " + name + "! From instance: 192.168.114.1:8082";
    }
}
