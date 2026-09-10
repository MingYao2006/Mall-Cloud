package com.mall.controller;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.entity.Category;
import com.mall.feign.CategoryServiceFeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类Controller
 */
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
    @GetMapping("/{id}")
    public Result<Category> findById(@PathVariable Long id) {
        Result<Category> result = categoryFeignClient.getCategoryById(id);
        if (result == null || result.getData() == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return result;
    }

    /**
     * 添加分类
     * POST /category
     */
    @PostMapping
    public Result<String> add(@RequestBody Category category) {
        return categoryFeignClient.addCategory(category);
    }

    /**
     * 更新分类
     * PUT /category
     */
    @PutMapping
    public Result<String> update(@RequestBody Category category) {
        return categoryFeignClient.updateCategory(category);
    }

    /**
     * 删除分类
     * DELETE /category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Long id) {
        return categoryFeignClient.deleteCategory(id);
    }

    /**
     * 查询子分类
     * GET /category/children/{parentId}
     */
    @GetMapping("/children/{parentId}")
    public Result<List<Category>> findByParentId(@PathVariable Long parentId) {
        return categoryFeignClient.findByParentId(parentId);
    }

    /**
     * 按层级查询分类
     * GET /category/level/{level}
     */
    @GetMapping("/level/{level}")
    public Result<List<Category>> findByLevel(@PathVariable Integer level) {
        return categoryFeignClient.findByLevel(level);
    }

    /**
     * 查询所有分类
     * GET /category/list
     */
    @GetMapping("/list")
    public Result<List<Category>> findAll() {
        return categoryFeignClient.listCategories();
    }

    /**
     * 更新分类状态
     * PUT /category/status
     */
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return categoryFeignClient.updateStatus(id, status);
    }
}
