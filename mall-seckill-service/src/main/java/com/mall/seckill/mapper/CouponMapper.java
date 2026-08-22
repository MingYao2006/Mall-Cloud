package com.mall.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.seckill.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 扣减优惠券库存（使用乐观锁）
     */
    @Update("UPDATE coupon SET available_stock = available_stock - 1, update_time = NOW() WHERE id = #{couponId} AND available_stock > 0")
    int decreaseStock(@Param("couponId") Long couponId);
}
