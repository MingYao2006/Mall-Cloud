package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.SeckillLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀日志数据访问接口
 * 继承 BaseMapper 后自动拥有以下方法：
 * - insert(T entity)：插入一条记录
 * - deleteById(Serializable id)：根据 ID 删除
 * - updateById(T entity)：根据 ID 更新
 * - selectById(Serializable id)：根据 ID 查询
 * - selectList(Wrapper<T> queryWrapper)：条件查询列表
 * - selectCount(Wrapper<T> queryWrapper)：条件查询总数
 * - selectPage(Page<T> page, Wrapper<T> queryWrapper)：分页查询
 */
@Mapper
public interface SeckillLogMapper extends BaseMapper<SeckillLog> {

    /**
     * 根据用户ID查询秒杀记录
     * @param userId 用户ID
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE user_id = #{userId} ORDER BY seckill_time DESC")
    List<SeckillLog> findByUserId(@Param("userId") Long userId);

    /**
     * 根据优惠券ID查询秒杀记录
     * @param couponId 优惠券ID
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE coupon_id = #{couponId} ORDER BY seckill_time DESC")
    List<SeckillLog> findByCouponId(@Param("couponId") Long couponId);

    /**
     * 根据用户ID和优惠券ID查询秒杀记录
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE user_id = #{userId} AND coupon_id = #{couponId} ORDER BY seckill_time DESC")
    List<SeckillLog> findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 根据状态查询秒杀记录
     * @param status 状态（0-失败 1-成功 2-待处理）
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE status = #{status} ORDER BY seckill_time DESC")
    List<SeckillLog> findByStatus(@Param("status") Integer status);

    /**
     * 查询指定时间范围内的秒杀记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE seckill_time BETWEEN #{startTime} AND #{endTime} ORDER BY seckill_time DESC")
    List<SeckillLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计某优惠券的秒杀成功次数
     * @param couponId 优惠券ID
     * @return 成功次数
     */
    @Select("SELECT COUNT(*) FROM seckill_log WHERE coupon_id = #{couponId} AND status = 1")
    Integer countSuccessByCouponId(@Param("couponId") Long couponId);

    /**
     * 统计某优惠券的秒杀失败次数
     * @param couponId 优惠券ID
     * @return 失败次数
     */
    @Select("SELECT COUNT(*) FROM seckill_log WHERE coupon_id = #{couponId} AND status = 0")
    Integer countFailByCouponId(@Param("couponId") Long couponId);

    /**
     * 统计用户在某优惠券活动中的参与次数
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 参与次数
     */
    @Select("SELECT COUNT(*) FROM seckill_log WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    Integer countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 查询用户最近一次的秒杀记录
     * @param userId 用户ID
     * @return 秒杀日志
     */
    @Select("SELECT * FROM seckill_log WHERE user_id = #{userId} ORDER BY seckill_time DESC LIMIT 1")
    SeckillLog findLatestByUserId(@Param("userId") Long userId);

    /**
     * 查询用户成功的秒杀记录
     * @param userId 用户ID
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE user_id = #{userId} AND status = 1 ORDER BY seckill_time DESC")
    List<SeckillLog> findSuccessByUserId(@Param("userId") Long userId);

    /**
     * 查询用户失败的秒杀记录
     * @param userId 用户ID
     * @return 秒杀日志列表
     */
    @Select("SELECT * FROM seckill_log WHERE user_id = #{userId} AND status = 0 ORDER BY seckill_time DESC")
    List<SeckillLog> findFailByUserId(@Param("userId") Long userId);

    /**
     * 统计某优惠券在指定时间范围内的秒杀次数
     * @param couponId 优惠券ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 秒杀次数
     */
    @Select("SELECT COUNT(*) FROM seckill_log WHERE coupon_id = #{couponId} AND seckill_time BETWEEN #{startTime} AND #{endTime}")
    Integer countByCouponIdAndTimeRange(@Param("couponId") Long couponId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}