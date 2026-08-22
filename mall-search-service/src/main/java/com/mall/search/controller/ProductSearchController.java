package com.mall.search.controller;

import com.mall.common.Result;
import com.mall.search.entity.Product;
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
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productSearchService.searchProducts(keyword);
        return Result.success(products);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类搜索商品")
    public Result<List<Product>> searchByCategory(@PathVariable Long categoryId) {
        List<Product> products = productSearchService.searchByCategory(categoryId);
        return Result.success(products);
    }

    @GetMapping("/brand")
    @Operation(summary = "按品牌搜索商品")
    public Result<List<Product>> searchByBrand(@RequestParam String brand) {
        List<Product> products = productSearchService.searchByBrand(brand);
        return Result.success(products);
    }
}