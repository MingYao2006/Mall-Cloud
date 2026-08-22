package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Item.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Insert("<script>" +
            "INSERT INTO order_item(order_id, order_no, product_id, product_name, product_image, " +
            "product_price, quantity, total_price, create_time, update_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.orderId}, #{item.orderNo}, #{item.productId}, #{item.productName}, #{item.productImage}, " +
            "#{item.productPrice}, #{item.quantity}, #{item.totalPrice}, #{item.createTime}, #{item.updateTime})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<OrderItem> orderItems);

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);

    @Select("SELECT * FROM order_item WHERE order_no = #{orderNo}")
    List<OrderItem> selectByOrderNo(String orderNo);
}