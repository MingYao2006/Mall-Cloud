package com.mall.controller;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.entity.Category;
import com.mall.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类Controller
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

//    @Autowired 字段注入：写法简单，适合快速开发
//    构造方法注入：更安全、更规范，适合生产环境
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {

        this.categoryService = categoryService;
    }

    /**
     * 查询分类详情
     * GET /category/{id}
     */
    @GetMapping("/{id}")
    public Result<Category> findById(@PathVariable Long id) {
        Category category = categoryService.findById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return Result.success(category);
    }

    /**
     * 添加分类
     * POST /category
     */
    @PostMapping
    public Result<String> add(@RequestBody Category category) {
        categoryService.add(category);
        return Result.success("添加成功");
    }

    /**
     * 更新分类
     * PUT /category
     */
    @PutMapping
    public Result<String> update(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success("更新成功");
    }

    /**
     * 删除分类
     * DELETE /category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Long id) {
        categoryService.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 查询子分类
     * GET /category/children/{parentId}
     */
    @GetMapping("/children/{parentId}")
    public Result<List<Category>> findByParentId(@PathVariable Long parentId) {
        List<Category> categories = categoryService.findByParentId(parentId);
        return Result.success(categories);
    }

    /**
     * 按层级查询分类
     * GET /category/level/{level}
     */
    @GetMapping("/level/{level}")
    public Result<List<Category>> findByLevel(@PathVariable Integer level) {
        List<Category> categories = categoryService.findByLevel(level);
        return Result.success(categories);
    }

    /**
     * 查询所有分类
     * GET /category/list
     */
    @GetMapping("/list")
    public Result<List<Category>> findAll() {
        List<Category> categories = categoryService.findAll();
        return Result.success(categories);
    }

    /**
     * 更新分类状态
     * PUT /category/status
     */
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
}
