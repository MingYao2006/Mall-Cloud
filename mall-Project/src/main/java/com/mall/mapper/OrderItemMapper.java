package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Item.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    // ============================================================
    // 保留以下自定义方法（BaseMapper 没有对应功能）
    // ============================================================

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

    @Select("SELECT * FROM order_item WHERE product_id = #{productId}")
    List<OrderItem> selectByProductId(Long productId);

    @Delete("DELETE FROM order_item WHERE order_id = #{orderId}")
    int deleteByOrderId(Long orderId);
}
