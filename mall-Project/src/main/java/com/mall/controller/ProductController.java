package com.mall.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.query.ProductQuery;
import com.mall.service.ProductService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {

        this.productService = productService;
    }

    /**
     * 添加商品
     * POST /product
     */
    @PostMapping
    public Result<Object> add(@RequestBody Product product) {
        productService.add(product);
        return Result.success("添加成功");
    }

    /**
     * 更新商品
     * PUT /product
     */
    @PutMapping
    public Result<Object> update(@RequestBody Product product) {
        productService.update(product);
        return Result.success("更新成功");
    }

    /**
     * 删除商品
     * DELETE /product/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 根据ID查询商品详情
     * GET /product/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> findById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return Result.success(product);
    }

    /**
     * 获取所有商品列表
     * GET /product/list
     */
    @GetMapping("/list")
    public Result<List<Product>> findAll() {
        List<Product> products = productService.findAll();
        return Result.success(products);
    }

    /**
     * 根据分类ID查询商品列表
     * GET /product/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public Result<List<Product>> listProductsByCategory(@PathVariable Long categoryId) {
        List<Product> products = productService.listProductsByCategory(categoryId);
        return Result.success(products);
    }

    /**
     * 根据状态查询商品列表
     * GET /product/status/{status}
     */
    @GetMapping("/status/{status}")
    public Result<List<Product>> listProductsByStatus(@PathVariable Integer status) {
        List<Product> products = productService.listProductsByStatus(status);
        return Result.success(products);
    }

    /**
     * 搜索商品（名称模糊查询）
     * GET /product/search?keyword=xxx
     */
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return Result.success(products);
    }

    /**
     * 分页查询商品
     * GET /product/page?pageNum=1&pageSize=10
     */
    @GetMapping("/page")
    public Result<PageResult> listProductsByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult pageResult = productService.listProductsByPage(pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 条件组合分页查询
     * GET /product/page/query?categoryId=1&name=手机&status=1&minPrice=100&maxPrice=10000&pageNum=1&pageSize=10
     */
    @GetMapping("/page/query")
    public Result<PageResult> listProductsByCondition(ProductQuery query) {
        PageResult pageResult = productService.listProductsByCondition(query);
        return Result.success(pageResult);
    }

    /**
     * 更新库存
     */
    @PostMapping("/stock/{id}")
    public Result<Object> updateStock(@PathVariable Long id,@RequestParam Integer stock) {
        productService.updateStock(id, stock);
        return Result.success("更新库存成功");
    }

/**
 * 扣减库存（下单时）
 */
    @PostMapping("/stock/decrease/{id}")
    public Result<Object> decreaseStock(@PathVariable Long id,@RequestParam Integer quantity) {
        productService.decreaseStock(id, quantity);
        return Result.success("扣减库存成功");
    }

    /**
     * 添加库存
     * POST /product/stock/increase/{id}
     */
    @PostMapping("/stock/increase/{id}")
    public Result<Object> increaseStock(@PathVariable Long id,@RequestParam Integer quantity) {
        productService.increaseStock(id, quantity);
        return Result.success("添加库存成功");
    }

    /**
     * 增加销量（支付成功后）
     */
    @PostMapping("/sales/{id}")
    public Result<Object> increaseSales(@PathVariable Long id,@RequestParam Integer quantity) {
        productService.increaseSales(id, quantity);
        return Result.success("添加销量成功");
    }

    /**
     * 上架商品
     */
    @PostMapping("/publish/{id}")
    public Result<Object> publishProduct(@PathVariable Long id) {
        productService.publishProduct(id);
        return Result.success("上架商品成功");
    }

    /**
     * 下架商品
     */
    @PostMapping("/unpublish/{id}")
    public Result<Object> unpublishProduct(@PathVariable Long id) {
        productService.unpublishProduct(id);
        return Result.success("下架商品成功");
    }

    /**
     * 校验商品是否存在
     */
    @GetMapping("/check/{id}")
    public Result<Object> checkProductExists(@PathVariable Long id) {
        boolean exists = productService.checkProductExists(id);
        return Result.success(exists);
    }

    /**
     * 校验库存是否充足
     */
    @GetMapping("/checkstock/{id}")
    public Result<Object> checkStockEnough(@PathVariable Long id,@RequestParam Integer quantity) {
        boolean enough = productService.checkStockEnough(id, quantity);
        return Result.success(enough);
    }

    /**
     * 上传商品主图
     * POST /product/upload/image
     */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return productService.uploadMainImage(file);
    }


}