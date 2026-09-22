package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.EsProduct;
import com.mall.feign.SearchServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品搜索 Controller - C端聚合服务（客户端版）
 * <p>
 * 提供客户端商品搜索接口。
 * ES 全量数据导入接口已迁移至 mall-project-admin。
 */
@RestController
@RequestMapping("/search")
@Tag(name = "搜索服务", description = "商品搜索接口（客户端聚合服务代理）")
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

    // ===== 以下管理端接口已迁移至 mall-project-admin =====
    // POST /search/import → AdminSearchController.importAll
}
