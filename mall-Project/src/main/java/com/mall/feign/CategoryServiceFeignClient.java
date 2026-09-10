package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.Category;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类服务 Feign 客户端
 * 调用 mall-category-service 微服务
 */
@FeignClient(name = "mall-category-service", path = "/category")
public interface CategoryServiceFeignClient {

    /**
     * 根据 ID 查询分类
     * GET /category/{id}
     */
    @GetMapping("/{id}")
    Result<Category> getCategoryById(@PathVariable("id") Long id);

    /**
     * 添加分类
     * POST /category
     */
    @PostMapping
    Result<String> addCategory(@RequestBody Category category);

    /**
     * 更新分类
     * PUT /category
     */
    @PutMapping
    Result<String> updateCategory(@RequestBody Category category);

    /**
     * 删除分类
     * DELETE /category/{id}
     */
    @DeleteMapping("/{id}")
    Result<String> deleteCategory(@PathVariable("id") Long id);

    /**
     * 查询子分类
     * GET /category/children/{parentId}
     */
    @GetMapping("/children/{parentId}")
    Result<List<Category>> findByParentId(@PathVariable("parentId") Long parentId);

    /**
     * 按层级查询分类
     * GET /category/level/{level}
     */
    @GetMapping("/level/{level}")
    Result<List<Category>> findByLevel(@PathVariable("level") Integer level);

    /**
     * 查询所有启用分类
     * GET /category/list
     */
    @GetMapping("/list")
    Result<List<Category>> listCategories();

    /**
     * 更新分类状态
     * PUT /category/status
     */
    @PutMapping("/status")
    Result<String> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);
}
