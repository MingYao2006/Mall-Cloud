package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Item.OrderItem;
import com.mall.common.vo.HotProductItem;
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

    /**
     * 商品热度排行：按有效订单（已支付 / 已发货 / 已完成）的累计销量倒序统计。
     * order_item 表本身没有逻辑删除字段，删除状态通过 orders.is_deleted 判定。
     */
    @Select("SELECT oi.product_id AS productId, oi.product_name AS productName, " +
            "oi.product_image AS productImage, " +
            "SUM(oi.quantity) AS quantity, IFNULL(SUM(oi.total_price), 0) AS amount " +
            "FROM order_item oi INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.is_deleted = 0 AND o.status IN (1, 2, 3) " +
            "GROUP BY oi.product_id, oi.product_name, oi.product_image " +
            "ORDER BY quantity DESC LIMIT #{limit}")
    List<HotProductItem> selectHotProducts(@Param("limit") Integer limit);
}