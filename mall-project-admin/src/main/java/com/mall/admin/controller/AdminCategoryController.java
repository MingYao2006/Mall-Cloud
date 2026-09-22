package com.mall.admin.controller;

import com.mall.common.Result;
import com.mall.entity.Category;
import com.mall.feign.CategoryServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器（B端）
 * <p>
 * 提供分类的增删改、状态更新等管理接口。
 */
@Tag(name = "分类管理", description = "分类管理接口（管理端）")
@RestController
@RequestMapping("/category")
public class AdminCategoryController {

    private final CategoryServiceFeignClient categoryFeignClient;

    public AdminCategoryController(CategoryServiceFeignClient categoryFeignClient) {
        this.categoryFeignClient = categoryFeignClient;
    }

    @Operation(summary = "查询分类详情")
    @GetMapping("/{id}")
    public Result<Category> findById(@PathVariable Long id) {
        return categoryFeignClient.getCategoryById(id);
    }

    @Operation(summary = "添加分类")
    @PostMapping
    public Result<String> add(@RequestBody Category category) {
        return categoryFeignClient.addCategory(category);
    }

    @Operation(summary = "更新分类")
    @PutMapping
    public Result<String> update(@RequestBody Category category) {
        return categoryFeignClient.updateCategory(category);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Long id) {
        return categoryFeignClient.deleteCategory(id);
    }

    @Operation(summary = "查询子分类")
    @GetMapping("/children/{parentId}")
    public Result<List<Category>> findByParentId(@PathVariable Long parentId) {
        return categoryFeignClient.findByParentId(parentId);
    }

    @Operation(summary = "按层级查询分类")
    @GetMapping("/level/{level}")
    public Result<List<Category>> findByLevel(@PathVariable Integer level) {
        return categoryFeignClient.findByLevel(level);
    }

    @Operation(summary = "查询所有分类（含禁用）")
    @GetMapping("/list")
    public Result<List<Category>> findAll() {
        return categoryFeignClient.listCategoriesForAdmin();
    }

    @Operation(summary = "更新分类状态")
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return categoryFeignClient.updateStatus(id, status);
    }
}
