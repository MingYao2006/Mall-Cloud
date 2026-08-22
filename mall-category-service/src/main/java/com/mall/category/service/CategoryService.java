package com.mall.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.category.entity.Category;
import com.mall.category.mapper.CategoryMapper;
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
            List<Category> children = allCategories.stream()
                    .filter(c -> rootCategory.getId().equals(c.getParentId()))
                    .collect(Collectors.toList());
            // 这里可以递归设置更深层级，简化起见只处理两级
        }
        
        return rootCategories;
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
        redisTemplate.delete("categories:all");
    }

    /**
     * 更新分类
     */
    public void updateCategory(Category category) {
        categoryMapper.updateById(category);
        
        // 清除缓存
        redisTemplate.delete("categories:all");
    }

    /**
     * 删除分类
     */
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
        
        // 清除缓存
        redisTemplate.delete("categories:all");
    }
}