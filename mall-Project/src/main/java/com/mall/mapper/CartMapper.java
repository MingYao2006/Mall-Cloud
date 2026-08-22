package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Cart;
import com.mall.entity.vo.CartVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    // ============================================================
    // BaseMapper<Cart> 已自动提供以下方法，无需手写：
    //   insert(Cart)          → INSERT INTO cart(...)
    //   selectById(Long)      → SELECT * FROM cart WHERE id=?
    //   updateById(Cart)      → UPDATE cart SET ... WHERE id=?
    //   deleteById(Long)      → DELETE FROM cart WHERE id=?
    //   selectList(Wrapper)   → SELECT * FROM cart (带条件)
    // ============================================================

    @Select("SELECT * FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    Cart selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Update("UPDATE cart SET quantity = #{quantity}, update_time = NOW() WHERE user_id = #{userId} AND product_id = #{productId}")
    int updateQuantity(@Param("userId") Long userId, @Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE cart SET selected = #{selected}, update_time = NOW() WHERE user_id = #{userId} AND product_id = #{productId}")
    int updateSelected(@Param("userId") Long userId, @Param("productId") Long productId, @Param("selected") Integer selected);

    @Update("UPDATE cart SET selected = #{selected}, update_time = NOW() WHERE user_id = #{userId}")
    int updateAllSelected(@Param("userId") Long userId, @Param("selected") Integer selected);

    @Delete("DELETE FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Delete("DELETE FROM cart WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Delete("<script>" +
            "DELETE FROM cart WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' close=')' separator=','>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);

    @Insert("<script>" +
            "INSERT INTO cart(user_id, product_id, quantity, selected, create_time, update_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.productId}, #{item.quantity}, #{item.selected}, NOW(), NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<Cart> cartList);

    // ==================== 关联查询（返回 CartVO） ====================

    @Select("SELECT c.id, c.user_id, c.product_id, c.quantity, c.selected, " +
            "p.name AS product_name, p.main_image AS product_image, p.price AS product_price, " +
            "p.stock AS product_stock, p.status AS product_status, " +
            "c.create_time, c.update_time " +
            "FROM cart c " +
            "LEFT JOIN product p ON c.product_id = p.id " +
            "WHERE c.user_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "selected", column = "selected"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "productImage", column = "product_image"),
            @Result(property = "productPrice", column = "product_price"),
            @Result(property = "productStock", column = "product_stock"),
            @Result(property = "productStatus", column = "product_status"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    List<CartVO> selectCartListByUserId(Long userId);

    @Select("SELECT c.id, c.user_id, c.product_id, c.quantity, c.selected, " +
            "p.name AS product_name, p.main_image AS product_image, p.price AS product_price, " +
            "p.stock AS product_stock, p.status AS product_status, " +
            "c.create_time, c.update_time " +
            "FROM cart c " +
            "LEFT JOIN product p ON c.product_id = p.id " +
            "WHERE c.user_id = #{userId} AND c.selected = 1 " +
            "ORDER BY c.create_time DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "selected", column = "selected"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "productImage", column = "product_image"),
            @Result(property = "productPrice", column = "product_price"),
            @Result(property = "productStock", column = "product_stock"),
            @Result(property = "productStatus", column = "product_status"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    List<CartVO> selectSelectedCartItems(Long userId);
}
