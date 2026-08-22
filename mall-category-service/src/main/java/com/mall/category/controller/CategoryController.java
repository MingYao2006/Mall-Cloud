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
}