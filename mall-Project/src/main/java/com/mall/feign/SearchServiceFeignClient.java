package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.EsProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 搜索微服务 Feign 客户端
 * <p>
 * 与商品、分类等功能的整合架构保持一致：
 * 聚合服务（mall-Project）不再直接实现搜索逻辑，统一通过 Feign 调用 mall-search-service 微服务。
 * 搜索微服务内部基于 Elasticsearch 实现。
 */
@FeignClient(
        name = "mall-search-service",
        contextId = "searchServiceFeignClient",
        path = "/search"
)
public interface SearchServiceFeignClient {

    /**
     * 搜索商品
     * GET /search/products?keyword=xxx
     */
    @GetMapping("/products")
    Result<List<EsProduct>> searchProducts(@RequestParam("keyword") String keyword);

    /**
     * 按分类搜索商品
     * GET /search/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    Result<List<EsProduct>> searchByCategory(@PathVariable("categoryId") Long categoryId);

    /**
     * 按品牌搜索商品
     * GET /search/brand?brand=xxx
     */
    @GetMapping("/brand")
    Result<List<EsProduct>> searchByBrand(@RequestParam("brand") String brand);

    /**
     * 全量导入商品数据到 ES（从 DB 同步）
     * POST /search/import
     */
    @PostMapping("/import")
    Result<Integer> importAll();
}
