package com.mall.search.controller;

import com.mall.common.Result;
import com.mall.search.entity.EsProduct;
import com.mall.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@Tag(name = "搜索服务", description = "商品搜索相关接口")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @GetMapping("/products")
    @Operation(summary = "搜索商品")
    public Result<List<EsProduct>> searchProducts(@RequestParam String keyword) {
        List<EsProduct> products = productSearchService.searchProducts(keyword);
        return Result.success(products);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类搜索商品")
    public Result<List<EsProduct>> searchByCategory(@PathVariable Long categoryId) {
        List<EsProduct> products = productSearchService.searchByCategory(categoryId);
        return Result.success(products);
    }

    @GetMapping("/brand")
    @Operation(summary = "按品牌搜索商品")
    public Result<List<EsProduct>> searchByBrand(@RequestParam String brand) {
        List<EsProduct> products = productSearchService.searchByBrand(brand);
        return Result.success(products);
    }

    /**
     * 全量导入商品数据到 ES（从 DB 同步）
     */
    @PostMapping("/import")
    @Operation(summary = "导入商品数据到ES")
    public Result<Integer> importAll() {
        int count = productSearchService.importAll();
        return Result.success(count);
    }
}