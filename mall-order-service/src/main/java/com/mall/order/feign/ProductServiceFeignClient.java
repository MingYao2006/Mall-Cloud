package com.mall.order.feign;

import com.mall.common.Result;
import com.mall.order.entity.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "mall-product-service",
        contextId = "productServiceFeignClient",
        path = "/product"
)
public interface ProductServiceFeignClient {
    
    /**
     * 根据 ID 查询商品
     */
    @GetMapping("/{id}")
    Result<Product> findById(@PathVariable("id") Long id);

    /**
     * 获取所有商品列表
     */
    @GetMapping("/list")
    Result<List<Product>> findAll();

    /**
     * 根据分类ID查询商品列表
     */
    @GetMapping("/category/{categoryId}")
    Result<List<Product>> listProductsByCategory(@PathVariable("categoryId") Long categoryId);

    /**
     * 根据状态查询商品列表
     */
    @GetMapping("/status/{status}")
    Result<List<Product>> listProductsByStatus(@PathVariable("status") Integer status);

    /**
     * 搜索商品（名称模糊查询）
     */
    @GetMapping("/search")
    Result<List<Product>> searchProducts(@RequestParam("keyword") String keyword);

    /**
     * 校验商品是否存在
     */
    @GetMapping("/check/{id}")
    Result<Boolean> checkProductExists(@PathVariable("id") Long id);

    /**
     * 校验库存是否充足
     */
    @GetMapping("/checkstock/{id}")
    Result<Boolean> checkStockEnough(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 扣减库存（下单时）
     */
    @PostMapping("/stock/decrease/{id}")
    Result<String> decreaseStock(
            @PathVariable("id") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 增加库存
     */
    @PostMapping("/stock/increase/{id}")
    Result<String> increaseStock(
            @PathVariable("id") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 更新库存
     */
    @PostMapping("/stock/{id}")
    Result<String> updateStock(
            @PathVariable("id") Long productId,
            @RequestParam("stock") Integer stock
    );

    /**
     * 增加销量（支付成功后）
     */
    @PostMapping("/sales/{id}")
    Result<String> increaseSales(
            @PathVariable("id") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 上架商品
     */
    @PostMapping("/publish/{id}")
    Result<String> publishProduct(@PathVariable("id") Long productId);

    /**
     * 下架商品
     */
    @PostMapping("/unpublish/{id}")
    Result<String> unpublishProduct(@PathVariable("id") Long productId);
}