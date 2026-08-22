package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.entity.Category;
import com.mall.entity.vo.CategoryVO;
import com.mall.mapper.CategoryMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    private static final String CATEGORY_CACHE_KEY = "category:";
    private static final String CATEGORY_LOCK_KEY = "lock:category:";
    private static final long CACHE_TIMEOUT = 30;
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 10;

    /**
     * 构造函数，注入CategoryMapper依赖
     * @param categoryMapper 分类数据访问层接口
     */
    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 添加新分类
     * @param category 分类实体对象
     * @throws RuntimeException 当分类名称已存在时抛出异常
     */
    public void add(Category category) {
        Category exist = categoryMapper.findByName(category.getName());
        if (exist != null) {
            throw new RuntimeException("分类名称已存在");
        }
        if (category.getStatus() == null) category.setStatus(1);
        if (category.getSort() == null) category.setSort(0);
        if (category.getLevel() == null) category.setLevel(1);
        if (category.getParentId() == null) category.setParentId(0L);
        categoryMapper.insert(category);
        clearCategoryCache();
    }

    /**
     * 更新分类信息
     * @param category 分类实体对象
     * @throws RuntimeException 当分类不存在或分类名称已被其他分类占用时抛出异常
     */
    public void update(Category category) {
        Category exist = categoryMapper.selectById(category.getId());
        if (exist == null) {
            throw new RuntimeException("分类不存在");
        }
        Category nameCategory = categoryMapper.findByName(category.getName());
        if (nameCategory != null && !nameCategory.getId().equals(category.getId())) {
            throw new RuntimeException("分类名称已存在");
        }
        categoryMapper.updateById(category);
        clearCategoryCache();
    }

    /**
     * 根据ID删除分类
     * @param id 分类ID
     * @throws RuntimeException 当分类不存在或存在子分类时抛出异常
     */
    public void deleteById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        int childCount = categoryMapper.countByParentId(id);
        if (childCount > 0) {
            throw new RuntimeException("存在子分类，无法删除");
        }
        categoryMapper.deleteById(id);
        clearCategoryCache();
    }

    /**
     * 根据ID查询分类详情（带缓存）
     * @param id 分类ID
     * @return 分类实体对象
     * @throws RuntimeException 当分类ID无效或分类不存在时抛出异常
     */
    public Category findById(Long id) {
        // 第1层：参数校验
        if (id == null || id <= 0) {
            throw new RuntimeException("分类ID不能为空");
        }

        // 定义缓存Key
        String cacheKey = CATEGORY_CACHE_KEY + "detail:" + id;
        String nullKey = CATEGORY_CACHE_KEY + "null:" + id;

        // 第2层：检查空值缓存（防止缓存穿透）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
            throw new RuntimeException("分类不存在");
        }

        // 第3层：尝试从缓存获取
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            return (Category) cached;
        }

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = CATEGORY_LOCK_KEY + "detail:" + id;
        RLock lock = redissonClient.getLock(lockKey);

        // 最大重试次数
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean tryLock = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
                if (!tryLock) {
                    // 获取锁失败，短暂等待后重试
                    Thread.sleep(100 * (i + 1)); // 递增等待时间
                    continue;
                }

                try {
                    // 双重检查：获取锁后再次检查缓存
                    cached = redisService.get(cacheKey);
                    if (cached != null) {
                        return (Category) cached;
                    }

                    // 第4层：缓存未命中，查询数据库
                    Category category = categoryMapper.selectById(id);

                    if (category == null) {
                        // 第5层：缓存空值防止缓存穿透（TTL=5分钟）
                        try {
                            redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            System.err.println("缓存空值失败: " + e.getMessage());
                        }
                        throw new RuntimeException("分类不存在");
                    }

                    // 第6层：写入缓存
                    try {
                        redisService.setWithRandomExpire(cacheKey, category, CACHE_TIMEOUT, TimeUnit.MINUTES, 300);
                    } catch (Exception e) {
                        System.err.println("缓存写入失败: " + e.getMessage());
                    }

                    return category;
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁失败", e);
            }
        }

        // 第7层：重试失败后的降级处理
        System.err.println("缓存击穿防护失败，降级到数据库查询");
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return category;
    }

    /**
     * 根据父分类ID查询子分类列表（带缓存）
     * @param parentId 父分类ID
     * @return 子分类列表
     * @throws RuntimeException 当父分类ID无效时抛出异常
     */
    public List<Category> findByParentId(Long parentId) {
        if (parentId == null || parentId < 0) {
            throw new RuntimeException("父分类ID无效");
        }

        String cacheKey = CATEGORY_CACHE_KEY + "children:" + parentId;
        String lockKey = CATEGORY_LOCK_KEY + "children:" + parentId;

        try {
            // 尝试从缓存获取
            Object cached = redisService.get(cacheKey);
            if (cached != null) {
                return (List<Category>) cached;
            }

            // 缓存未命中，使用互斥锁防止缓存击穿
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
                
                if (locked) {
                    try {
                        // 双重检查
                        cached = redisService.get(cacheKey);
                        if (cached != null) {
                            return (List<Category>) cached;
                        }

                        // 查询数据库
                        List<Category> categories = categoryMapper.findByParentId(parentId);

                        // 写入缓存（即使为空列表也缓存，防止缓存穿透）
                        redisService.setWithRandomExpire(cacheKey, categories, CACHE_TIMEOUT, TimeUnit.MINUTES, 300);

                        return categories;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // 获取锁失败，等待并重试
                    Thread.sleep(50);
                    return findByParentId(parentId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁失败", e);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Redis不可用，降级到数据库: " + e.getMessage());
            // Redis异常时直接查询数据库
            return categoryMapper.findByParentId(parentId);
        }
    }

    /**
     * 根据分类层级查询分类列表（带缓存）
     * @param level 分类层级（1-3）
     * @return 指定层级的分类列表
     * @throws RuntimeException 当分类层级无效时抛出异常
     */
    public List<Category> findByLevel(Integer level) {
        if (level == null || level < 1 || level > 3) {
            throw new RuntimeException("分类层级无效（1-3）");
        }

        String cacheKey = CATEGORY_CACHE_KEY + "level:" + level;

        try {
            Object cached = redisService.get(cacheKey);
            if (cached != null) {
                return (List<Category>) cached;
            }
        } catch (Exception e) {
            System.err.println("Redis不可用，降级到数据库: " + e.getMessage());
        }

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:category:cache:level:" + level;
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
                    Object cached = redisService.get(cacheKey);
                    if (cached != null) {
                        return (List<Category>) cached;
                    }

                    // 查询数据库
                    List<Category> categories = categoryMapper.findByLevel(level);

                    // 写入缓存
                    try {
                        redisService.setWithRandomExpire(cacheKey, categories, CACHE_TIMEOUT, TimeUnit.MINUTES, 300);

                    } catch (Exception e) {
                        System.err.println("缓存写入失败: " + e.getMessage());
                    }

                    return categories;
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断", e);
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                System.err.println("缓存查询失败: " + e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    return categoryMapper.findByLevel(level);
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        return categoryMapper.findByLevel(level);
    }
    /**
     * 查询所有分类（带缓存）
     * @return 所有分类列表
     */
    @SuppressWarnings("unchecked")
    public List<Category> findAll() {
        String cacheKey = CATEGORY_CACHE_KEY + "all";
        List<Category> cached = (List<Category>) redisService.get(cacheKey);
        if (cached != null) return cached;

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:category:cache:all";
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
                    cached = (List<Category>) redisService.get(cacheKey);
                    if (cached != null) {
                        return cached;
                    }

                    // 查询数据库
                    List<Category> categories = categoryMapper.selectList(null);

                    // 写入缓存
                    if (categories != null && !categories.isEmpty()) {
                        try {
                            redisService.setWithRandomExpire(cacheKey, categories, CACHE_TIMEOUT, TimeUnit.MINUTES, 300);
                        } catch (Exception e) {
                            System.err.println("缓存写入失败: " + e.getMessage());
                        }
                    }

                    return categories;
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断", e);
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                System.err.println("缓存查询失败: " + e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    return categoryMapper.selectList(null);
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        return categoryMapper.selectList(null);
    }

    /**
     * 获取分类树（一级分类 → 二级 → 三级）
     */
    @SuppressWarnings("unchecked")
    public List<CategoryVO> getCategoryTree() {
        String cacheKey = CATEGORY_CACHE_KEY + "tree";
        List<CategoryVO> cached = (List<CategoryVO>) redisService.get(cacheKey);
        if (cached != null) return cached;

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:category:cache:tree";
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
                    cached = (List<CategoryVO>) redisService.get(cacheKey);
                    if (cached != null) {
                        return cached;
                    }

                    // 查询数据库
                    List<Category> all = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>()
                                    .eq(Category::getStatus, 1)
                                    .orderByAsc(Category::getSort, Category::getId)
                    );

                    List<CategoryVO> tree = all.stream()
                            .filter(c -> c.getParentId() == 0)
                            .map(c -> toVO(c, all))
                            .collect(Collectors.toList());

                    // 写入缓存
                    try {
                        redisService.setWithRandomExpire(cacheKey, tree, CACHE_TIMEOUT, TimeUnit.MINUTES, 300);
                    } catch (Exception e) {
                        System.err.println("缓存写入失败: " + e.getMessage());
                    }

                    return tree;
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断", e);
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                System.err.println("缓存查询失败: " + e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    List<Category> all = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>()
                                    .eq(Category::getStatus, 1)
                                    .orderByAsc(Category::getSort, Category::getId)
                    );
                    return all.stream()
                            .filter(c -> c.getParentId() == 0)
                            .map(c -> toVO(c, all))
                            .collect(Collectors.toList());
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort, Category::getId)
        );
        return all.stream()
                .filter(c -> c.getParentId() == 0)
                .map(c -> toVO(c, all))
                .collect(Collectors.toList());
    }

    /**
     * 将Category实体转换为CategoryVO对象（递归构建子分类）
     * @param category 当前分类实体
     * @param all 所有分类列表
     * @return 分类VO对象
     */
    private CategoryVO toVO(Category category, List<Category> all) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setLevel(category.getLevel());
        vo.setSort(category.getSort());
        vo.setIcon(category.getIcon());
        vo.setStatus(category.getStatus());
        // 1. 从所有分类列表创建流
        List<CategoryVO> children = all.stream()
                // 2. 筛选出当前分类的子分类
                .filter(c -> c.getParentId().equals(category.getId()))
                // 3. 将每个子分类转换为VO对象（递归）
                .map(c -> toVO(c, all))
                .collect(Collectors.toList());
        vo.setChildren(children.isEmpty() ? null : children);

        return vo;
    }

    /**
     * 更新分类状态
     * @param id 分类ID
     * @param status 状态值（0-禁用，1-启用）
     * @throws RuntimeException 当分类不存在时抛出异常
     */
    public void updateStatus(Long id, Integer status) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        categoryMapper.updateStatus(id, status);

        clearCategoryCache();
    }

    /**
     * 清除所有分类缓存
     */
    private void clearCategoryCache() {
        try {
            // 使用通配符清除所有分类相关缓存
            redisService.deleteByPattern(CATEGORY_CACHE_KEY + "*");
            System.out.println("已清除所有分类缓存");
        } catch (Exception e) {
            System.err.println("清除分类缓存失败: " + e.getMessage());
        }
    }
}
