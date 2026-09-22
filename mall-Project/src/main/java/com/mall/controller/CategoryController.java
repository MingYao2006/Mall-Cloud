package com.mall.controller;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.entity.Category;
import com.mall.feign.CategoryServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类Controller - C端聚合服务（客户端版）
 * <p>
 * 仅提供客户端所需的分类浏览、查询等只读接口。
 * 分类的增删改、状态更新等管理接口已迁移至 mall-project-admin。
 */
@Tag(name = "分类服务", description = "分类浏览接口（客户端聚合服务代理）")
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryServiceFeignClient categoryFeignClient;

    public CategoryController(CategoryServiceFeignClient categoryFeignClient) {
        this.categoryFeignClient = categoryFeignClient;
    }

    /**
     * 查询分类详情
     * GET /category/{id}
     */
    @Operation(summary = "查询分类详情")
    @GetMapping("/{id}")
    public Result<Category> findById(@PathVariable Long id) {
        Result<Category> result = categoryFeignClient.getCategoryById(id);
        if (result == null || result.getData() == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return result;
    }

    /**
     * 查询子分类
     * GET /category/children/{parentId}
     */
    @Operation(summary = "查询子分类")
    @GetMapping("/children/{parentId}")
    public Result<List<Category>> findByParentId(@PathVariable Long parentId) {
        return categoryFeignClient.findByParentId(parentId);
    }

    /**
     * 按层级查询分类
     * GET /category/level/{level}
     */
    @Operation(summary = "按层级查询分类")
    @GetMapping("/level/{level}")
    public Result<List<Category>> findByLevel(@PathVariable Integer level) {
        return categoryFeignClient.findByLevel(level);
    }

    /**
     * 查询所有分类
     * GET /category/list
     */
    @Operation(summary = "查询所有分类")
    @GetMapping("/list")
    public Result<List<Category>> findAll() {
        return categoryFeignClient.listCategories();
    }

    // ===== 以下管理端接口已迁移至 mall-project-admin =====
    // POST   /category        → AdminCategoryController.add
    // PUT    /category        → AdminCategoryController.update
    // DELETE /category/{id}   → AdminCategoryController.deleteById
    // PUT    /category/status → AdminCategoryController.updateStatus
}
