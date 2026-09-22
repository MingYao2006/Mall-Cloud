package com.mall.admin.controller;

import com.mall.common.Result;
import com.mall.entity.EsProduct;
import com.mall.feign.SearchServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索管理控制器（B端）
 * <p>
 * 提供 ES 数据全量导入等管理接口。
 */
@Tag(name = "搜索管理", description = "搜索管理接口（管理端）")
@RestController
@RequestMapping("/search")
public class AdminSearchController {

    private final SearchServiceFeignClient searchService;

    public AdminSearchController(SearchServiceFeignClient searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "全量导入商品数据到ES")
    @PostMapping("/import")
    public Result<Integer> importAll() {
        return searchService.importAll();
    }

    @Operation(summary = "搜索商品（管理端验证用）")
    @GetMapping("/products")
    public Result<List<EsProduct>> searchProducts(@RequestParam String keyword) {
        return searchService.searchProducts(keyword);
    }
}
