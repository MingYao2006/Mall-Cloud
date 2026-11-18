package com.mall.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.SeckillPriceUtil;
import com.mall.entity.Product;
import com.mall.entity.query.ProductQuery;
import com.mall.feign.ProductServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品Controller - C端聚合服务（客户端版）
 * <p>
 * 仅提供客户端所需的商品浏览、搜索等只读接口。
 * 商品的增删改、上下架、库存管理、图片上传等管理接口已迁移至 mall-project-admin。
 */
@Tag(name = "商品服务", description = "商品浏览接口（客户端聚合服务代理）")
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductServiceFeignClient productService;

    public ProductController(ProductServiceFeignClient productService) {
        this.productService = productService;
    }

    /**
     * 根据ID查询商品详情
     * GET /product/{id}
     */
    @Operation(summary = "根据ID查询商品详情")
    @GetMapping("/{id}")
    public Result<Product> findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    /**
     * 获取所有商品列表
     * GET /product/list
     */
    @Operation(summary = "获取所有商品列表")
    @GetMapping("/list")
    public Result<List<Product>> findAll() {
        return productService.findAll();
    }

    /**
     * 根据分类ID查询商品列表
     * GET /product/category/{categoryId}
     */
    @Operation(summary = "根据分类ID查询商品列表")
    @GetMapping("/category/{categoryId}")
    public Result<List<Product>> listProductsByCategory(@PathVariable Long categoryId) {
        return productService.listProductsByCategory(categoryId);
    }

    /**
     * 根据状态查询商品列表
     * GET /product/status/{status}
     */
    @Operation(summary = "根据状态查询商品列表")
    @GetMapping("/status/{status}")
    public Result<List<Product>> listProductsByStatus(@PathVariable Integer status) {
        return productService.listProductsByStatus(status);
    }

    /**
     * 搜索商品（名称模糊查询）
     * GET /product/search?keyword=xxx
     */
    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    /**
     * 分页查询商品
     * GET /product/page?pageNum=1&pageSize=10
     */
    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    public Result<PageResult> listProductsByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return productService.listProductsByPage(pageNum, pageSize);
    }

    /**
     * 条件组合分页查询
     * POST /product/page/query
     *
     * C 端商品列表页的筛选（分类 / 价格区间 / 排序）依赖这个接口。
     * 网关白名单已放行 /api/c/product/page/query。
     */
    @Operation(summary = "条件组合分页查询")
    @PostMapping("/page/query")
    public Result<PageResult> listProductsByCondition(@RequestBody ProductQuery query) {
        return productService.listProductsByCondition(query);
    }

    /**
     * 秒杀商品列表（权威定价）
     *
     * <p>秒杀价由 {@link SeckillPriceUtil} 按商品 ID 稳定计算，前端只负责展示，
     * 下单时也由订单服务用同一规则重算，保证「页面价 = 订单商品价 = 实付金额」。
     * 返回列表结构与 /product/page 一致，额外给每条附带 seckillPrice 字段。
     */
    @Operation(summary = "秒杀商品列表（含权威秒杀价）")
    @GetMapping("/seckill")
    public Result<PageResult> listSeckillProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        Result<PageResult> pageResult = productService.listProductsByPage(pageNum, pageSize);
        PageResult page = pageResult == null ? null : pageResult.getData();
        if (page == null || page.getList() == null) {
            return pageResult == null ? Result.success(new PageResult(pageNum, pageSize, 0L, new ArrayList<>())) : pageResult;
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Object obj : page.getList()) {
            // Feign 反序列化后元素可能是 Product，也可能是 LinkedHashMap（泛型丢失），两种都要兼容
            Long id;
            BigDecimal price;
            Map<String, Object> row = new LinkedHashMap<>();
            if (obj instanceof Product) {
                Product p = (Product) obj;
                id = p.getId();
                price = p.getPrice();
                row.put("id", p.getId());
                row.put("name", p.getName());
                row.put("price", p.getPrice());
                row.put("originalPrice", p.getOriginalPrice() != null ? p.getOriginalPrice() : p.getPrice());
                row.put("mainImage", p.getMainImage());
                row.put("brand", p.getBrand());
                row.put("categoryId", p.getCategoryId());
                row.put("status", p.getStatus());
                row.put("stock", p.getStock());
                row.put("sales", p.getSales());
            } else if (obj instanceof Map) {
                Map<?, ?> src = (Map<?, ?>) obj;
                row.putAll((Map<String, Object>) src);
                Object idVal = src.get("id");
                Object priceVal = src.get("price");
                id = idVal instanceof Number ? ((Number) idVal).longValue() : null;
                price = priceVal instanceof Number
                        ? BigDecimal.valueOf(((Number) priceVal).doubleValue())
                        : (priceVal != null ? new BigDecimal(priceVal.toString()) : null);
                // 原价字段统一补齐，前端划线价用得到
                if (!row.containsKey("originalPrice")) {
                    row.put("originalPrice", src.get("originalPrice") != null ? src.get("originalPrice") : priceVal);
                }
            } else {
                continue;
            }
            if (id == null || price == null) {
                continue;
            }
            row.put("seckillPrice", SeckillPriceUtil.compute(id, price));
            enriched.add(row);
        }

        PageResult out = new PageResult(pageNum, pageSize, page.getTotal(), enriched);
        return Result.success(out);
    }

    /**
     * 校验商品是否存在（下单时内部调用）
     * GET /product/check/{id}
     */
    @Operation(summary = "校验商品是否存在")
    @GetMapping("/check/{id}")
    public Result<Boolean> checkProductExists(@PathVariable Long id) {
        return productService.checkProductExists(id);
    }

    /**
     * 校验库存是否充足（下单时内部调用）
     * GET /product/checkstock/{id}
     */
    @Operation(summary = "校验库存是否充足")
    @GetMapping("/checkstock/{id}")
    public Result<Boolean> checkStockEnough(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.checkStockEnough(id, quantity);
    }

    // ===== 以下管理端接口已迁移至 mall-project-admin =====
    // POST   /product           → AdminProductController.add
    // PUT    /product           → AdminProductController.update
    // DELETE /product/{id}      → AdminProductController.delete
    // POST   /product/publish/{id}   → AdminProductController.publishProduct
    // POST   /product/unpublish/{id} → AdminProductController.unpublishProduct
    // POST   /product/stock/{id}     → AdminProductController.updateStock
    // POST   /product/stock/increase/{id}  → AdminProductController.increaseStock
    // POST   /product/upload/image  → AdminProductController.uploadImage
    // POST   /product/page/query    → AdminProductController.listProductsByCondition

    /**
     * 扣减库存（下单时内部调用，C端保留）
     */
    @Operation(summary = "扣减库存")
    @PostMapping("/stock/decrease/{id}")
    public Result<String> decreaseStock(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.decreaseStock(id, quantity);
    }

    /**
     * 增加销量（支付成功后内部调用，C端保留）
     */
    @Operation(summary = "增加销量")
    @PostMapping("/sales/{id}")
    public Result<String> increaseSales(@PathVariable Long id, @RequestParam Integer quantity) {
        return productService.increaseSales(id, quantity);
    }
}