package com.mall.category.controller;

import com.mall.common.Result;
import com.mall.category.entity.Category;
import com.mall.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Tag(name = "分类服务", description = "商品分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/all")
    @Operation(summary = "获取所有分类")
    public Result<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树结构")
    public Result<List<Category>> getCategoryTree() {
        List<Category> categories = categoryService.getCategoryTree();
        return Result.success(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取分类")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return Result.success(category);
    }

    @PostMapping
    @Operation(summary = "添加分类")
    public Result<String> addCategory(@RequestBody Category category) {
        categoryService.addCategory(category);
        return Result.success("添加成功");
    }

    @PutMapping
    @Operation(summary = "更新分类")
    public Result<String> updateCategory(@RequestBody Category category) {
        categoryService.updateCategory(category);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有启用分类")
    public Result<List<Category>> listCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    @GetMapping("/children/{parentId}")
    @Operation(summary = "查询子分类")
    public Result<List<Category>> findByParentId(@PathVariable Long parentId) {
        List<Category> categories = categoryService.findByParentId(parentId);
        return Result.success(categories);
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "按层级查询分类")
    public Result<List<Category>> findByLevel(@PathVariable Integer level) {
        List<Category> categories = categoryService.findByLevel(level);
        return Result.success(categories);
    }

    @PutMapping("/status")
    @Operation(summary = "更新分类状态")
    public Result<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
}