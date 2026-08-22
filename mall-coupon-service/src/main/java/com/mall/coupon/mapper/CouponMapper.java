package com.mall.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券数据访问接口
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 根据优惠券名称查询
     */
    @Select("SELECT * FROM coupon WHERE name = #{name}")
    Coupon findByName(@Param("name") String name);

    /**
     * 查询所有有效的优惠券
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND available_stock > 0 AND end_time > NOW() ORDER BY create_time DESC")
    List<Coupon> findAvailableCoupons();

    /**
     * 根据优惠券类型查询
     */
    @Select("SELECT * FROM coupon WHERE type = #{type} AND status = 1 ORDER BY create_time DESC")
    List<Coupon> findByType(@Param("type") Integer type);

    /**
     * 查询指定时间范围内有效的优惠券
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND available_stock > 0 AND start_time <= #{currentTime} AND end_time >= #{currentTime} ORDER BY create_time DESC")
    List<Coupon> findValidCoupons(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 扣减优惠券库存（使用乐观锁）
     */
    @Update("UPDATE coupon SET available_stock = available_stock - 1, update_time = NOW() WHERE id = #{couponId} AND available_stock > 0")
    int decreaseStock(@Param("couponId") Long couponId);

    /**
     * 增加优惠券库存（回滚库存）
     */
    @Update("UPDATE coupon SET available_stock = available_stock + 1, update_time = NOW() WHERE id = #{couponId}")
    int increaseStock(@Param("couponId") Long couponId);

    /**
     * 更新优惠券状态
     */
    @Update("UPDATE coupon SET status = #{status}, update_time = NOW() WHERE id = #{couponId}")
    int updateStatus(@Param("couponId") Long couponId, @Param("status") Integer status);

    /**
     * 查询即将过期的优惠券
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND end_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL #{days} DAY) ORDER BY end_time ASC")
    List<Coupon> findExpiringSoon(@Param("days") Integer days);
}