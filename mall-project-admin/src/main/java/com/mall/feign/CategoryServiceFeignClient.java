package com.mall.feign;

import com.mall.common.Result;
import com.mall.entity.Category;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-category-service 微服务，
 * 包含分类的增删改、状态更新等管理接口。
 */
@FeignClient(
        name = "mall-category-service",
        contextId = "categoryServiceFeignClient",
        path = "/category"
)
public interface CategoryServiceFeignClient {

    @GetMapping("/{id}")
    Result<Category> getCategoryById(@PathVariable("id") Long id);

    @PostMapping
    Result<String> addCategory(@RequestBody Category category);

    @PutMapping
    Result<String> updateCategory(@RequestBody Category category);

    @DeleteMapping("/{id}")
    Result<String> deleteCategory(@PathVariable("id") Long id);

    @GetMapping("/children/{parentId}")
    Result<List<Category>> findByParentId(@PathVariable("parentId") Long parentId);

    @GetMapping("/level/{level}")
    Result<List<Category>> findByLevel(@PathVariable("level") Integer level);

    @GetMapping("/admin/list")
    Result<List<Category>> listCategoriesForAdmin();

    @GetMapping("/list")
    Result<List<Category>> listCategories();

    @PutMapping("/status")
    Result<String> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);
}
