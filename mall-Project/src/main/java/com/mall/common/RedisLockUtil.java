package com.mall.common;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类（基于Redisson）
 */
@Component
public class RedisLockUtil {
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取锁（阻塞等待，自动续期）
     * <p>会一直阻塞直到获取锁，锁会自动续期（看门狗机制）</p>
     * <p>风险：如果业务执行时间过长或线程卡死，锁不会自动释放</p>
     *
     * @param lockKey 锁的key
     * @return RLock 锁对象，需要手动unlock
     */
    public RLock lock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        return lock;
    }

    /**
     * 获取锁（阻塞等待，指定过期时间）
     * <p>会一直阻塞直到获取锁，锁在指定时间后自动释放</p>
     * <p>不会自动续期，防止死锁</p>
     *
     * @param lockKey   锁的key
     * @param leaseTime 锁自动释放时间
     * @param unit      时间单位
     * @return RLock 锁对象，需要手动unlock
     */
    public RLock lock(String lockKey, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(leaseTime, unit);
        return lock;
    }

    /**
     * 尝试获取锁（立即返回，不等待）
     * <p>尝试获取锁，获取失败立即返回false</p>
     *
     * @param lockKey 锁的key
     * @return 获取成功返回true，失败返回false
     */
    public boolean tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock();
    }

    /**
     * 尝试获取锁（带等待时间和过期时间）
     * <p>在等待时间内尝试获取锁，获取成功后锁在指定时间后自动释放</p>
     * <p>推荐使用此方法，既能控制等待时间，又能防止死锁</p>
     *
     * @param lockKey   锁的key
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param unit      时间单位
     * @return 获取成功返回true，失败返回false
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 释放锁
     *
     * @param lock 锁对象
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 判断当前线程是否持有锁
     *
     * @param lockKey 锁的key
     * @return 是否持有锁
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 判断锁是否被任意线程持有
     *
     * @param lockKey 锁的key
     * @return 是否被持有
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}
