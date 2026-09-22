package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.EsProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-search-service 微服务，
 * 主要用于 ES 数据全量导入等管理操作。
 */
@FeignClient(
        name = "mall-search-service",
        contextId = "searchServiceFeignClient",
        path = "/search"
)
public interface SearchServiceFeignClient {

    @GetMapping("/products")
    Result<List<EsProduct>> searchProducts(@RequestParam("keyword") String keyword);

    @GetMapping("/category/{categoryId}")
    Result<List<EsProduct>> searchByCategory(@PathVariable("categoryId") Long categoryId);

    @GetMapping("/brand")
    Result<List<EsProduct>> searchByBrand(@RequestParam("brand") String brand);

    @PostMapping("/import")
    Result<Integer> importAll();
}
