package com.mall.seckill.service;

import com.mall.common.BusinessException;
import com.mall.seckill.config.RabbitMQConfig;
import com.mall.seckill.entity.Coupon;
import com.mall.seckill.entity.SeckillLog;
import com.mall.seckill.entity.UserCoupon;
import com.mall.seckill.mapper.CouponMapper;
import com.mall.seckill.mapper.SeckillLogMapper;
import com.mall.seckill.mapper.UserCouponMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SeckillService 核心链路的纯单元测试（不依赖任何中间件，mvn test 可直接跑）。
 * 用例全部来自真实踩过的坑：过期活动白扣库存、限领标记误挡、双重回补、
 * DB 扣减失败时按权威值重建而非盲补 +1、MQ 幂等键缺失等。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class SeckillServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long COUPON_ID = 100L;
    private static final String LIMIT_KEY = "seckill:limit:" + USER_ID;
    private static final String STOCK_KEY = "seckill:stock:" + COUPON_ID;
    private static final String USER_KEY = "seckill:user:" + COUPON_ID + ":" + USER_ID;
    private static final String LOCK_KEY = "seckill:lock:" + COUPON_ID;
    private static final String MSG_ID = "test-message-id";
    private static final String SECKILL_RESULT_KEY = SeckillService.SECKILL_RESULT_KEY;

    @Mock
    private RedisTemplate redisTemplate;
    @Mock
    private ValueOperations valueOperations;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock rLock;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private SeckillLogMapper seckillLogMapper;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SeckillService seckillService;

    /** 构造一张「进行中、每人限领 1 张」的活动券 */
    private Coupon activeCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(COUPON_ID);
        coupon.setStatus(1);
        coupon.setLimitPerUser(1);
        coupon.setAvailableStock(42);
        coupon.setStartTime(LocalDateTime.now().minusHours(1));
        coupon.setEndTime(LocalDateTime.now().plusHours(1));
        return coupon;
    }

    /** 放行限流 + 拿到分布式锁，返回后代码可走到库存判定 */
    private void passRateLimitAndLock(long requestCount) throws InterruptedException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(LIMIT_KEY, 1L)).thenReturn(requestCount);
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(rLock);
        when(rLock.tryLock(3L, TimeUnit.SECONDS)).thenReturn(true);
    }

    @Test
    @DisplayName("参数为 null 直接拒绝，绝不触碰 Redis 与数据库")
    void nullParamShouldFailFast() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> seckillService.seckillCoupon(null, COUPON_ID));

        assertEquals(400, e.getCode());
        verifyNoInteractions(redisTemplate, redissonClient, couponMapper, userCouponMapper);
    }

    @Test
    @DisplayName("60 秒内超过 10 次请求触发限流 429，不进入加锁与扣库存")
    void rateLimitExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(LIMIT_KEY, 1L)).thenReturn(11L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> seckillService.seckillCoupon(USER_ID, COUPON_ID));

        assertEquals(429, e.getCode());
        verifyNoInteractions(redissonClient, couponMapper);
    }

    @Test
    @DisplayName("活动已结束：必须在扣库存之前拦截，Redis 库存一分不减")
    void endedActivityMustNotConsumeStock() throws InterruptedException {
        Coupon coupon = activeCoupon();
        coupon.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(couponMapper.selectById(COUPON_ID)).thenReturn(coupon);
        passRateLimitAndLock(1L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> seckillService.seckillCoupon(USER_ID, COUPON_ID));

        assertEquals(600, e.getCode());
        // 核心断言：Lua 预减从未执行，DB 条件 UPDATE 也从未执行
        verify(redisTemplate, never()).execute(any(RedisCallback.class));
        verify(couponMapper, never()).decreaseStock(anyLong());
    }

    @Test
    @DisplayName("每人限领 1 张且已参与过：快速拒绝，不查库存")
    void singlePerUserAlreadyParticipated() throws InterruptedException {
        when(couponMapper.selectById(COUPON_ID)).thenReturn(activeCoupon());
        passRateLimitAndLock(1L);
        when(redisTemplate.hasKey(USER_KEY)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> seckillService.seckillCoupon(USER_ID, COUPON_ID));

        assertEquals(600, e.getCode());
        assertEquals("您已参与过该活动", e.getMessage());
        verify(redisTemplate, never()).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("Redis 库存已抢完（Lua 返回 -1）：不再做 DB 扣减")
    void redisStockSoldOut() throws InterruptedException {
        when(couponMapper.selectById(COUPON_ID)).thenReturn(activeCoupon());
        passRateLimitAndLock(1L);
        when(redisTemplate.hasKey(USER_KEY)).thenReturn(false);
        when(userCouponMapper.countByUserIdAndCouponId(USER_ID, COUPON_ID)).thenReturn(0);
        when(redisTemplate.hasKey(STOCK_KEY)).thenReturn(true);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(-1L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> seckillService.seckillCoupon(USER_ID, COUPON_ID));

        assertEquals(600, e.getCode());
        assertEquals("优惠券已抢完", e.getMessage());
        verify(couponMapper, never()).decreaseStock(anyLong());
    }

    @Test
    @DisplayName("正常抢到：落券 + 写日志 + 标记已参与，锁在收尾时释放，且绝不回补库存")
    void seckillSuccess() throws InterruptedException {
        when(couponMapper.selectById(COUPON_ID)).thenReturn(activeCoupon());
        passRateLimitAndLock(1L);
        when(redisTemplate.hasKey(USER_KEY)).thenReturn(false);
        when(userCouponMapper.countByUserIdAndCouponId(USER_ID, COUPON_ID)).thenReturn(0);
        when(redisTemplate.hasKey(STOCK_KEY)).thenReturn(true);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(4L);
        when(couponMapper.decreaseStock(COUPON_ID)).thenReturn(1);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        seckillService.seckillCoupon(USER_ID, COUPON_ID);

        verify(userCouponMapper).insert(any(UserCoupon.class));
        verify(seckillLogMapper).insert(any(SeckillLog.class));
        // 限领 1 张：24 小时参与标记必须写上
        verify(valueOperations).set(eq(USER_KEY), eq("1"), eq(24L), eq(TimeUnit.HOURS));
        verify(rLock).unlock();
        // 成功后库存位已被真实消耗，不许出现任何 +1 回补
        verify(valueOperations, never()).increment(eq(STOCK_KEY), anyLong());
    }

    @Test
    @DisplayName("Redis 减了但 DB 扣减返回 0：按 DB 权威值重建 key，而不是盲补 +1（双补回归测试）")
    void dbDecreaseFailedRebuildsStockNotBlindCompensate() throws InterruptedException {
        Coupon coupon = activeCoupon(); // availableStock = 42
        when(couponMapper.selectById(COUPON_ID)).thenReturn(coupon);
        passRateLimitAndLock(1L);
        when(redisTemplate.hasKey(USER_KEY)).thenReturn(false);
        when(userCouponMapper.countByUserIdAndCouponId(USER_ID, COUPON_ID)).thenReturn(0);
        when(redisTemplate.hasKey(STOCK_KEY)).thenReturn(true);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(3L);
        when(couponMapper.decreaseStock(COUPON_ID)).thenReturn(0);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> seckillService.seckillCoupon(USER_ID, COUPON_ID));

        assertEquals(500, e.getCode());
        // 无事务可用的分支下：唯一出口按 DB 权威值 42 重建 key
        verify(valueOperations).set(eq(STOCK_KEY), eq(42), eq(1L), eq(TimeUnit.DAYS));
        // 且绝不能走 increment 盲补，否则就是曾经的双补 bug
        verify(valueOperations, never()).increment(eq(STOCK_KEY), anyLong());
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    @DisplayName("MQ 消息必须携带幂等键 messageId，且返回值与消息体一致（供前端轮询结果）")
    void sendSeckillMessageCarriesMessageId() {
        String returnedId = seckillService.sendSeckillMessage(USER_ID, COUPON_ID);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.SECKILL_EXCHANGE),
                eq(RabbitMQConfig.SECKILL_ROUTING_KEY),
                captor.capture());

        Map message = captor.getValue();
        assertEquals(USER_ID, message.get("userId"));
        assertEquals(COUPON_ID, message.get("couponId"));
        assertNotNull(message.get("messageId"), "缺少幂等键，MQ 至少投递一次会导致重复发货");
        assertNotEquals("", message.get("messageId"));
        // 入口返回的 messageId 必须就是消息体里那个，否则前端拿着它查不到结果
        assertEquals(message.get("messageId"), returnedId, "返回值与消息体的 messageId 不一致");
    }

    @Test
    @DisplayName("抢券结果：未产生时返回 null，已产生时可读回")
    void seckillResultRoundTrip() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(SECKILL_RESULT_KEY + MSG_ID)).thenReturn(null);
        assertNull(seckillService.getSeckillResult(MSG_ID), "结果未产生时应返回 null，让前端继续轮询");

        Map<String, Object> stored = new HashMap<>();
        stored.put("status", "SUCCESS");
        stored.put("message", "恭喜，抢券成功！");
        when(valueOperations.get(SECKILL_RESULT_KEY + MSG_ID)).thenReturn(stored);

        Map<String, Object> got = seckillService.getSeckillResult(MSG_ID);
        assertNotNull(got);
        assertEquals("SUCCESS", got.get("status"));
    }

    @Test
    @DisplayName("抢券结果：写入失败不能影响主流程（券已发出去，不能让异常冒泡）")
    void saveSeckillResultSwallowsRedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(any(), any(), anyLong(), any(TimeUnit.class));

        assertDoesNotThrow(() -> seckillService.saveSeckillResult(MSG_ID, true, "ok"));
    }
}
