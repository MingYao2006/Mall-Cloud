package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.RedisService;
import com.mall.common.Result;
import com.mall.product.entity.Product;
import com.mall.product.entity.query.ProductQuery;
import com.mall.product.feign.ProductServiceFeignClient;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.ProductMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品Service - 使用 MyBatis Plus 重构
 * <p>
 * MyBatis Plus 优化点：
 * 1. 使用 LambdaQueryWrapper 替代手写 SQL，类型安全
 * 2. 使用 Page 对象替代 PageHelper，内置分页
 * 3. 使用 LambdaUpdateWrapper 实现字段级更新
 * 4. 继承 BaseMapper 后自动拥有 CRUD 方法
 */
@Slf4j
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    @Autowired
    private ProductServiceFeignClient productServiceFeignClient;
    @Autowired
    private OssService ossService;

    public ProductService(ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }


    @Resource//缓存击穿
    private RedissonClient redissonClient;

    @Autowired//获取和存入缓存
    private RedisService redisService;

    @Autowired//缓存穿透
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_CACHE_KEY = "product:";
    private static final long CACHE_TIMEOUT = 30; // 30分钟基础过期时间
    private static final int RANDOM_EXPIRE_MIN = 10; // 随机过期时间最小值（分钟）
    private static final int RANDOM_EXPIRE_MAX = 40; // 随机过期时间最大值（分钟）

    /**
     * 添加商品
     * 使用 MyBatis Plus 的 insert 方法
     */
    public void add(Product product) {
        // 1. 校验商品名称
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new BusinessException(400, "商品名称不能为空");
        }

        // 2. 校验分类是否存在
        if (product.getCategoryId() == null || categoryMapper.selectById(product.getCategoryId()) == null) {
            throw new BusinessException(400, "商品分类不存在");
        }

        // 3. 校验价格
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "商品价格不能为空且不能为负数");
        }

        // 4. 校验原价
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "商品原价不能为负数");
        }
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(product.getPrice()) < 0) {
            throw new BusinessException(400, "商品原价不能小于售价");
        }

        // 5. 校验库存
        if (product.getStock() != null && product.getStock() < 0) {
            throw new BusinessException(400, "商品库存不能为负数");
        }

        // 6. 设置默认值
        if (product.getStatus() == null) {
            product.setStatus(0); // 默认下架
        }
        if (product.getStock() == null) {
            product.setStock(0); // 默认库存为0
        }
        if (product.getSales() == null) {
            product.setSales(0); // 默认销量为0
        }
        if (product.getSort() == null) {
            product.setSort(0); // 默认排序为0
        }
        if (product.getIsDeleted() == null) {
            product.setIsDeleted(0); // 默认未删除
        }

        // 7. 设置创建时间和更新时间（如果未配置自动填充）
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        // 8. 使用 MyBatis Plus 的 insert 方法
        int result = productMapper.insert(product);
        if (result <= 0) {
            throw new BusinessException(500, "添加商品失败");
        }

        // 9. 清除商品列表缓存
        clearProductListCache(product.getCategoryId());
    }

    /**
     * 更新商品
     * 使用 MyBatis Plus 的 updateById 方法
     */
    public void update(Product product) {
        // 1. 校验商品ID不为空
        if (product.getId() == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }

        // 2. 使用 MyBatis Plus 的 selectById 方法查询商品是否存在
        Product existProduct = productMapper.selectById(product.getId());
        if (existProduct == null) {
            throw new BusinessException(404, "商品不存在");
        }

        // 3. 校验商品名称
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new BusinessException(400, "商品名称不能为空");
        }

        // 4. 校验分类是否存在
        if (product.getCategoryId() == null || categoryMapper.selectById(product.getCategoryId()) == null) {
            throw new BusinessException(400, "商品分类不存在");
        }

        // 5. 校验价格
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "商品价格不能为空且不能为负数");
        }

        // 6. 校验原价
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "商品原价不能为负数");
        }
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(product.getPrice()) < 0) {
            throw new BusinessException(400, "商品原价不能小于售价");
        }

        // 7. 校验库存
        if (product.getStock() != null && product.getStock() < 0) {
            throw new BusinessException(400, "商品库存不能为负数");
        }

        // 8. 设置更新时间
        product.setUpdateTime(LocalDateTime.now());

        // 9. 使用 MyBatis Plus 的 updateById 方法
        int result = productMapper.updateById(product);
        if (result <= 0) {
            throw new BusinessException(500, "更新商品失败");
        }

        // 10. 清除商品详情缓存、库存缓存和列表缓存
        clearProductDetailCache(product.getId());
        // 如果分类发生变化，需要清除新旧分类的列表缓存
        if (!existProduct.getCategoryId().equals(product.getCategoryId())) {
            clearProductListCache(existProduct.getCategoryId());
            clearProductListCache(product.getCategoryId());
        } else {
            clearProductListCache(product.getCategoryId());
        }
    }

    /**
     * 根据ID删除商品（逻辑删除）
     * 使用 MyBatis Plus 的 deleteById 方法，配合 @TableLogic 注解自动实现逻辑删除
     */
    public void delete(Long id) {
        // 1. 校验商品ID不为空
        if (id == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }

        // 2. 使用 MyBatis Plus 的 selectById 方法查询商品是否存在
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        // 3. 使用 MyBatis Plus 的 deleteById 方法（自动执行逻辑删除）
        int result = productMapper.deleteById(id);

        // 4. 校验删除结果
        if (result <= 0) {
            throw new BusinessException(500, "删除商品失败");
        }

        // 5. 清除商品详情缓存、库存缓存和列表缓存
        clearProductDetailCache(id);
        clearProductListCache(product.getCategoryId());
    }

    /**
     * 根据ID查询商品（带缓存穿透保护）
     * 互斥锁解决缓存击穿
     * 使用 MyBatis Plus 的 selectById 方法
     *
     * @param id 商品ID
     * @return 商品信息
     * @throws RuntimeException 当商品ID无效或商品不存在时抛出异常
     */
    public Product findById(Long id) {
        // 第1层：参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(400, "商品ID不能为空");
        }

        // 定义缓存Key
        String cacheKey = PRODUCT_CACHE_KEY + id;
        String nullKey = PRODUCT_CACHE_KEY + "null:" + id;

        // 第2层：检查空值缓存（防止缓存穿透）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
            throw new BusinessException(404, "商品不存在");
        }

        // 第3层：尝试从缓存获取
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (Product) cached;
        }

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:product:cache:" + id;
        RLock lock = redissonClient.getLock(lockKey);

        // 最大重试次数
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!tryLock) {
                    // 获取锁失败，短暂等待后重试
                    Thread.sleep(100 * (i + 1)); // 递增等待时间
                    continue;
                }

                try {
                    // 双重检查：获取锁后再次检查缓存
                    cached = redisService.get(cacheKey);
                    if (cached != null) {
                        return (Product) cached;
                    }

                    // 第4层：缓存未命中，查询数据库
                    Product product = productMapper.selectById(id);

                    if (product == null) {
                        // 第5层：缓存空值防止缓存穿透（TTL=5分钟）
                        try {
                            redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存空值失败: {}", e.getMessage());
                        }
                        throw new BusinessException(404, "商品不存在");
                    }

                    // 第6层：写入缓存（带随机过期时间，防止缓存雪崩）
                    try {
                        // 计算随机过期时间：基础时间 + 随机时间
                        int randomExpire = RANDOM_EXPIRE_MIN + (int)(Math.random() * (RANDOM_EXPIRE_MAX - RANDOM_EXPIRE_MIN));
                        redisService.set(cacheKey, product, randomExpire, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.warn("缓存写入失败: {}", e.getMessage());
                    }

                    return product;
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "获取锁被中断");
            } catch (RuntimeException e) {
                // 商品不存在的异常直接抛出
                throw e;
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                log.warn("缓存查询失败: {}", e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    Product product = productMapper.selectById(id);
                    if (product == null) {
                        throw new BusinessException(404, "商品不存在");
                    }
                    return product;
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return product;
    }

    /**
     * 查询所有商品
     * 使用 MyBatis Plus 的 LambdaQueryWrapper
     */
    public List<Product> findAll() {

        // 定义缓存Key
        String cacheKey = PRODUCT_CACHE_KEY + "all";
        String nullKey = PRODUCT_CACHE_KEY + "null:all";

        // 第2层：检查空值缓存（防止缓存穿透）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
            throw new BusinessException(404, "商品不存在");
        }
        // 第3层：尝试从缓存获取
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (List<Product>) cached;
        }

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:product:cache:all";
        RLock lock = redissonClient.getLock(lockKey);

        // 最大重试次数
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!tryLock) {
                    // 获取锁失败，短暂等待后重试
                    Thread.sleep(100 * (i + 1)); // 递增等待时间
                    continue;
                }

                try {
                    // 双重检查：获取锁后再次检查缓存
                    cached = redisService.get(cacheKey);
                    if (cached != null) {
                        return (List<Product>) cached;
                    }

                    // 第4层：缓存未命中，查询数据库
                    List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().
                            orderByAsc(Product::getSort).
                            orderByDesc(Product::getCreateTime));

                    if (products == null) {
                        // 第5层：缓存空值防止缓存穿透（TTL=5分钟）
                        try {
                            redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存空值失败: {}", e.getMessage());
                        }
                        throw new BusinessException(404, "商品不存在");
                    }

                    // 第6层：写入缓存（带随机过期时间，防止缓存雪崩）
                    try {
                        // 计算随机过期时间：基础时间 + 随机时间
                        int randomExpire = RANDOM_EXPIRE_MIN + (int)(Math.random() * (RANDOM_EXPIRE_MAX - RANDOM_EXPIRE_MIN));
                        redisService.set(cacheKey, products, randomExpire, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.warn("缓存写入失败: {}", e.getMessage());
                    }

                    return products;
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "获取锁被中断");
            } catch (RuntimeException e) {
                // 商品不存在的异常直接抛出
                throw e;
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                log.warn("缓存查询失败: {}", e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().
                            orderByAsc(Product::getSort).
                            orderByDesc(Product::getCreateTime));
                    if (products == null) {
                        throw new BusinessException(404, "商品不存在");
                    }
                    return products;
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().
                orderByAsc(Product::getSort).
                orderByDesc(Product::getCreateTime));
        if (products == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return products;
    }

        /**
         * 根据分类ID查询商品列表
         * 使用 MyBatis Plus 的 LambdaQueryWrapper
         */
        public List<Product> listProductsByCategory (Long categoryId){
            // 0.1 校验分类ID不为空
            if (categoryId == null) {
                throw new BusinessException(400, "商品分类ID不能为空");
            }

            // 0.2. 校验分类是否存在
            if (categoryMapper.selectById(categoryId) == null) {
                throw new BusinessException(400, "商品分类不存在");
            }

            // 定义缓存Key
            String cacheKey = PRODUCT_CACHE_KEY +categoryId;
            String nullKey = PRODUCT_CACHE_KEY + "null"+categoryId;

            // 第2层：检查空值缓存（防止缓存穿透）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
                throw new BusinessException(404, "商品不存在");
            }
            // 第3层：尝试从缓存获取
            Object cached = redisService.get(cacheKey);
            if (cached != null) {
                return (List<Product>) cached;
            }

            // 缓存未命中时，使用互斥锁防止缓存击穿
            String lockKey = "lock:product:cache";
            RLock lock = redissonClient.getLock(lockKey);

            // 最大重试次数
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                    if (!tryLock) {
                        // 获取锁失败，短暂等待后重试
                        Thread.sleep(100 * (i + 1)); // 递增等待时间
                        continue;
                    }

                    try {
                        // 双重检查：获取锁后再次检查缓存
                        cached = redisService.get(cacheKey);
                        if (cached != null) {
                            return (List<Product>) cached;
                        }

                        // 第4层：缓存未命中，查询数据库
                        List<Product> products = productMapper.selectList(
                                new LambdaQueryWrapper<Product>()
                                        .eq(Product::getCategoryId, categoryId)
                                        .orderByAsc(Product::getSort)
                                        .orderByDesc(Product::getCreateTime)
                        );
                        if (products == null) {
                            // 第5层：缓存空值防止缓存穿透（TTL=5分钟）
                            try {
                                redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                            } catch (Exception e) {
                                log.warn("缓存空值失败: {}", e.getMessage());
                            }
                            throw new BusinessException(404, "商品不存在");
                        }

                        // 第6层：写入缓存
                        try {
                            redisService.set(cacheKey, products, CACHE_TIMEOUT, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存写入失败: {}", e.getMessage());
                        }

                        return products;
                    } finally {
                        // 确保锁被释放
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(500, "获取锁被中断");
                } catch (RuntimeException e) {
                    // 商品不存在的异常直接抛出
                    throw e;
                } catch (Exception e) {
                    // 其他异常记录日志，继续重试
                    log.warn("缓存查询失败: {}", e.getMessage());
                    if (i == maxRetries - 1) {
                        // 最后一次重试失败，降级直接查数据库
                        List<Product> products = productMapper.selectList(
                                new LambdaQueryWrapper<Product>()
                                        .eq(Product::getCategoryId, categoryId)
                                        .orderByAsc(Product::getSort)
                                        .orderByDesc(Product::getCreateTime)
                        );
                        if (products == null) {
                            throw new BusinessException(404, "商品不存在");
                        }
                        return products;
                    }
                }
            }

            // 所有重试都失败，降级直接查数据库
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getCategoryId, categoryId)
                            .orderByAsc(Product::getSort)
                            .orderByDesc(Product::getCreateTime)
            );
            if (products == null) {
                throw new BusinessException(404, "商品不存在");
            }
            return products;
        }

        /**
         * 根据状态查询商品列表
         * 使用 MyBatis Plus 的 LambdaQueryWrapper
         */
        public List<Product> listProductsByStatus (Integer status){
            // 0. 校验状态值合法（0 或 1）
            if (status == null || (status != 0 && status != 1)) {
                throw new BusinessException(400, "商品状态值不合法");
            }

            // 定义缓存Key
            String cacheKey = PRODUCT_CACHE_KEY +status;
            String nullKey = PRODUCT_CACHE_KEY + "null"+status;

            // 第2层：检查空值缓存（防止缓存穿透）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
                throw new BusinessException(404, "商品不存在");
            }
            // 第3层：尝试从缓存获取
            Object cached = redisService.get(cacheKey);
            if (cached != null) {
                return (List<Product>) cached;
            }

            // 缓存未命中时，使用互斥锁防止缓存击穿
            String lockKey = "lock:product:cache";
            RLock lock = redissonClient.getLock(lockKey);

            // 最大重试次数
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                    if (!tryLock) {
                        // 获取锁失败，短暂等待后重试
                        Thread.sleep(100 * (i + 1)); // 递增等待时间
                        continue;
                    }

                    try {
                        // 双重检查：获取锁后再次检查缓存
                        cached = redisService.get(cacheKey);
                        if (cached != null) {
                            return (List<Product>) cached;
                        }

                        // 第4层：缓存未命中，查询数据库
                        List<Product> products = productMapper.selectList(
                                new LambdaQueryWrapper<Product>()
                                        .eq(Product::getStatus, status)
                                        .orderByAsc(Product::getSort)
                                        .orderByDesc(Product::getCreateTime)
                        );
                        if (products == null) {
                            // 第5层：缓存空值防止缓存穿透（TTL=5分钟）
                            try {
                                redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                            } catch (Exception e) {
                                log.warn("缓存空值失败: {}", e.getMessage());
                            }
                            throw new BusinessException(404, "商品不存在");
                        }

                        // 第6层：写入缓存
                        try {
                            redisService.set(cacheKey, products, CACHE_TIMEOUT, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存写入失败: {}", e.getMessage());
                        }

                        return products;
                    } finally {
                        // 确保锁被释放
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(500, "获取锁被中断");
                } catch (RuntimeException e) {
                    // 商品不存在的异常直接抛出
                    throw e;
                } catch (Exception e) {
                    // 其他异常记录日志，继续重试
                    log.warn("缓存查询失败: {}", e.getMessage());
                    if (i == maxRetries - 1) {
                        // 最后一次重试失败，降级直接查数据库
                        List<Product> products = productMapper.selectList(
                                new LambdaQueryWrapper<Product>()
                                        .eq(Product::getStatus, status)
                                        .orderByAsc(Product::getSort)
                                        .orderByDesc(Product::getCreateTime)
                        );
                        if (products == null) {
                            throw new BusinessException(404, "商品不存在");
                        }
                        return products;
                    }
                }
            }

            // 所有重试都失败，降级直接查数据库
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, status)
                            .orderByAsc(Product::getSort)
                            .orderByDesc(Product::getCreateTime)
            );
            if (products == null) {
                throw new BusinessException(404, "商品不存在");
            }
            return products;
        }

        /**
         * 搜索商品（名称模糊查询）
         * 使用 MyBatis Plus 的 LambdaQueryWrapper
         */
        public List<Product> searchProducts (String keyword){
            // 1. 校验关键字不为空
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new BusinessException(400, "搜索关键字不能为空");
            }

            // 2. 使用 MyBatis Plus 的 LambdaQueryWrapper 模糊查询
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .like(Product::getName, keyword.trim())
                            .orderByDesc(Product::getCreateTime)
            );

            // 3. 返回查询结果
            return products;
        }

        // ==================== 分页查询 ====================

        /**
         * 分页查询商品
         * 使用 MyBatis Plus 的 Page 对象替代 PageHelper
         */
        public PageResult listProductsByPage (Integer pageNum, Integer pageSize){
            // 0. 参数校验与默认值处理
            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }

            // 1. 构建缓存Key
            String cacheKey = PRODUCT_CACHE_KEY + "page:" + pageNum + ":" + pageSize;

            // 2. 尝试从缓存获取
            PageResult cached = (PageResult) redisService.get(cacheKey);

            // 3. 缓存命中直接返回
            if (cached != null) {
                return cached;
            }

            // 缓存未命中时，使用互斥锁防止缓存击穿
            String lockKey = "lock:product:cache:page:" + pageNum + ":" + pageSize;
            RLock lock = redissonClient.getLock(lockKey);

            // 最大重试次数
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                    if (!tryLock) {
                        // 获取锁失败，短暂等待后重试
                        Thread.sleep(100 * (i + 1)); // 递增等待时间
                        continue;
                    }

                    try {
                        // 双重检查：获取锁后再次检查缓存
                        cached = (PageResult) redisService.get(cacheKey);
                        if (cached != null) {
                            return cached;
                        }

                        // 4. 使用 MyBatis Plus 的 Page 对象分页查询
                        Page<Product> page = new Page<>(pageNum, pageSize);
                        Page<Product> resultPage = productMapper.selectPage(
                                page,
                                new LambdaQueryWrapper<Product>()
                                        .orderByAsc(Product::getSort)
                                        .orderByDesc(Product::getCreateTime)
                        );

                        // 5. 封装分页结果
                        PageResult pageResult = new PageResult(
                                pageNum,
                                pageSize,
                                resultPage.getTotal(),
                                resultPage.getRecords()
                        );

                        // 6. 查询结果写入缓存
                        try {
                            redisService.set(cacheKey, pageResult, 5, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存写入失败: {}", e.getMessage());
                        }

                        return pageResult;
                    } finally {
                        // 确保锁被释放
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(500, "获取锁被中断");
                } catch (Exception e) {
                    // 其他异常记录日志，继续重试
                    log.warn("缓存查询失败: {}", e.getMessage());
                    if (i == maxRetries - 1) {
                        // 最后一次重试失败，降级直接查数据库
                        Page<Product> page = new Page<>(pageNum, pageSize);
                        Page<Product> resultPage = productMapper.selectPage(
                                page,
                                new LambdaQueryWrapper<Product>()
                                        .orderByAsc(Product::getSort)
                                        .orderByDesc(Product::getCreateTime)
                        );
                        return new PageResult(pageNum, pageSize, resultPage.getTotal(), resultPage.getRecords());
                    }
                }
            }

            // 所有重试都失败，降级直接查数据库
            Page<Product> page = new Page<>(pageNum, pageSize);
            Page<Product> resultPage = productMapper.selectPage(
                    page,
                    new LambdaQueryWrapper<Product>()
                            .orderByAsc(Product::getSort)
                            .orderByDesc(Product::getCreateTime)
            );
            return new PageResult(pageNum, pageSize, resultPage.getTotal(), resultPage.getRecords());
        }

        /**
         * 条件组合分页查询
         * 使用 MyBatis Plus 的 LambdaQueryWrapper 构建动态条件
         */
        public PageResult listProductsByCondition (ProductQuery query){
            // 1. 参数校验与默认值处理
            if (query == null) {
                query = new ProductQuery();
            }
            if (query.getPageNum() == null || query.getPageNum() < 1) {
                query.setPageNum(1);
            }
            if (query.getPageSize() == null || query.getPageSize() < 1) {
                query.setPageSize(10);
            }

            // 2. 使用 MyBatis Plus 的 Page 对象
            Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());

            // 3. 使用 LambdaQueryWrapper 构建动态查询条件
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

            // 分类条件
            wrapper.eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId());

            // 状态条件：指定了状态才过滤，未指定时查询全部商品
            wrapper.eq(query.getStatus() != null, Product::getStatus, query.getStatus());

            // 关键字模糊查询：优先使用 keyword，为空时回退到 name
            String searchKeyword = query.getKeyword();
            if ((searchKeyword == null || searchKeyword.isEmpty())
                    && query.getName() != null && !query.getName().isEmpty()) {
                searchKeyword = query.getName();
            }
            wrapper.like(searchKeyword != null && !searchKeyword.isEmpty(),
                    Product::getName, searchKeyword);

            // 价格区间
            wrapper.ge(query.getMinPrice() != null, Product::getPrice, query.getMinPrice());
            wrapper.le(query.getMaxPrice() != null, Product::getPrice, query.getMaxPrice());

            // 排序：根据 orderBy / orderType 动态排序（白名单校验防 SQL 注入）
            String orderBy = query.getOrderBy();
            boolean asc = "asc".equalsIgnoreCase(query.getOrderType());
            if (orderBy == null || orderBy.isEmpty()) {
                // 默认按排序字段升序
                wrapper.orderByAsc(Product::getSort);
            } else {
                switch (orderBy) {
                    case "price":
                        if (asc) {
                            wrapper.orderByAsc(Product::getPrice);
                        } else {
                            wrapper.orderByDesc(Product::getPrice);
                        }
                        break;
                    case "sales":
                        if (asc) {
                            wrapper.orderByAsc(Product::getSales);
                        } else {
                            wrapper.orderByDesc(Product::getSales);
                        }
                        break;
                    case "createTime":
                        if (asc) {
                            wrapper.orderByAsc(Product::getCreateTime);
                        } else {
                            wrapper.orderByDesc(Product::getCreateTime);
                        }
                        break;
                    case "id":
                        if (asc) {
                            wrapper.orderByAsc(Product::getId);
                        } else {
                            wrapper.orderByDesc(Product::getId);
                        }
                        break;
                    case "sort":
                    default:
                        if (asc) {
                            wrapper.orderByAsc(Product::getSort);
                        } else {
                            wrapper.orderByDesc(Product::getSort);
                        }
                        break;
                }
            }
            // 次要排序：创建时间倒序
            wrapper.orderByDesc(Product::getCreateTime);

            // 4. 执行分页查询
            Page<Product> resultPage = productMapper.selectPage(page, wrapper);

            // 5. 封装分页结果返回
            return new PageResult(
                    query.getPageNum(),
                    query.getPageSize(),
                    resultPage.getTotal(),
                    resultPage.getRecords()
            );
        }

        // ==================== 库存与销量管理 ====================

        /**
         * 更新库存
         * 使用 MyBatis Plus 的 LambdaUpdateWrapper 实现字段级更新
         *
         * @param id    商品ID
         * @param stock 新库存数量
         *              <p>
         *              实现流程：
         *              1. 参数校验（商品ID不能为空，库存数量不能为空且不能为负数）
         *              2. 查询商品是否存在
         *              3. 使用分布式锁防止并发修改库存
         *              - 锁key: lock:product:stock:{id}
         *              - 尝试获取锁（等待3秒，锁持有10秒）
         *              4. 更新商品库存
         *              5. 释放锁
         */
        public void updateStock (Long id, Integer stock){
            // 1. 参数校验
            if (id == null) {
                throw new BusinessException(400, "商品ID不能为空");
            }
            if (stock == null || stock < 0) {
                throw new BusinessException(400, "库存数量不能为空且不能为负数");
            }

            // 2. 查询商品是否存在

            //2.1 加分布式锁
            String lockKey = "lock:product:stock:" + id;
            RLock lock = redissonClient.getLock(lockKey);
            try {
//            2.2尝试获取锁
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
//            2.3判断是否获取成功
                if (!tryLock) {
                    throw new BusinessException(500, "系统繁忙，请稍后重试");
                }
//            2.4查询商品是否存在
                Product product = productMapper.selectById(id);
                if (product == null) {
                    throw new BusinessException(404, "商品不存在");
                }
                // 3. 使用 LambdaUpdateWrapper 只更新库存和更新时间
                LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Product::getId, id)
                        .set(Product::getStock, stock)
                        .set(Product::getUpdateTime, LocalDateTime.now());
//            2.5判断是否执行成功
                int rows = productMapper.update(null, updateWrapper);
                if (rows <= 0) {
                    throw new BusinessException(500, "更新库存失败");
                }

                // 4. 清除商品详情缓存和库存缓存
                clearProductDetailCache(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "获取锁被中断");
            } finally {
//            释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        /**
         * 扣减库存（下单时）
         * 使用 MyBatis Plus 的 LambdaUpdateWrapper
         */
        public void decreaseStock (Long id, Integer quantity){
            // 1. 参数校验
            if (id == null) {
                throw new BusinessException(400, "商品ID不能为空");
            }
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(400, "扣减数量必须大于0");
            }

            // 2. 查询商品是否存在
            Product product = productMapper.selectById(id);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }

            // 3. 校验库存是否充足（库存为 null 时按 0 处理，避免空指针）
            int currentStock = product.getStock() == null ? 0 : product.getStock();
            if (currentStock < quantity) {
                throw new BusinessException(400, "库存不足，当前库存：" + currentStock);
            }

            // 4. 使用 LambdaUpdateWrapper 扣减库存
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, id)
                    .set(Product::getStock, currentStock - quantity)
                    .set(Product::getUpdateTime, LocalDateTime.now());

            int result = productMapper.update(null, updateWrapper);
            if (result <= 0) {
                throw new BusinessException(500, "扣减库存失败");
            }

            // 5. 清除商品详情缓存和库存缓存
            clearProductDetailCache(id);
        }

        /**
         * 增加销量（支付成功后）
         * 使用 MyBatis Plus 的 LambdaUpdateWrapper
         */
        public void increaseSales (Long id, Integer quantity){
            // 1. 参数校验
            if (id == null) {
                throw new BusinessException(400, "商品ID不能为空");
            }
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(400, "销量数量必须大于0");
            }

            // 2. 查询商品是否存在
            Product product = productMapper.selectById(id);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }

            // 3. 使用 LambdaUpdateWrapper 增加销量
            int newSales = product.getSales() == null ? quantity : product.getSales() + quantity;
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, id)
                    .set(Product::getSales, newSales)
                    .set(Product::getUpdateTime, LocalDateTime.now());

            int result = productMapper.update(null, updateWrapper);
            if (result <= 0) {
                throw new BusinessException(500, "增加销量失败");
            }

            // 4. 清除商品详情缓存
            clearProductDetailCache(id);
        }

        // ==================== 商品状态管理 ====================

        /**
         * 上架商品
         * 使用 MyBatis Plus 的 LambdaUpdateWrapper
         */
        public void publishProduct (Long id){
            // 1. 参数校验
            if (id == null) {
                throw new BusinessException(400, "商品ID不能为空");
            }

            // 2. 查询商品是否存在
            Product product = productMapper.selectById(id);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }

            // 3. 校验库存是否充足
            if (product.getStock() == null || product.getStock() <= 0) {
                throw new BusinessException(400, "商品库存不足，无法上架");
            }

            // 4. 使用 LambdaUpdateWrapper 更新状态
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, id)
                    .set(Product::getStatus, 1)
                    .set(Product::getUpdateTime, LocalDateTime.now());

            int result = productMapper.update(null, updateWrapper);
            if (result <= 0) {
                throw new BusinessException(500, "上架商品失败");
            }

            // 5. 清除商品详情缓存和列表缓存
            clearProductDetailCache(id);
            clearProductListCache(product.getCategoryId());
        }

        /**
         * 下架商品
         * 使用 MyBatis Plus 的 LambdaUpdateWrapper
         */
        public void unpublishProduct (Long id){
            // 1. 参数校验
            if (id == null) {
                throw new BusinessException(400, "商品ID不能为空");
            }

            // 2. 查询商品是否存在
            Product product = productMapper.selectById(id);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }

            // 3. 使用 LambdaUpdateWrapper 更新状态
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, id)
                    .set(Product::getStatus, 0)
                    .set(Product::getUpdateTime, LocalDateTime.now());

            int result = productMapper.update(null, updateWrapper);
            if (result <= 0) {
                throw new BusinessException(500, "下架商品失败");
            }

            // 4. 清除商品详情缓存和列表缓存
            clearProductDetailCache(id);
            clearProductListCache(product.getCategoryId());
        }

        // ==================== 校验方法 ====================

        /**
         * 校验商品是否存在
         * 使用 MyBatis Plus 的 selectById 方法
         */
        public boolean checkProductExists (Long id){
            // 1. 参数校验
            if (id == null) {
                return false;
            }

            // 2. 先从缓存获取
            String cacheKey = PRODUCT_CACHE_KEY + "exists:" + id;
            Boolean cached = (Boolean) redisService.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            // 3. 缓存未命中，使用 MyBatis Plus 查询数据库
            boolean exists = productMapper.selectById(id) != null;

            // 4. 写入缓存
            redisService.set(cacheKey, exists, CACHE_TIMEOUT, TimeUnit.MINUTES);

            return exists;
        }

        /**
         * 校验库存是否充足
         * 使用 MyBatis Plus 的 selectById 方法
         */
        public boolean checkStockEnough (Long id, Integer quantity){
            // 1. 参数校验
            if (id == null || quantity == null || quantity <= 0) {
                return false;
            }

            // 2. 先从缓存获取库存
            String cacheKey = PRODUCT_CACHE_KEY + "stock:" + id;
            Integer cachedStock = (Integer) redisService.get(cacheKey);
            if (cachedStock != null) {
                return cachedStock >= quantity;
            }

            // 3. 缓存未命中，使用 MyBatis Plus 查询数据库
            Product product = productMapper.selectById(id);
            if (product == null) {
                return false;
            }

            // 4. 写入缓存（库存实时性要求高，使用短过期时间）
            if (product.getStock() != null) {
                redisService.set(cacheKey, product.getStock(), 1, TimeUnit.MINUTES);
            }

            // 5. 校验库存是否充足并返回结果
            return product.getStock() != null && product.getStock() >= quantity;
        }

        // ==================== 图片上传 ====================

        /**
         * 上传商品主图（OSS）
         */
        public Result<String> uploadMainImage (org.springframework.web.multipart.MultipartFile file){
            // 1. 校验文件
            if (file == null || file.isEmpty()) {
                return Result.error(400, "请选择要上传的文件");
            }

            // 2. 校验文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error(400, "只能上传图片文件");
            }

            // 3. 校验文件大小（最大 5MB）
            long maxSize = 5 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return Result.error(400, "图片大小不能超过5MB");
            }

            // 4. 上传到 OSS
            try {
                String imageUrl = ossService.upload(file, "product/");
                return Result.success(imageUrl);
            } catch (Exception e) {
                return Result.error(500, "上传失败：" + e.getMessage());
            }
        }

        // ==================== 缓存清除辅助方法 ====================

        /**
         * 清除商品详情缓存
         *
         * @param productId 商品ID
         */
        private void clearProductDetailCache (Long productId){
            if (productId == null) {
                return;
            }
            try {
                redisService.delete(PRODUCT_CACHE_KEY + "detail:" + productId);
                redisService.delete(PRODUCT_CACHE_KEY + productId);
                redisService.delete(PRODUCT_CACHE_KEY + "stock:" + productId);
                redisService.delete(PRODUCT_CACHE_KEY + "exists:" + productId);
            } catch (Exception e) {
                // 缓存清除失败不影响主流程，记录日志即可
                log.warn("清除商品详情缓存失败: {}", e.getMessage());
            }
        }

        /**
         * 清除商品列表缓存
         *
         * @param categoryId 分类ID（可选）
         */
        private void clearProductListCache (Long categoryId){
            try {
                // 清除所有商品列表缓存
                redisService.delete(PRODUCT_CACHE_KEY + "list:all");

                // 清除分类列表缓存
                if (categoryId != null) {
                    redisService.delete(PRODUCT_CACHE_KEY + "list:category:" + categoryId);
                }

                // 清除状态列表缓存（使用通配符）
                redisService.deleteByPattern(PRODUCT_CACHE_KEY + "list:status:*");

                // 清除分页缓存（使用通配符）
                redisService.deleteByPattern(PRODUCT_CACHE_KEY + "page:*");
            } catch (Exception e) {
                // 缓存清除失败不影响主流程，记录日志即可
                log.warn("清除商品列表缓存失败: {}", e.getMessage());
            }
        }

    /**
     * 增加库存（取消订单时恢复库存）
     *
     * @param id 商品ID
     * @param quantity 增加的库存数量
     */
    public void increaseStock(Long id, Integer quantity) {
        // 1. 参数校验
        if (id == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "增加数量必须大于0");
        }

        // 2. 原子回补库存：stock = stock + ?，由数据库完成计算
        // 取消订单的回补现在由资源队列消费者触发，多订单并发回补时"先查后写"会互相覆盖
        int affected = productMapper.increaseStock(id, quantity);
        if (affected == 0) {
            throw new BusinessException(404, "商品不存在");
        }

        // 3. 清除商品详情缓存和库存缓存
        clearProductDetailCache(id);
    }
}
