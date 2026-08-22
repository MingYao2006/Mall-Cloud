package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券数据访问接口
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
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 根据优惠券名称查询
     * @param name 优惠券名称
     * @return 优惠券对象
     */
    @Select("SELECT * FROM coupon WHERE name = #{name}")
    Coupon findByName(@Param("name") String name);

    /**
     * 查询所有有效的优惠券
     * @return 优惠券列表
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND available_stock > 0 AND end_time > NOW() ORDER BY create_time DESC")
    List<Coupon> findAvailableCoupons();

    /**
     * 根据优惠券类型查询
     * @param type 优惠券类型（1-满减 2-折扣 3-无门槛）
     * @return 优惠券列表
     */
    @Select("SELECT * FROM coupon WHERE type = #{type} AND status = 1 ORDER BY create_time DESC")
    List<Coupon> findByType(@Param("type") Integer type);

    /**
     * 查询指定时间范围内有效的优惠券
     * @param currentTime 当前时间
     * @return 优惠券列表
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND available_stock > 0 AND start_time <= #{currentTime} AND end_time >= #{currentTime} ORDER BY create_time DESC")
    List<Coupon> findValidCoupons(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 扣减优惠券库存（使用乐观锁）
     * @param couponId 优惠券ID
     * @return 影响行数（0表示扣减失败，1表示扣减成功）
     */
    @Update("UPDATE coupon SET available_stock = available_stock - 1, update_time = NOW() WHERE id = #{couponId} AND available_stock > 0")
    int decreaseStock(@Param("couponId") Long couponId);

    /**
     * 增加优惠券库存（回滚库存）
     * @param couponId 优惠券ID
     * @return 影响行数
     */
    @Update("UPDATE coupon SET available_stock = available_stock + 1, update_time = NOW() WHERE id = #{couponId}")
    int increaseStock(@Param("couponId") Long couponId);

    /**
     * 更新优惠券状态
     * @param couponId 优惠券ID
     * @param status 状态（0-禁用 1-启用）
     * @return 影响行数
     */
    @Update("UPDATE coupon SET status = #{status}, update_time = NOW() WHERE id = #{couponId}")
    int updateStatus(@Param("couponId") Long couponId, @Param("status") Integer status);

    /**
     * 查询即将过期的优惠券（指定天数内）
     * @param days 天数
     * @return 优惠券列表
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND end_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL #{days} DAY) ORDER BY end_time ASC")
    List<Coupon> findExpiringSoon(@Param("days") Integer days);

    /**
     * 查询库存不足的优惠券
     * @param threshold 库存阈值
     * @return 优惠券列表
     */
    @Select("SELECT * FROM coupon WHERE status = 1 AND available_stock <= #{threshold} ORDER BY available_stock ASC")
    List<Coupon> findLowStock(@Param("threshold") Integer threshold);

    /**
     * 根据金额查询可用的满减优惠券
     * @param amount 订单金额
     * @return 可用的满减优惠券列表
     */
    @Select("SELECT * FROM coupon WHERE type = 1 AND status = 1 AND available_stock > 0 AND min_amount <= #{amount} AND start_time <= NOW() AND end_time >= NOW() ORDER BY value DESC")
    List<Coupon> findUsableFullReductionCoupons(@Param("amount") java.math.BigDecimal amount);

    /**
     * 统计优惠券总领取数量
     * @param couponId 优惠券ID
     * @return 领取数量
     */
    @Select("SELECT (total_stock - available_stock) FROM coupon WHERE id = #{couponId}")
    Integer countClaimed(@Param("couponId") Long couponId);
}
