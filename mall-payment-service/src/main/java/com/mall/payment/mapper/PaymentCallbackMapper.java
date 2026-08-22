package com.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.payment.entity.PaymentCallback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentCallbackMapper extends BaseMapper<PaymentCallback> {

    @Select("SELECT * FROM payment_callback WHERE payment_no = #{paymentNo} AND is_deleted = 0 ORDER BY create_time DESC")
    List<PaymentCallback> selectByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT * FROM payment_callback WHERE payment_no = #{paymentNo} AND callback_status = 1 AND is_deleted = 0 LIMIT 1")
    PaymentCallback selectSuccessByPaymentNo(@Param("paymentNo") String paymentNo);
}
