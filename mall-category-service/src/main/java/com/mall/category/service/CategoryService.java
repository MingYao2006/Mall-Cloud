package com.mall.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.category.entity.Category;
import com.mall.category.mapper.CategoryMapper;
import com.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取所有分类（管理端用，包含禁用分类）
     */
    public List<Category> getAllCategoriesForAdmin() {
        String cacheKey = "categories:admin:all";

        // 先从缓存获取
        List<Category> categories = (List<Category>) redisTemplate.opsForValue().get(cacheKey);
        if (categories != null) {
            return categories;
        }

        // 从数据库查询（不按 status 过滤，包含禁用分类）
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);

        categories = categoryMapper.selectList(queryWrapper);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, categories, 1, TimeUnit.HOURS);

        return categories;
    }

    /**
     * 获取所有分类
     */
    public List<Category> getAllCategories() {
        String cacheKey = "categories:all";

        // 先从缓存获取
        List<Category> categories = (List<Category>) redisTemplate.opsForValue().get(cacheKey);
        if (categories != null) {
            return categories;
        }

        // 从数据库查询
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort);

        categories = categoryMapper.selectList(queryWrapper);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, categories, 1, TimeUnit.HOURS);

        return categories;
    }

    /**
     * 获取分类树结构
     */
    public List<Category> getCategoryTree() {
        List<Category> allCategories = getAllCategories();

        // 找出所有一级分类
        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        // 为每个一级分类设置子分类
        for (Category rootCategory : rootCategories) {
            buildChildren(rootCategory, allCategories);
            // TODO 这里可以递归设置更深层级，简化起见只处理两级
        }

        return rootCategories;
    }

    /**
     * 递归组装子分类
     */
    private void buildChildren(Category parent, List<Category> allCategories) {
        List<Category> children = allCategories.stream().filter(c -> parent.getId().equals(c.getParentId()))
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (Category child : children) {
            buildChildren(child, allCategories);
        }
    }

    /**
     * 根据ID获取分类
     */
    public Category getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 添加分类
     */
    public void addCategory(Category category) {
        categoryMapper.insert(category);

        // 清除缓存
        clearCategoryCache();
    }

    /**
     * 更新分类
     */
    public void updateCategory(Category category) {
        categoryMapper.updateById(category);

        // 清除缓存
        clearCategoryCache();
    }

    /**
     * 删除分类
     */
    public void deleteCategory(Long id) {
        // 先校验分类是否存在且未被逻辑删除（selectById 会因 @TableLogic 自动过滤已删除记录）
        if (categoryMapper.selectById(id) == null) {
            throw new IllegalArgumentException("分类不存在或已被删除");
        }

        // 删除前校验是否存在子分类，避免产生孤儿数据
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, id);
        Long childCount = categoryMapper.selectCount(wrapper);
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("该分类下存在子分类，无法删除");
        }

        int rows = categoryMapper.deleteById(id);
        if (rows == 0) {
            throw new IllegalArgumentException("分类不存在或已被删除");
        }

        // 清除缓存
        clearCategoryCache();
    }

    /**
     * 根据父分类ID查询子分类
     */
    public List<Category> findByParentId(Long parentId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort);
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 根据层级查询分类
     */
    public List<Category> findByLevel(Integer level) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getLevel, level)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort);
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 更新分类状态
     */
    public void updateStatus(Long id, Integer status) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        category.setStatus(status);
        categoryMapper.updateById(category);

        // 清除缓存
        clearCategoryCache();
    }

    /**
     * 清除分类相关缓存（包含前端和管理端）
     */
    private void clearCategoryCache() {
        redisTemplate.delete("categories:all");
        redisTemplate.delete("categories:admin:all");
    }
}