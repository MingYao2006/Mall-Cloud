package com.mall.service;

import com.mall.common.Result;
import com.mall.entity.Category;
import com.mall.entity.Product;
import com.mall.feign.CategoryServiceFeignClient;
import com.mall.feign.ProductServiceFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热服务
 * 
 * 实现 CommandLineRunner 接口，在 Spring Boot 启动时自动执行
 * 不需要contoller调用
 * 
 * 作用：
 * 1. 系统启动时预加载热点数据到缓存
 * 2. 避免系统刚启动时大量请求打到数据库
 * 3. 防止缓存雪崩
 */
@Service
public class CacheWarmupService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);

    @Autowired
    private ProductServiceFeignClient productService;

    @Autowired
    private CategoryServiceFeignClient categoryService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheAvalancheService cacheAvalancheService;

    private static final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        logger.info("========== 开始缓存预热 ==========");
        
        try {
            // 1. 预热商品数据
            warmupProducts();
            
            // 2. 预热分类数据
            warmupCategories();
            
            logger.info("========== 缓存预热完成 ==========");
        } catch (Exception e) {
            logger.error("缓存预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热商品数据
     */
    private void warmupProducts() {
        logger.info("开始预热商品数据...");
        
        try {
            // 查询所有上架商品
            Result<List<Product>> result = productService.findAll();
            List<Product> products = result != null ? result.getData() : null;
            
            if (products == null || products.isEmpty()) {
                logger.warn("没有商品数据需要预热");
                return;
            }
            
            int count = 0;
            for (Product product : products) {
                String cacheKey = "product:" + product.getId();
                
                // 使用随机过期时间（30-60分钟）
                int randomExpire = 30 + random.nextInt(31);
                redisTemplate.opsForValue().set(cacheKey, product, randomExpire, TimeUnit.MINUTES);
                
                count++;
            }
            
            logger.info("商品数据预热完成，共预热 {} 条数据", count);
        } catch (Exception e) {
            logger.error("商品数据预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热分类数据
     */
    private void warmupCategories() {
        logger.info("开始预热分类数据...");
        
        try {
            // 查询所有分类
            Result<List<Category>> result = categoryService.listCategories();
            List<Category> categories = result != null ? result.getData() : null;
            
            if (categories == null || categories.isEmpty()) {
                logger.warn("没有分类数据需要预热");
                return;
            }
            
            int count = 0;
            for (com.mall.entity.Category category : categories) {
                String cacheKey = "category:" + category.getId();
                
                // 分类数据变化较少，设置较长的过期时间（1-2小时）
                int randomExpire = 60 + random.nextInt(61);
                redisTemplate.opsForValue().set(cacheKey, category, randomExpire, TimeUnit.MINUTES);
                
                count++;
            }
            
            logger.info("分类数据预热完成，共预热 {} 条数据", count);
        } catch (Exception e) {
            logger.error("分类数据预热失败: {}", e.getMessage(), e);
        }
    }



    /**
     * 手动触发缓存预热
     */
    public void manualWarmup() {
        logger.info("手动触发缓存预热...");
        try {
            warmupProducts();
            warmupCategories();
            logger.info("手动缓存预热完成");
        } catch (Exception e) {
            logger.error("手动缓存预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        logger.info("开始清空所有缓存...");
        try {
            // 清空商品缓存
            redisTemplate.delete(redisTemplate.keys("product:*"));
            
            // 清空分类缓存
            redisTemplate.delete(redisTemplate.keys("category:*"));
            
            logger.info("所有缓存已清空");
        } catch (Exception e) {
            logger.error("清空缓存失败: {}", e.getMessage(), e);
        }
    }
}
