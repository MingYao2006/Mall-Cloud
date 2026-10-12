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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

        // Redis 预减库存是否已生效：只有生效过才需要补偿，否则异常路径会把库存越补越多
        AtomicBoolean redisStockDecreased = new AtomicBoolean(false);
        // 补偿出口守卫：CAS 保证一个请求最多回补一次，杜绝 catch 与 afterCompletion 叠加成双倍
        AtomicBoolean compensated = new AtomicBoolean(false);
        // 业务是否真正做成（DB 扣减 + 落券都成功）；事务是否落地由 afterCompletion 的 status 决定
        AtomicBoolean bizSuccess = new AtomicBoolean(false);
        boolean unlockAfterTransaction = false;

        try {
            // 不传 leaseTime，交给 Redisson 看门狗续期：固定 10 秒租约一过期，
            // 锁就在事务还没提交时被别人拿走，限领判定直接失效
            boolean tryLock = lock.tryLock(3, TimeUnit.SECONDS);
            if (!tryLock) {
                return Result.error(500, "系统繁忙，请稍后重试");
            }
            unlockAfterTransaction = finishAfterTransaction(lock, couponId,
                    redisStockDecreased, compensated, bizSuccess);

            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || coupon.getStatus() != 1) {
                return Result.error(600, "优惠券不存在或活动未开始");
            }
            if (coupon.getStartTime() == null || LocalDateTime.now().isBefore(coupon.getStartTime())) {
                return Result.error(600, "活动尚未开始");
            }
            if (coupon.getEndTime() == null || LocalDateTime.now().isAfter(coupon.getEndTime())) {
                return Result.error(600, "活动已结束");
            }

            Integer limitPerUser = coupon.getLimitPerUser();
            String userkey = SECKILL_USER_KEY + couponId + ":" + userId;
            // 「已参与」标记是布尔值，只能在每人限领 1 张时当快速拒绝用；
            // limitPerUser > 1 时它会误挡合法的第二次抢券，判定交给下面的 DB count
            boolean singlePerUser = limitPerUser != null && limitPerUser == 1;
            if (singlePerUser && Boolean.TRUE.equals(redisTemplate.hasKey(userkey))) {
                return Result.error(600, "您已参与过该活动");
            }

            if (limitPerUser != null && limitPerUser > 0) {
                Integer count = userCouponMapper.countByUserIdAndCouponId(userId, couponId);
                if (count != null && count >= limitPerUser) {
                    return Result.error(600, "已达到每人限抢数量");
                }
            }

            Long stock = decreaseStock(couponId);
            if (stock < 0) {
                return Result.error(600, "优惠券已抢完");
            }
            redisStockDecreased.set(true);

            int decreased = couponMapper.decreaseStock(couponId);
            if (decreased == 0) {
                // 这里不回补：Redis 已减而 DB 没扣，统一交给唯一出口按 !bizSuccess 补恰好一次
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
            
            if (singlePerUser) {
                redisTemplate.opsForValue().set(userkey, "1", 24, TimeUnit.HOURS);
            }
            logger.info("秒杀成功: userId={}, couponId={}", userId, couponId);
            bizSuccess.set(true);
            return Result.success("抢券成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("秒杀被中断: userId={}, couponId={}", userId, couponId, e);
            return Result.error(600, "系统异常");
        } catch (Exception e) {
            logger.error("秒杀失败: userId={}, couponId={}", userId, couponId, e);
            throw e;
        } finally {
            if (!unlockAfterTransaction) {
                // 当前没有事务同步可用（如内部自调用绕过代理）：先定库存再释放锁。
                // 这条分支不能按 +1 回补：没事务就没回滚，coupon 那次条件 UPDATE 已经永久生效，
                // +1 会凭空造出一个库存位，只能按 DB 权威值重建。
                if (redisStockDecreased.get() && !bizSuccess.get()) {
                    rebuildRedisStock(couponId);
                }
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 事务结束后统一收尾：先按最终结果决定要不要回补 Redis 库存，再释放分布式锁。
     * 两件事必须放在同一个 synchronization 里按顺序做，不能依赖多个 synchronization 之间的回调顺序；
     * 也不能把 unlock 放进 finally —— 那会早于 @Transactional 提交，下一个请求拿到锁时读不到
     * 上一个请求尚未提交的 user_coupon 行，限领判定就会失效。
     *
     * @return false 表示当前没有事务同步可用，由调用方在 finally 里自己走唯一出口
     */
    private boolean finishAfterTransaction(RLock lock, Long couponId,
                                           AtomicBoolean redisStockDecreased,
                                           AtomicBoolean compensated,
                                           AtomicBoolean bizSuccess) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                try {
                    compensateIfRequired(couponId, redisStockDecreased, compensated, bizSuccess, status);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        });
        return true;
    }

    /**
     * 库存补偿的唯一出口：只有「Redis 已减」且「这一个库存位最终没被消耗掉」时才回补，且靠 CAS 只补一次。
     */
    private void compensateIfRequired(Long couponId, AtomicBoolean redisStockDecreased,
                                      AtomicBoolean compensated, AtomicBoolean bizSuccess, int status) {
        if (!redisStockDecreased.get()) {
            return;
        }
        // 只有「业务成功 + 事务已提交」才等于库存真的被卖掉（STATUS_UNKNOWN 时提交结果不可知，不能算成功）
        if (status == TransactionSynchronization.STATUS_COMMITTED && bizSuccess.get()) {
            return;
        }
        if (status == TransactionSynchronization.STATUS_UNKNOWN) {
            // 提交阶段抛异常：DB 到底扣没扣无法判断，猜 +1 可能虚增，不猜——以 DB 为权威值重建 key，幂等收敛
            rebuildRedisStock(couponId);
            return;
        }
        compensateOnce(couponId, compensated);
    }

    /**
     * 回补 1 个库存，重复调用只会生效一次。
     */
    private void compensateOnce(Long couponId, AtomicBoolean compensated) {
        if (!compensated.compareAndSet(false, true)) {
            logger.warn("Redis库存已补偿过，跳过重复回补: couponId={}", couponId);
            return;
        }
        compensateRedisStock(couponId);
    }

    /**
     * 事务提交结果未知时按 DB 可用库存重建 Redis key，比盲猜方向安全。
     * 此刻分布式锁仍未释放，同活动的其他请求进不了临界区，重建期间不会被并发预减污染。
     */
    private void rebuildRedisStock(Long couponId) {
        try {
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null) {
                logger.error("Redis库存重建跳过，优惠券不存在: couponId={}", couponId);
                return;
            }
            redisTemplate.opsForValue().set(COUPON_STOCK_KEY + couponId,
                    coupon.getAvailableStock(), 1, TimeUnit.DAYS);
            logger.warn("Redis库存按 DB 权威值重建: couponId={}, stock={}", couponId, coupon.getAvailableStock());
        } catch (Exception e) {
            logger.error("Redis库存重建失败: couponId={}", couponId, e);
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