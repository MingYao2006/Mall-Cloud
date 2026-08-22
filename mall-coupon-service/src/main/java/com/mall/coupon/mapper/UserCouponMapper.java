package com.mall.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户优惠券数据访问接口
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 根据用户ID查询所有优惠券
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} ORDER BY obtain_time DESC")
    List<UserCoupon> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和状态查询优惠券
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = #{status} ORDER BY obtain_time DESC")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询用户可用的优惠券
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 0 AND expire_time > #{currentTime} ORDER BY expire_time ASC")
    List<UserCoupon> findAvailableByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据优惠券ID查询所有领取记录
     */
    @Select("SELECT * FROM user_coupon WHERE coupon_id = #{couponId} ORDER BY obtain_time DESC")
    List<UserCoupon> findByCouponId(@Param("couponId") Long couponId);

    /**
     * 检查用户是否已领取某优惠券
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND coupon_id = #{couponId} LIMIT 1")
    UserCoupon findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 使用优惠券
     */
    @Update("UPDATE user_coupon SET status = 1, order_id = #{orderId}, use_time = NOW() WHERE id = #{id} AND status = 0")
    int useCoupon(@Param("id") Long id, @Param("orderId") Long orderId);

    /**
     * 批量更新过期的优惠券状态
     */
    @Update("UPDATE user_coupon SET status = 2 WHERE status = 0 AND expire_time < #{currentTime}")
    int updateExpiredCoupons(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 统计用户领取某优惠券的数量
     */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    Integer countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 统计用户优惠券数量（按状态）
     */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND status = #{status}")
    Integer countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询即将过期的用户优惠券
     */
    @Select("SELECT * FROM user_coupon WHERE user_id = #{userId} AND status = 0 AND expire_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL #{days} DAY) ORDER BY expire_time ASC")
    List<UserCoupon> findExpiringSoon(@Param("userId") Long userId, @Param("days") Integer days);
}