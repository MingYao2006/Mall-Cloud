package com.mall.feign;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.query.ProductQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-product-service 微服务，
 * 包含全部管理端所需的商品操作接口（增删改、上下架、库存、图片上传等）。
 */
@FeignClient(
        name = "mall-product-service",
        contextId = "productServiceFeignClient",
        path = "/product"
)
public interface ProductServiceFeignClient {

    @PostMapping
    Result<Object> add(@RequestBody Product product);

    @PutMapping
    Result<Object> update(@RequestBody Product product);

    @DeleteMapping("/{id}")
    Result<Object> delete(@PathVariable("id") Long id);

    @GetMapping("/{id}")
    Result<Product> findById(@PathVariable("id") Long id);

    @GetMapping("/list")
    Result<List<Product>> findAll();

    @GetMapping("/category/{categoryId}")
    Result<List<Product>> listProductsByCategory(@PathVariable("categoryId") Long categoryId);

    @GetMapping("/status/{status}")
    Result<List<Product>> listProductsByStatus(@PathVariable("status") Integer status);

    @GetMapping("/search")
    Result<List<Product>> searchProducts(@RequestParam("keyword") String keyword);

    @GetMapping("/page")
    Result<PageResult> listProductsByPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    @PostMapping("/page/query")
    Result<PageResult> listProductsByCondition(@RequestBody ProductQuery query);

    @PostMapping("/stock/{id}")
    Result<String> updateStock(@PathVariable("id") Long productId, @RequestParam("stock") Integer stock);

    @PostMapping("/stock/decrease/{id}")
    Result<String> decreaseStock(@PathVariable("id") Long productId, @RequestParam("quantity") Integer quantity);

    @PostMapping("/stock/increase/{id}")
    Result<Object> increaseStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PostMapping("/sales/{id}")
    Result<String> increaseSales(@PathVariable("id") Long productId, @RequestParam("quantity") Integer quantity);

    @PostMapping("/publish/{id}")
    Result<String> publishProduct(@PathVariable("id") Long productId);

    @PostMapping("/unpublish/{id}")
    Result<String> unpublishProduct(@PathVariable("id") Long productId);

    @GetMapping("/check/{id}")
    Result<Boolean> checkProductExists(@PathVariable("id") Long id);

    @GetMapping("/checkstock/{id}")
    Result<Boolean> checkStockEnough(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> uploadImage(@RequestPart("file") MultipartFile file);
}
