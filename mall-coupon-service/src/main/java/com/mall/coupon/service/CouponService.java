package com.mall.coupon.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.CouponStatus;
import com.mall.common.PageResult;
import com.mall.common.RedisService;
import com.mall.common.Result;
import com.mall.coupon.entity.Coupon;
import com.mall.coupon.entity.UserCoupon;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.mapper.UserCouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CouponService {

    private final RedisService redisService;
    private final CacheService cacheService;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    public CouponService(RedisService redisService, CacheService cacheService, CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.redisService = redisService;
        this.cacheService = cacheService;
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    /**
     * 获取优惠券列表（带缓存）
     */
    public List<Coupon> getCouponsList() {
        String cacheKey = "coupon:active:list";
        List<Coupon> list = cacheService.getOrLoad(cacheKey, () ->
                        couponMapper.selectList(
                                new LambdaQueryWrapper<Coupon>()
                                        .eq(Coupon::getStatus, CouponStatus.ON)
                                        .gt(Coupon::getEndTime, LocalDateTime.now())
                                        .orderByDesc(Coupon::getCreateTime)
                        ),
                30, TimeUnit.MINUTES);
        // 派生状态在缓存命中后重新计算，避免「未开始 / 已过期 / 已抢光」跟着 30 分钟缓存一起变旧
        if (list != null) {
            list.forEach(this::fillDisplayStatus);
        }
        return list;
    }

    /**
     * 获取优惠券详情（带缓存）
     */
    public Coupon getCouponDetail(Long couponId) {
        String cacheKey = "coupon:detail:" + couponId;
        Coupon coupon = cacheService.getOrLoad(cacheKey, () -> couponMapper.selectById(couponId), 5, TimeUnit.SECONDS);
        return fillDisplayStatus(coupon);
    }

    /**
     * 清除优惠券缓存
     */
    public void deleteCouponCache(Long couponId) {
        String cacheKey = "coupon:detail:" + couponId;
        cacheService.delete(cacheKey);
    }

    /** 管理端分页查询：status/type/keyword 都可空 */
    public PageResult pageCoupons(Integer status, Integer type, String keyword,
                                  Integer pageNum, Integer pageSize) {
        Page<Coupon> page = new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100));
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Coupon::getStatus, status)
                .eq(type != null, Coupon::getType, type)
                .like(StrUtil.isNotBlank(keyword), Coupon::getName, keyword)
                .orderByDesc(Coupon::getCreateTime);
        IPage<Coupon> result = couponMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillDisplayStatus);
        return new PageResult((int) result.getCurrent(), (int) result.getSize(),
                result.getTotal(), result.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> createCoupon(Coupon coupon) {
        String err = validate(coupon);              // 见下方校验
        if (err != null) return Result.error(400, err);
        if (couponMapper.findByName(coupon.getName()) != null) {
            return Result.error(400, "优惠券名称已存在");
        }
        coupon.setId(null);
        coupon.setStatus(coupon.getStatus() == null ? CouponStatus.OFF : coupon.getStatus()); // 默认先下架，确认无误再上架
        coupon.setAvailableStock(coupon.getAvailableStock() == null
                ? coupon.getTotalStock() : coupon.getAvailableStock());
        coupon.setVersion(0);
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());
        couponMapper.insert(coupon);
        evictCouponCache(coupon.getId());
        return Result.success("创建成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateCoupon(Coupon coupon) {
        Coupon exist = couponMapper.selectById(coupon.getId());
        if (exist == null) return Result.error(404, "优惠券不存在");
        String err = validate(coupon);
        if (err != null) return Result.error(400, err);
        if (!exist.getName().equals(coupon.getName()) && couponMapper.findByName(coupon.getName()) != null) {
            return Result.error(400, "优惠券名称已存在");
        }
        // 库存只能加不能减（减库存要走 adjustStock 且要同步 Redis）
        coupon.setAvailableStock(null);
        coupon.setTotalStock(null);
        coupon.setVersion(null);          // 乐观锁：不传 version 就不校验，简单起见改用 updateById + exist.getVersion()
        coupon.setUpdateTime(LocalDateTime.now());
        couponMapper.updateById(coupon);
        evictCouponCache(coupon.getId());
        return Result.success("更新成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateStatus(Long couponId, Integer status) {
        Coupon exist = couponMapper.selectById(couponId);
        if (exist == null) return Result.error(404, "优惠券不存在");
        if (!CouponStatus.isValid(status)) {
            return Result.error(400, "状态只能是 0-未上架 / 1-已上架 / 2-已停用");
        }
        if (exist.getStatus() != null && exist.getStatus() == CouponStatus.STOPPED && status == CouponStatus.ON) {
            return Result.error(400, "已停用的优惠券不能再上架");
        }
        if (status == CouponStatus.ON) {
            if (exist.getEndTime() == null || exist.getEndTime().isBefore(LocalDateTime.now())) {
                return Result.error(400, "已过期的优惠券不能上架");
            }
            if (exist.getStartTime() != null && !exist.getEndTime().isAfter(exist.getStartTime())) {
                return Result.error(400, "结束时间必须晚于开始时间");
            }
        }
        couponMapper.updateStatus(couponId, status);   // mapper 里已有
        evictCouponCache(couponId);
        return Result.success(status == CouponStatus.ON ? "已上架" : status == CouponStatus.STOPPED ? "已停用" : "已下架");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> adjustStock(Long couponId, Integer totalStock) {
        Coupon exist = couponMapper.selectById(couponId);
        if (exist == null) return Result.error(404, "优惠券不存在");
        if (totalStock == null || totalStock < 0) return Result.error(400, "库存不能为负");
        int used = exist.getTotalStock() - exist.getAvailableStock();   // 已领取数量
        if (totalStock < used) {
            return Result.error(400, "总库存不能小于已领取数量 " + used);
        }
        LambdaUpdateWrapper<Coupon> uw = new LambdaUpdateWrapper<>();
        uw.eq(Coupon::getId, couponId)
                .set(Coupon::getTotalStock, totalStock)
                .set(Coupon::getAvailableStock, totalStock - used)
                .set(Coupon::getUpdateTime, LocalDateTime.now());
        couponMapper.update(null, uw);
        evictCouponCache(couponId);
        return Result.success("库存已更新");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> deleteCoupon(Long couponId) {
        Coupon exist = couponMapper.selectById(couponId);
        if (exist == null) return Result.error(404, "优惠券不存在");
        Long claimed = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, couponId));
        if (claimed != null && claimed > 0) {
            return Result.error(400, "该券已有 " + claimed + " 条领取记录，不能删除，请改为下架");
        }
        couponMapper.deleteById(couponId);
        evictCouponCache(couponId);
        return Result.success("已删除");
    }

    /** 领取/使用统计 */
    public Map<String, Object> getStats(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) return Collections.emptyMap();
        Map<String, Object> map = new HashMap<>();
        map.put("claimed", coupon.getTotalStock() - coupon.getAvailableStock());
        map.put("unused", userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId).eq(UserCoupon::getStatus, 0)));
        map.put("used", userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId).eq(UserCoupon::getStatus, 1)));
        map.put("expired", userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId).eq(UserCoupon::getStatus, 2)));
        return map;
    }

    /** 领取明细，复用已有的 findByCouponId */
    public List<UserCoupon> getClaimRecords(Long couponId) {
        return userCouponMapper.findByCouponId(couponId);
    }

    /** 用 CouponStatus 统一计算派生状态，管理端与 C 端都直接读这三个字段 */
    private Coupon fillDisplayStatus(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        int display = CouponStatus.derive(coupon.getStatus(), coupon.getStartTime(),
                coupon.getEndTime(), coupon.getAvailableStock());
        coupon.setDisplayStatus(display);
        coupon.setDisplayStatusText(CouponStatus.text(display));
        coupon.setDisplayStatusType(CouponStatus.tagType(display));
        return coupon;
    }

    private String validate(Coupon c) {
        if (StrUtil.isBlank(c.getName())) return "名称不能为空";
        if (c.getStatus() != null && !CouponStatus.isValid(c.getStatus())) return "状态只能是 0-未上架 / 1-已上架 / 2-已停用";
        if (c.getType() == null || c.getType() < 1 || c.getType() > 3) return "类型只能是 1/2/3";
        if (c.getValue() == null || c.getValue().compareTo(BigDecimal.ZERO) <= 0) return "面额必须大于 0";
        if (c.getType() == 2 && c.getValue().compareTo(new BigDecimal("10")) >= 0) return "折扣券的面额要填折扣数（如 8 表示 8 折）";
        if (c.getMinAmount() == null) c.setMinAmount(BigDecimal.ZERO);
        if (c.getTotalStock() == null || c.getTotalStock() < 0) return "总库存不能为负";
        if (c.getLimitPerUser() == null || c.getLimitPerUser() < 1) return "每人限领至少 1 张";
        if (c.getStartTime() == null || c.getEndTime() == null) return "开始/结束时间不能为空";
        if (!c.getEndTime().isAfter(c.getStartTime())) return "结束时间必须晚于开始时间";
        return null;
    }

    /** 一次清三种缓存：列表缓存、详情缓存、抢券的 Redis 库存 */
    private void evictCouponCache(Long couponId) {
        cacheService.delete("coupon:detail:" + couponId);
        cacheService.delete("coupon:active:list");
        redisService.delete("seckill:stock:" + couponId);
    }
}