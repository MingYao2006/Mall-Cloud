package com.mall.service;

import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.entity.SeckillLog;
import com.mall.entity.UserCoupon;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.SeckillLogMapper;
import com.mall.mapper.UserCouponMapper;
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

    private static final long CACHE_TIMEOUT = 30; // 30分钟基础过期时间
    private static final int RANDOM_EXPIRE_MIN = 10; // 随机过期时间最小值（分钟）
    private static final int RANDOM_EXPIRE_MAX = 40; // 随机过期时间最大值（分钟）
    private static final int RATE_LIMIT_THRESHOLD = 10; // 用户限流阈值：60秒内最多请求10次

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

    /**
     * 秒杀抢券 - 核心方法
     * 使用 Redis + 分布式锁 + 数据库乐观锁 三层防护
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> seckillCoupon(Long userId, Long couponId) {
        // 1. 基础参数校验
        if (userId == null || couponId == null) {
            return Result.error(500, "参数错误");
        }
        
        // 2. 用户限流 - 防止恶意刷接口（提前到最前面，快速失败）
        String limitKey = "seckill:limit:" + userId;
        Long requestCount = redisTemplate.opsForValue().increment(limitKey, 1);
        if (requestCount == 1) {
            redisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        }
        if (requestCount > RATE_LIMIT_THRESHOLD) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        
        // 3. 分布式锁 - 防止并发超卖
        String rediskey = SECKILL_LOCK_KEY + couponId;
        RLock lock = redissonClient.getLock(rediskey);

        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!tryLock) {
                return Result.error(500, "系统繁忙，请稍后重试");
            }

            // 4. 幂等性校验 - 防止重复提交（放在锁内，确保一致性）
            String userkey = SECKILL_USER_KEY + couponId + ":" + userId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(userkey))) {
                return Result.error(600, "您已参与过该活动");
            }

            // 5. 查询优惠券信息并校验每人限抢数量
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || coupon.getStatus() != 1) {
                return Result.error(600, "优惠券不存在或活动未开始");
            }
            
            // 检查用户已领取数量是否达到限制
            if (coupon.getLimitPerUser() != null && coupon.getLimitPerUser() > 0) {
                Integer count = userCouponMapper.countByUserIdAndCouponId(userId, couponId);
                if (count != null && count >= coupon.getLimitPerUser()) {
                    return Result.error(600, "已达到每人限抢数量");
                }
            }

            // 6. Redis 预减库存（Lua脚本保证原子性）
            Long stock = decreaseStock(couponId);
            if (stock < 0) {
                return Result.error(600, "优惠券已抢完");
            }

            // 7. 数据库扣减库存（乐观锁）
            int decreased = couponMapper.decreaseStock(couponId);
            if (decreased == 0) {
                // 数据库扣减失败，回滚Redis库存
                redisTemplate.opsForValue().increment(COUPON_STOCK_KEY + couponId);
                return Result.fail("抢券失败，请重试");
            }
            
            // 8. 生成用户优惠券
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(userId);
            userCoupon.setCouponId(couponId);
            userCoupon.setStatus(0);
            userCoupon.setExpireTime(LocalDateTime.now().plusDays(7));
            userCouponMapper.insert(userCoupon);
            
            // 9. 记录秒杀日志
            SeckillLog seckillLog = new SeckillLog();
            seckillLog.setCouponId(couponId);
            seckillLog.setUserId(userId);
            seckillLog.setStatus(1);
            seckillLog.setSeckillTime(LocalDateTime.now());
            seckillLogMapper.insert(seckillLog);
            
            // 10. 标记用户已参与（防重）
            redisTemplate.opsForValue().set(userkey, "1", 24, TimeUnit.HOURS);
            logger.info("秒杀成功: userId={}, couponId={}", userId, couponId);
            return Result.success("抢券成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 补偿Redis库存
            compensateRedisStock(couponId);
            return Result.error(600, "系统异常");
        } catch (Exception e) {
            // 事务回滚时补偿Redis库存
            compensateRedisStock(couponId);
            logger.error("秒杀失败，回滚Redis库存: userId={}, couponId={}", userId, couponId, e);
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 补偿Redis库存
     */
    private void compensateRedisStock(Long couponId) {
        try {
            String stockKey = COUPON_STOCK_KEY + couponId;
            redisTemplate.opsForValue().increment(stockKey, 1);
            logger.info("Redis库存补偿成功: couponId={}", couponId);
        } catch (Exception e) {
            logger.error("Redis库存补偿失败: couponId={}", couponId, e);
        }
    }

    /**
     * Redis 预减库存（Lua脚本保证原子性）
     */
    private Long decreaseStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        // 如果Redis中没有库存，先从数据库加载
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))) {
                Coupon coupon = couponMapper.selectById(couponId);
                if (coupon != null) {
                    redisTemplate.opsForValue().set(stockKey, coupon.getAvailableStock(), 1, TimeUnit.DAYS);
                }
            }
            // Lua脚本：原子性减库存
//            Redis 保证 Lua 脚本在执行过程中不会被其他命令打断，就像数据库的事务一样。
//            分布式锁 + 普通操作（✅ 可用，但慢
            String luaScript = "local stock = redis.call('get', KEYS[1]) " +
                    "if not stock then return -1 end " +
                    "stock = tonumber(stock) " +
                    "if stock <= 0 then return -1 end " +
                    "redis.call('decr', KEYS[1]) " +
                    "return stock - 1";


//            local stock = redis.call('get', KEYS[1])      -- 第1步：获取库存
//            if not stock then return -1 end               -- 不存在返回-1
//            stock = tonumber(stock)                       -- 转换为数字
//            if stock <= 0 then return -1 end              -- 库存不足返回-1
//            redis.call('decr', KEYS[1])                   -- 第2步：扣减库存
//            return stock - 1                              -- 返回剩余库存

//          execute()：通过 RedisTemplate 执行原生 Redis 命令
            Long result = (Long) redisTemplate.execute(
//                    RedisCallback：底层连接回调，直接操作 Redis 连接
//                    eval()：执行 Lua 脚本，参数依次为脚本内容、返回类型、key 数量、key 值
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