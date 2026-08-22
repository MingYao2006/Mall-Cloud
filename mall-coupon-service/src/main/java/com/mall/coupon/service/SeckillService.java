package com.mall.coupon.service;

import com.mall.common.Result;
import com.mall.coupon.entity.Coupon;
import com.mall.coupon.entity.SeckillLog;
import com.mall.coupon.entity.UserCoupon;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.mapper.SeckillLogMapper;
import com.mall.coupon.mapper.UserCouponMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillService.class);
    private static final String COUPON_STOCK_KEY = "seckill:stock:";
    private static final String SECKILL_USER_KEY = "seckill:user:";
    private static final String SECKILL_LOCK_KEY = "seckill:lock:";
    private static final int RATE_LIMIT_THRESHOLD = 10;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private SeckillLogMapper seckillLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<String> seckillCoupon(Long userId, Long couponId) {
        if (userId == null || couponId == null) {
            return Result.error(500, "参数错误");
        }
        
        String limitKey = "seckill:limit:" + userId;
        Long requestCount = redisTemplate.opsForValue().increment(limitKey, 1);
        if (requestCount == 1) {
            redisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        }
        if (requestCount > RATE_LIMIT_THRESHOLD) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        
        String rediskey = SECKILL_LOCK_KEY + couponId;
        RLock lock = redissonClient.getLock(rediskey);

        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!tryLock) {
                return Result.error(500, "系统繁忙，请稍后重试");
            }

            String userkey = SECKILL_USER_KEY + couponId + ":" + userId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(userkey))) {
                return Result.error(600, "您已参与过该活动");
            }

            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || coupon.getStatus() != 1) {
                return Result.error(600, "优惠券不存在或活动未开始");
            }
            
            if (coupon.getLimitPerUser() != null && coupon.getLimitPerUser() > 0) {
                Integer count = userCouponMapper.countByUserIdAndCouponId(userId, couponId);
                if (count != null && count >= coupon.getLimitPerUser()) {
                    return Result.error(600, "已达到每人限抢数量");
                }
            }

            Long stock = decreaseStock(couponId);
            if (stock < 0) {
                return Result.error(600, "优惠券已抢完");
            }

            int decreased = couponMapper.decreaseStock(couponId);
            if (decreased == 0) {
                redisTemplate.opsForValue().increment(COUPON_STOCK_KEY + couponId);
                return Result.fail("抢券失败，请重试");
            }
            
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(userId);
            userCoupon.setCouponId(couponId);
            userCoupon.setStatus(0);
            userCoupon.setExpireTime(LocalDateTime.now().plusDays(7));
            userCouponMapper.insert(userCoupon);
            
            SeckillLog seckillLog = new SeckillLog();
            seckillLog.setCouponId(couponId);
            seckillLog.setUserId(userId);
            seckillLog.setStatus(1);
            seckillLog.setSeckillTime(LocalDateTime.now());
            seckillLogMapper.insert(seckillLog);
            
            redisTemplate.opsForValue().set(userkey, "1", 24, TimeUnit.HOURS);
            logger.info("秒杀成功: userId={}, couponId={}", userId, couponId);
            return Result.success("抢券成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            compensateRedisStock(couponId);
            return Result.error(600, "系统异常");
        } catch (Exception e) {
            compensateRedisStock(couponId);
            logger.error("秒杀失败，回滚Redis库存: userId={}, couponId={}", userId, couponId, e);
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void compensateRedisStock(Long couponId) {
        try {
            String stockKey = COUPON_STOCK_KEY + couponId;
            redisTemplate.opsForValue().increment(stockKey, 1);
            logger.info("Redis库存补偿成功: couponId={}", couponId);
        } catch (Exception e) {
            logger.error("Redis库存补偿失败: couponId={}", couponId, e);
        }
    }

    private Long decreaseStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))) {
                Coupon coupon = couponMapper.selectById(couponId);
                if (coupon != null) {
                    redisTemplate.opsForValue().set(stockKey, coupon.getAvailableStock(), 1, TimeUnit.DAYS);
                }
            }
            String luaScript = "local stock = redis.call('get', KEYS[1]) " +
                    "if not stock then return -1 end " +
                    "stock = tonumber(stock) " +
                    "if stock <= 0 then return -1 end " +
                    "redis.call('decr', KEYS[1]) " +
                    "return stock - 1";

            Long result = (Long) redisTemplate.execute(
                    (RedisCallback<Long>) connection -> connection.eval(
                            luaScript.getBytes(),
                            ReturnType.INTEGER,
                            1,
                            stockKey.getBytes()
                    )
            );
            return result;

        } catch (Exception e) {
            throw new RuntimeException("预减库存失败", e);
        }
    }
}