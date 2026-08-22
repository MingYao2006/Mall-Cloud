package com.mall.cart.feign;

import com.mall.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "mall-product-service",
        contextId = "productServiceFeignClient",
        path = "/product"
)
public interface ProductServiceFeignClient {

    /**
     * 根据 ID 查询商品
     */
    @GetMapping("/{id}")
    Result<?> findById(@PathVariable("id") Long id);

    /**
     * 校验商品是否存在
     */
    @GetMapping("/check/{id}")
    Result<Boolean> checkProductExists(@PathVariable("id") Long id);

    /**
     * 校验库存是否充足
     */
    @GetMapping("/checkstock/{id}")
    Result<Boolean> checkStockEnough(@PathVariable("id") Long id, Integer quantity);
}