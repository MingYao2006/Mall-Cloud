package com.mall.admin.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.query.ProductQuery;
import com.mall.feign.ProductServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品管理控制器（B端）
 * <p>
 * 提供商品的增删改、上下架、库存管理、图片上传等管理接口。
 * 所有请求经过 AdminJwtInterceptor 校验管理员权限后到达此处。
 */
@Tag(name = "商品管理", description = "商品管理接口（管理端）")
@RestController
@RequestMapping("/product")
public class AdminProductController {

    private final ProductServiceFeignClient productService;

    public AdminProductController(ProductServiceFeignClient productService) {
        this.productService = productService;
    }

    @Operation(summary = "添加商品")
    @PostMapping
    public Result<Object> add(@RequestBody Product product) {
        return productService.add(product);
    }

    @Operation(summary = "更新商品")
    @PutMapping
    public Result<Object> update(@RequestBody Product product) {
        return productService.update(product);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        return productService.delete(id);
    }

    @Operation(summary = "根据ID查询商品详情")
    @GetMapping("/{id}")
    public Result<Product> findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @Operation(summary = "获取所有商品列表")
    @GetMapping("/list")
    public Result<List<Product>> findAll() {
        return productService.findAll();
    }

    @Operation(summary = "根据分类ID查询商品列表")
    @GetMapping("/category/{categoryId}")
    public Result<List<Product>> listProductsByCategory(@PathVariable Long categoryId) {
        return productService.listProductsByCategory(categoryId);
    }

    @Operation(summary = "根据状态查询商品列表")
    @GetMapping("/status/{status}")
    public Result<List<Product>> listProductsByStatus(@PathVariable Integer status) {
        return productService.listProductsByStatus(status);
    }

    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    public Result<PageResult> listProductsByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return productService.listProductsByPage(pageNum, pageSize);
    }

    @Operation(summary = "条件组合分页查询")
    @PostMapping("/page/query")
    public Result<PageResult> listProductsByCondition(@RequestBody ProductQuery query) {
        return productService.listProductsByCondition(query);
    }

    @Operation(summary = "更新库存")
    @PostMapping("/stock/{id}")
    public Result<String> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        return productService.updateStock(id, stock);
    }

    @Operation(summary = "增加库存")
    @PostMapping("/stock/increase/{id}")
    public Result<Object> increaseStock(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.increaseStock(id, quantity);
    }

    @Operation(summary = "上架商品")
    @PostMapping("/publish/{id}")
    public Result<String> publishProduct(@PathVariable Long id) {
        return productService.publishProduct(id);
    }

    @Operation(summary = "下架商品")
    @PostMapping("/unpublish/{id}")
    public Result<String> unpublishProduct(@PathVariable Long id) {
        return productService.unpublishProduct(id);
    }

    @Operation(summary = "上传商品主图")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return productService.uploadImage(file);
    }

    @Operation(summary = "校验商品是否存在")
    @GetMapping("/check/{id}")
    public Result<Boolean> checkProductExists(@PathVariable Long id) {
        return productService.checkProductExists(id);
    }

    @Operation(summary = "校验库存是否充足")
    @GetMapping("/checkstock/{id}")
    public Result<Boolean> checkStockEnough(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.checkStockEnough(id, quantity);
    }
}
