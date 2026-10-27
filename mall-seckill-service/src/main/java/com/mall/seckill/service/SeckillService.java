package com.mall.seckill.service;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.seckill.config.RabbitMQConfig;
import com.mall.seckill.entity.Coupon;
import com.mall.seckill.entity.SeckillLog;
import com.mall.seckill.entity.UserCoupon;
import com.mall.seckill.mapper.CouponMapper;
import com.mall.seckill.mapper.SeckillLogMapper;
import com.mall.seckill.mapper.UserCouponMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SeckillService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillService.class);
    private static final String COUPON_STOCK_KEY = "seckill:stock:";
    private static final String SECKILL_USER_KEY = "seckill:user:";
    private static final String SECKILL_LOCK_KEY = "seckill:lock:";
    /** 抢券结果 key 前缀：seckill:result:{messageId} */
    public static final String SECKILL_RESULT_KEY = "seckill:result:";
    /** 结果保留时长（秒）：够前端轮询拿结果即可，不必长期占内存 */
    public static final long RESULT_TTL_SECONDS = 300;

    private static final long CACHE_TIMEOUT = 30; // 30分钟基础过期时间
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀消息到队列（异步处理）
     *
     * @return 本次请求的 messageId，前端拿它去轮询抢券结果
     */
    public String sendSeckillMessage(Long userId,Long couponId){
        // 封装消息数据
        Map<String, Object> message=new HashMap<>();
        message.put("userId",userId);
        message.put("couponId",couponId);
        // 幂等键：MQ 是至少投递一次，重复投递靠它在消费端去重
        String messageId = UUID.randomUUID().toString();
        message.put("messageId", messageId);
        // 发送消息
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,      // 交换机
                RabbitMQConfig.SECKILL_ROUTING_KEY,   // 路由键
                message                               // 消息内容
        );
        logger.info("秒杀信息发送成功: userId={}, couponId={}, messageId={}", userId, couponId, messageId);
        return messageId;
    }

    /**
     * 记录抢券结果，供前端轮询查询。
     * 只在消费端调用；TTL 到点自动清理，不需要额外补偿。
     */
    public void saveSeckillResult(String messageId, boolean success, String message) {
        if (messageId == null) {
            return;
        }
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", success ? "SUCCESS" : "FAIL");
            result.put("message", message);
            redisTemplate.opsForValue().set(SECKILL_RESULT_KEY + messageId, result,
                    RESULT_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 结果写失败不能影响主流程：用户只是查不到结果，券已经发了
            logger.warn("写入秒杀结果失败: messageId={}, 错误: {}", messageId, e.getMessage());
        }
    }

    /**
     * 查询抢券结果。
     * <p>
     * 返回类型用 Object 而不是 Map&lt;String,Object&gt;：Redis 的 Jackson 序列化器带类型信息，
     * 读回来是 HashMap，强转成具体泛型只会多一次 unchecked 警告，这里不做无谓的转换。
     *
     * @return null 表示结果尚未产生（还在排队/处理中），前端应继续轮询
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSeckillResult(String messageId) {
        if (messageId == null) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(SECKILL_RESULT_KEY + messageId);
            // 只接受 Map，防止历史脏数据或类型漂移导致 ClassCastException
            return value instanceof Map ? (Map<String, Object>) value : null;
        } catch (Exception e) {
            logger.warn("查询秒杀结果失败: messageId={}, 错误: {}", messageId, e.getMessage());
            return null;
        }
    }

    /**
     * 秒杀抢券 - 核心方法
     * 使用 Redis + 分布式锁 + 数据库乐观锁 三层防护
     */
    @Transactional(rollbackFor = Exception.class)
    public void seckillCoupon(Long userId, Long couponId) {
        // 1. 基础参数校验
        if (userId == null || couponId == null) {
            throw new BusinessException(400, "参数错误");
        }
        
        // 2. 用户限流 - 防止恶意刷接口
        String limitKey = "seckill:limit:" + userId;
        Long requestCount = redisTemplate.opsForValue().increment(limitKey, 1);
        if (requestCount == 1) {
            redisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        }
        if (requestCount > RATE_LIMIT_THRESHOLD) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }
        
        // 3. 分布式锁 - 防止并发超卖
        String rediskey = SECKILL_LOCK_KEY + couponId;
        RLock lock = redissonClient.getLock(rediskey);

        // Redis 预减库存是否已生效：只有生效过才需要补偿，否则异常路径会把库存越补越多
        AtomicBoolean redisStockDecreased = new AtomicBoolean(false);
        // 补偿出口守卫：CAS 保证一个请求最多回补一次，杜绝 catch 与 afterCompletion 叠加成双倍
        AtomicBoolean compensated = new AtomicBoolean(false);
        // 业务是否真正做成（DB 扣减 + 落券都成功）；事务是否落地由 afterCompletion 的 status 决定，
        // 两个条件合起来才等于「这一个库存位真的被消耗掉了」
        AtomicBoolean bizSuccess = new AtomicBoolean(false);
        boolean unlockAfterTransaction = false;

        try {
            // 不传 leaseTime，交给 Redisson 看门狗续期：固定 10 秒租约一过期，
            // 锁就在事务还没提交时被别人拿走，限领判定直接失效
            boolean tryLock = lock.tryLock(3, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
            unlockAfterTransaction = finishAfterTransaction(lock, couponId,
                    redisStockDecreased, compensated, bizSuccess);

            // 4. 查询优惠券信息，校验状态与活动时间（必须在任何库存扣减之前，
            //    否则过期活动会先吃掉一份 Redis 库存再抛异常）
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || coupon.getStatus() != 1) {
                throw new BusinessException(600, "优惠券不存在或活动未开始");
            }
            if (coupon.getStartTime() == null || LocalDateTime.now().isBefore(coupon.getStartTime())) {
                throw new BusinessException(600, "活动尚未开始");
            }
            if (coupon.getEndTime() == null || LocalDateTime.now().isAfter(coupon.getEndTime())) {
                throw new BusinessException(600, "活动已结束");
            }

            // 5. 每人限抢数量校验
            Integer limitPerUser = coupon.getLimitPerUser();
            String userkey = SECKILL_USER_KEY + couponId + ":" + userId;
            // 「已参与」标记是布尔值，只能在每人限领 1 张时当快速拒绝用；
            // limitPerUser > 1 时它会误挡合法的第二次抢券，判定交给下面的 DB count
            boolean singlePerUser = limitPerUser != null && limitPerUser == 1;
            if (singlePerUser && Boolean.TRUE.equals(redisTemplate.hasKey(userkey))) {
                throw new BusinessException(600, "您已参与过该活动");
            }

            if (limitPerUser != null && limitPerUser > 0) {
                Integer count = userCouponMapper.countByUserIdAndCouponId(userId, couponId);
                if (count != null && count >= limitPerUser) {
                    throw new BusinessException(600, "已达到每人限抢数量");
                }
            }

            // 6. Redis 预减库存（Lua脚本保证原子性）
            Long stock = decreaseStock(couponId);
            if (stock < 0) {
                throw new BusinessException(600, "优惠券已抢完");
            }
            redisStockDecreased.set(true);

            // 7. 数据库扣减库存（原子 UPDATE）
            int decreased = couponMapper.decreaseStock(couponId);
            if (decreased == 0) {
                // 这里不回补：补偿动作全权交给唯一出口，按 !bizSuccess 补恰好一次
                throw new BusinessException(500, "抢券失败，请重试");
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
            
            // 10. 每人限领 1 张时标记已参与
            if (singlePerUser) {
                redisTemplate.opsForValue().set(userkey, "1", 24, TimeUnit.HOURS);
            }
            bizSuccess.set(true);
            logger.info("秒杀成功: userId={}, couponId={}", userId, couponId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("秒杀被中断: userId={}, couponId={}", userId, couponId, e);
            throw new BusinessException(500, "系统异常");
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
            throw new BusinessException(500, "预减库存失败");
        }
    }
}