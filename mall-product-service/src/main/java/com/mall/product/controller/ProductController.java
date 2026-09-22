package com.mall.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.product.entity.Product;
import com.mall.product.entity.query.ProductQuery;
import com.mall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "商品服务", description = "商品相关接口")
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {

        this.productService = productService;
    }

    /**
     * 测试接口
     */
    @Operation(summary = "测试接口")
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("商品服务接口正常工作");
    }

    /**
     * 添加商品
     * POST /product
     */
    @Operation(summary = "添加商品")
    @PostMapping
    public Result<Object> add(@RequestBody @Valid Product product) {
        productService.add(product);
        return Result.success("添加成功");
    }

    /**
     * 更新商品
     * PUT /product
     */
    @Operation(summary = "更新商品")
    @PutMapping
    public Result<Object> update(@RequestBody @Valid Product product) {
        productService.update(product);
        return Result.success("更新成功");
    }

    /**
     * 删除商品
     * DELETE /product/{id}
     */
    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 根据ID查询商品详情
     * GET /product/{id}
     */
    @Operation(summary = "根据ID查询商品详情")
    @GetMapping("/{id}")
    @SentinelResource(
            value = "getProductDetail",
            blockHandler = "handleGetProductBlock"
    )
    public Result<Product> findById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return Result.success(product);
    }

    /**
     * 获取所有商品列表
     * GET /product/list
     */
    @Operation(summary = "获取所有商品列表")
    @GetMapping("/list")
    @SentinelResource(
            value = "listAllProducts",
            blockHandler = "handleListProductsBlock"
    )
    public Result<List<Product>> findAll() {
        List<Product> products = productService.findAll();
        return Result.success(products);
    }

    /**
     * 根据分类ID查询商品列表
     * GET /product/category/{categoryId}
     */
    @Operation(summary = "根据分类ID查询商品列表")
    @GetMapping("/category/{categoryId}")
    @SentinelResource(
            value = "listProductsByCategory",
            blockHandler = "handleListProductsBlock"
    )
    public Result<List<Product>> listProductsByCategory(@PathVariable Long categoryId) {
        List<Product> products = productService.listProductsByCategory(categoryId);
        return Result.success(products);
    }

    /**
     * 根据状态查询商品列表
     * GET /product/status/{status}
     */
    @Operation(summary = "根据状态查询商品列表")
    @GetMapping("/status/{status}")
    public Result<List<Product>> listProductsByStatus(@PathVariable Integer status) {
        List<Product> products = productService.listProductsByStatus(status);
        return Result.success(products);
    }

    /**
     * 搜索商品（名称模糊查询）
     * GET /product/search?keyword=xxx
     */
    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    @SentinelResource(
            value = "searchProducts",
            blockHandler = "handleSearchProductsBlock"
    )
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return Result.success(products);
    }

    /**
     * 分页查询商品
     * GET /product/page?pageNum=1&pageSize=10
     */
    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    @SentinelResource(
            value = "listProductsByPage",
            blockHandler = "handlePageProductsBlock"
    )
    public Result<PageResult> listProductsByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult pageResult = productService.listProductsByPage(pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 条件组合分页查询
     * POST /product/page/query
     */
    @Operation(summary = "条件组合分页查询")
    @PostMapping("/page/query")
    public Result<PageResult> listProductsByCondition(@RequestBody ProductQuery query) {
        PageResult pageResult = productService.listProductsByCondition(query);
        return Result.success(pageResult);
    }

    /**
     * 更新库存
     */
    @Operation(summary = "更新库存")
    @PostMapping("/stock/{id}")
    public Result<Object> updateStock(@PathVariable Long id,@RequestParam Integer stock) {
        productService.updateStock(id, stock);
        return Result.success("更新库存成功");
    }

/**
 * 扣减库存（下单时）
 */
    @Operation(summary = "扣减库存")
    @PostMapping("/stock/decrease/{id}")
    public Result<Object> decreaseStock(@PathVariable Long id,@RequestParam Integer quantity) {
        productService.decreaseStock(id, quantity);
        return Result.success("扣减库存成功");
    }

    /**
     * 添加库存
     * POST /product/stock/increase/{id}
     */
    @Operation(summary = "添加库存")
    @PostMapping("/stock/increase/{id}")
    public Result<Object> increaseStock(@PathVariable Long id,@RequestParam Integer quantity) {
        productService.increaseStock(id, quantity);
        return Result.success("添加库存成功");
    }

    /**
     * 增加销量（支付成功后）
     */
    @Operation(summary = "增加销量")
    @PostMapping("/sales/{id}")
    public Result<Object> increaseSales(@PathVariable Long id,@RequestParam Integer quantity) {
        productService.increaseSales(id, quantity);
        return Result.success("添加销量成功");
    }

    /**
     * 上架商品
     */
    @Operation(summary = "上架商品")
    @PostMapping("/publish/{id}")
    public Result<Object> publishProduct(@PathVariable Long id) {
        productService.publishProduct(id);
        return Result.success("上架商品成功");
    }

    /**
     * 下架商品
     */
    @Operation(summary = "下架商品")
    @PostMapping("/unpublish/{id}")
    public Result<Object> unpublishProduct(@PathVariable Long id) {
        productService.unpublishProduct(id);
        return Result.success("下架商品成功");
    }

    /**
     * 校验商品是否存在
     */
    @Operation(summary = "校验商品是否存在")
    @GetMapping("/check/{id}")
    public Result<Object> checkProductExists(@PathVariable Long id) {
        boolean exists = productService.checkProductExists(id);
        return Result.success(exists);
    }

    /**
     * 校验库存是否充足
     */
    @Operation(summary = "校验库存是否充足")
    @GetMapping("/checkstock/{id}")
    public Result<Object> checkStockEnough(@PathVariable Long id,@RequestParam Integer quantity) {
        boolean enough = productService.checkStockEnough(id, quantity);
        return Result.success(enough);
    }

    /**
     * 上传商品主图
     * POST /product/upload/image
     */
    @Operation(summary = "上传商品主图")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestPart("file") MultipartFile file) {
        return productService.uploadMainImage(file);
    }
    /**
     * 限流处理方法
     * */
    public Result<Product> handleGetProductBlock(Long id, BlockException ex) {
        // 返回默认商品信息
        Product defaultProduct = new Product();
        defaultProduct.setId(id);
        defaultProduct.setName("商品信息加载中，请稍后再试");
        return Result.success(defaultProduct);
    }

    /**
     * 商品列表限流处理方法
     * */
    public Result<List<Product>> handleListProductsBlock(BlockException ex) {
        return Result.fail("系统繁忙，商品列表暂时不可用");
    }

    /**
     * 商品搜索限流处理方法
     * */
    public Result<List<Product>> handleSearchProductsBlock(String keyword, BlockException ex) {
        return Result.fail("搜索请求过多，请稍后再试");
    }

    /**
     * 商品分页查询限流处理方法
     * */
    public Result<PageResult> handlePageProductsBlock(Integer pageNum, Integer pageSize, BlockException ex) {
        return Result.fail("系统繁忙，请稍后再试");
    }
}