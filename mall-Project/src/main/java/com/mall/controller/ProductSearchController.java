package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.EsProduct;
import com.mall.feign.SearchServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品搜索 Controller - 微服务整合版
 * <p>
 * 与商品、分类等功能的整合架构保持一致：
 * 本 Controller 不再直接实现搜索逻辑，统一通过 Feign 调用 mall-search-service 微服务，
 * 搜索微服务内部基于 Elasticsearch 实现。
 */
@RestController
@RequestMapping("/search")
@Tag(name = "搜索服务", description = "商品搜索相关接口（聚合服务代理）")
public class ProductSearchController {

    private final SearchServiceFeignClient searchService;

    public ProductSearchController(SearchServiceFeignClient searchService) {
        this.searchService = searchService;
    }

    /**
     * 搜索商品
     * GET /search/products?keyword=xxx
     */
    @GetMapping("/products")
    @Operation(summary = "搜索商品")
    public Result<List<EsProduct>> searchProducts(@RequestParam String keyword) {
        return searchService.searchProducts(keyword);
    }

    /**
     * 按分类搜索商品
     * GET /search/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类搜索商品")
    public Result<List<EsProduct>> searchByCategory(@PathVariable Long categoryId) {
        return searchService.searchByCategory(categoryId);
    }

    /**
     * 按品牌搜索商品
     * GET /search/brand?brand=xxx
     */
    @GetMapping("/brand")
    @Operation(summary = "按品牌搜索商品")
    public Result<List<EsProduct>> searchByBrand(@RequestParam String brand) {
        return searchService.searchByBrand(brand);
    }

    /**
     * 全量导入商品数据到 ES（从 DB 同步）
     * POST /search/import
     */
    @PostMapping("/import")
    @Operation(summary = "导入商品数据到ES")
    public Result<Integer> importAll() {
        return searchService.importAll();
    }
}
