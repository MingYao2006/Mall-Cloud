package com.mall.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.query.ProductQuery;
import com.mall.feign.ProductServiceFeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品Controller - 微服务整合版
 * <p>
 * 与分类功能保持一致的整合架构：
 * 本 Controller 不再直接操作数据库，统一通过 Feign 调用 mall-product-service 微服务。
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductServiceFeignClient productService;

    public ProductController(ProductServiceFeignClient productService) {
        this.productService = productService;
    }

    /**
     * 添加商品
     * POST /product
     */
    @PostMapping
    public Result<Object> add(@RequestBody Product product) {
        return productService.add(product);
    }

    /**
     * 更新商品
     * PUT /product
     */
    @PutMapping
    public Result<Object> update(@RequestBody Product product) {
        return productService.update(product);
    }

    /**
     * 删除商品
     * DELETE /product/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        return productService.delete(id);
    }

    /**
     * 根据ID查询商品详情
     * GET /product/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    /**
     * 获取所有商品列表
     * GET /product/list
     */
    @GetMapping("/list")
    public Result<List<Product>> findAll() {
        return productService.findAll();
    }

    /**
     * 根据分类ID查询商品列表
     * GET /product/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public Result<List<Product>> listProductsByCategory(@PathVariable Long categoryId) {
        return productService.listProductsByCategory(categoryId);
    }

    /**
     * 根据状态查询商品列表
     * GET /product/status/{status}
     */
    @GetMapping("/status/{status}")
    public Result<List<Product>> listProductsByStatus(@PathVariable Integer status) {
        return productService.listProductsByStatus(status);
    }

    /**
     * 搜索商品（名称模糊查询）
     * GET /product/search?keyword=xxx
     */
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    /**
     * 分页查询商品
     * GET /product/page?pageNum=1&pageSize=10
     */
    @GetMapping("/page")
    public Result<PageResult> listProductsByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return productService.listProductsByPage(pageNum, pageSize);
    }

    /**
     * 条件组合分页查询
     * POST /product/page/query
     */
    @PostMapping("/page/query")
    public Result<PageResult> listProductsByCondition(@RequestBody ProductQuery query) {
        return productService.listProductsByCondition(query);
    }

    /**
     * 更新库存
     */
    @PostMapping("/stock/{id}")
    public Result<String> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        return productService.updateStock(id, stock);
    }

/**
 * 扣减库存（下单时）
 */
    @PostMapping("/stock/decrease/{id}")
    public Result<String> decreaseStock(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.decreaseStock(id, quantity);
    }

    /**
     * 添加库存
     * POST /product/stock/increase/{id}
     */
    @PostMapping("/stock/increase/{id}")
    public Result<Object> increaseStock(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.increaseStock(id, quantity);
    }

    /**
     * 增加销量（支付成功后）
     */
    @PostMapping("/sales/{id}")
    public Result<String> increaseSales(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.increaseSales(id, quantity);
    }

    /**
     * 上架商品
     */
    @PostMapping("/publish/{id}")
    public Result<String> publishProduct(@PathVariable Long id) {
        return productService.publishProduct(id);
    }

    /**
     * 下架商品
     */
    @PostMapping("/unpublish/{id}")
    public Result<String> unpublishProduct(@PathVariable Long id) {
        return productService.unpublishProduct(id);
    }

    /**
     * 校验商品是否存在
     */
    @GetMapping("/check/{id}")
    public Result<Boolean> checkProductExists(@PathVariable Long id) {
        return productService.checkProductExists(id);
    }

    /**
     * 校验库存是否充足
     */
    @GetMapping("/checkstock/{id}")
    public Result<Boolean> checkStockEnough(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.checkStockEnough(id, quantity);
    }

    /**
     * 上传商品主图
     * POST /product/upload/image
     */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return productService.uploadImage(file);
    }


}