package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // ============================================================
    // BaseMapper<Order> 已自动提供（可安全删除下面的手写版本）：
    //   insert(Order)        → INSERT INTO orders(...)
    //   selectById(Long)     → SELECT * FROM orders WHERE id=? AND is_deleted=0
    //   deleteById(Long)     → UPDATE orders SET is_deleted=1 WHERE id=? (@TableLogic)
    //

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND is_deleted = 0")
    Order selectByOrderNo(String orderNo);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status = #{status} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT * FROM orders WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectAll();

    @Update("UPDATE orders SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE orders SET status = 1, pay_amount = #{payAmount}, payment_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int payOrder(@Param("id") Long id, @Param("payAmount") java.math.BigDecimal payAmount);

    @Update("UPDATE orders SET status = 2, delivery_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int deliverOrder(Long id);

    @Update("UPDATE orders SET status = 3, receive_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int receiveOrder(Long id);

    @Update("UPDATE orders SET status = 4, update_time = NOW() WHERE id = #{id}")
    int cancelOrder(Long id);
}
