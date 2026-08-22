package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户优惠券数据访问接口
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
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 根据用户ID查询所有优惠券
     * @param userId 用户ID
     * @return 用户优惠券列表
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} ORDER BY obtain_time DESC")
    List<UserCoupon> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和状态查询优惠券
     * @param userId 用户ID
     * @param status 状态（0-未使用 1-已使用 2-已过期）
     * @return 用户优惠券列表
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = #{status} ORDER BY obtain_time DESC")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询用户可用的优惠券（未使用且未过期）
     * @param userId 用户ID
     * @param currentTime 当前时间
     * @return 可用的用户优惠券列表
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 0 AND expire_time > #{currentTime} ORDER BY expire_time ASC")
    List<UserCoupon> findAvailableByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据优惠券ID查询所有领取记录
     * @param couponId 优惠券ID
     * @return 用户优惠券列表
     */
    @Select("SELECT * FROM user_coupon WHERE coupon_id = #{couponId} ORDER BY obtain_time DESC")
    List<UserCoupon> findByCouponId(@Param("couponId") Long couponId);

    /**
     * 检查用户是否已领取某优惠券
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 用户优惠券对象（存在则返回，不存在则返回null）
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND coupon_id = #{couponId} LIMIT 1")
    UserCoupon findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 使用优惠券（更新状态为已使用）
     * @param id 用户优惠券ID
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Update("UPDATE user_coupon SET status = 1, order_id = #{orderId}, use_time = NOW() WHERE id = #{id} AND status = 0")
    int useCoupon(@Param("id") Long id, @Param("orderId") Long orderId);

    /**
     * 批量更新过期的优惠券状态
     * @param currentTime 当前时间
     * @return 影响行数
     */
    @Update("UPDATE user_coupon SET status = 2 WHERE status = 0 AND expire_time < #{currentTime}")
    int updateExpiredCoupons(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 统计用户领取某优惠券的数量
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 领取数量
     */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    Integer countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 统计用户优惠券数量（按状态）
     * @param userId 用户ID
     * @param status 状态
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND status = #{status}")
    Integer countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询即将过期的用户优惠券（指定天数内）
     * @param userId 用户ID
     * @param days 天数
     * @return 用户优惠券列表
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 0 AND expire_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL #{days} DAY) ORDER BY expire_time ASC")
    List<UserCoupon> findExpiringSoon(@Param("userId") Long userId, @Param("days") Integer days);
}
