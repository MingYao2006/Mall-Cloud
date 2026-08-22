package com.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("SELECT * FROM payment WHERE payment_no = #{paymentNo} AND is_deleted = 0")
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT * FROM payment WHERE order_no = #{orderNo} AND is_deleted = 0")
    Payment selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM payment WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Payment> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE payment SET status = #{status}, transaction_id = #{transactionId}, " +
            "payment_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int updatePaySuccess(@Param("id") Long id, @Param("status") Integer status, 
                         @Param("transactionId") String transactionId);

    @Update("UPDATE payment SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
