package com.mall.product.feign;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.product.entity.Product;
import com.mall.product.entity.query.ProductQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "mall-project",
        contextId = "productServiceFeignClient",
        path = "/api/product"
)
public interface ProductServiceFeignClient {
    
    /**
     * 根据 ID 查询商品
     * GET /api/product/{id}
     */
    @GetMapping("/{id}")
    Result<Product> findById(@PathVariable("id") Long id);

    /**
     * 获取所有商品列表
     * GET /api/product/list
     */
    @GetMapping("/list")
    Result<List<Product>> findAll();

    /**
     * 根据分类ID查询商品列表
     * GET /api/product/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    Result<List<Product>> listProductsByCategory(@PathVariable("categoryId") Long categoryId);

    /**
     * 根据状态查询商品列表
     * GET /api/product/status/{status}
     */
    @GetMapping("/status/{status}")
    Result<List<Product>> listProductsByStatus(@PathVariable("status") Integer status);

    /**
     * 搜索商品（名称模糊查询）
     * GET /api/product/search?keyword=xxx
     */
    @GetMapping("/search")
    Result<List<Product>> searchProducts(@RequestParam("keyword") String keyword);

    /**
     * 分页查询商品
     * GET /api/product/page?pageNum=1&pageSize=10
     */
    @GetMapping("/page")
    Result<PageResult> listProductsByPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    /**
     * 条件组合分页查询
     * POST /api/product/page/query
     */
    @PostMapping("/page/query")
    Result<PageResult> listProductsByCondition(@RequestBody ProductQuery query);

    /**
     * 校验商品是否存在
     * GET /api/product/check/{id}
     */
    @GetMapping("/check/{id}")
    Result<Boolean> checkProductExists(@PathVariable("id") Long id);

    /**
     * 校验库存是否充足
     * GET /api/product/checkstock/{id}?quantity=1
     */
    @GetMapping("/checkstock/{id}")
    Result<Boolean> checkStockEnough(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 扣减库存（下单时）
     * POST /api/product/stock/decrease/{id}?quantity=2
     */
    @PostMapping("/stock/decrease/{id}")
    Result<String> decreaseStock(
            @PathVariable("id") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 更新库存
     * POST /api/product/stock/{id}?stock=100
     */
    @PostMapping("/stock/{id}")
    Result<String> updateStock(
            @PathVariable("id") Long productId,
            @RequestParam("stock") Integer stock
    );

    /**
     * 增加销量（支付成功后）
     * POST /api/product/sales/{id}?quantity=1
     */
    @PostMapping("/sales/{id}")
    Result<String> increaseSales(
            @PathVariable("id") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 上架商品
     * POST /api/product/publish/{id}
     */
    @PostMapping("/publish/{id}")
    Result<String> publishProduct(@PathVariable("id") Long productId);

    /**
     * 下架商品
     * POST /api/product/unpublish/{id}
     */
    @PostMapping("/unpublish/{id}")
    Result<String> unpublishProduct(@PathVariable("id") Long productId);

    /**
     * 添加库存
     * POST /product/
     */
    @PostMapping("/stock/decrease/{id}")
    Result<Object> increaseStock(@PathVariable Long id,@RequestParam Integer quantity);
}
