package com.mall.cache.controller;

import com.mall.common.Result;
import com.mall.cache.service.CacheAvalancheService;
import com.mall.cache.service.CacheWarmupService;
import com.mall.cache.service.MultiLevelCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cache")
@Tag(name = "缓存服务", description = "缓存管理相关接口")
public class CacheController {

    @Autowired
    private CacheWarmupService cacheWarmupService;

    @Autowired
    private CacheAvalancheService cacheAvalancheService;

    @Autowired
    private MultiLevelCacheService multiLevelCacheService;

    @PostMapping("/warmup/product/{productId}")
    @Operation(summary = "商品缓存预热")
    public Result<String> warmupProduct(@PathVariable Long productId, @RequestBody Object productData) {
        cacheWarmupService.warmupProductCache(productId, productData);
        return Result.success("预热成功");
    }

    @PostMapping("/warmup/batch")
    @Operation(summary = "批量缓存预热")
    public Result<String> warmupBatch(@RequestParam String keyPrefix, @RequestBody Map<Long, Object> dataMap, @RequestParam long expireHours) {
        cacheWarmupService.warmupBatch(keyPrefix, dataMap, expireHours);
        return Result.success("批量预热成功");
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清除缓存")
    public Result<String> clearCache(@RequestParam String keyPattern) {
        cacheWarmupService.clearCache(keyPattern);
        return Result.success("缓存清除成功");
    }

    @PostMapping("/set")
    @Operation(summary = "设置缓存（带随机过期时间）")
    public Result<String> setCache(@RequestParam String key, @RequestBody Object value, @RequestParam long expireMinutes) {
        cacheAvalancheService.setCacheWithRandomExpire(key, value, expireMinutes);
        return Result.success("设置成功");
    }

    @GetMapping("/multi-level/{key}")
    @Operation(summary = "多级缓存获取")
    public Result<Object> getMultiLevelCache(@PathVariable String key) {
        Object value = multiLevelCacheService.get(key, null);
        return Result.success(value);
    }

    @PostMapping("/multi-level")
    @Operation(summary = "多级缓存写入")
    public Result<String> putMultiLevelCache(@RequestParam String key, @RequestBody Object value) {
        multiLevelCacheService.put(key, value);
        return Result.success("写入成功");
    }

    @DeleteMapping("/multi-level/{key}")
    @Operation(summary = "多级缓存删除")
    public Result<String> evictMultiLevelCache(@PathVariable String key) {
        multiLevelCacheService.evict(key);
        return Result.success("删除成功");
    }

    @DeleteMapping("/multi-level/local")
    @Operation(summary = "清空本地缓存")
    public Result<String> clearLocalCache() {
        multiLevelCacheService.clearLocalCache();
        return Result.success("本地缓存已清空");
    }
}