package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Select("SELECT * FROM cart WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Cart> findByUserId(@Param("userId") Long userId);

    @Update("UPDATE cart SET quantity = quantity + #{quantity}, update_time = NOW() WHERE id = #{id}")
    int increaseQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE cart SET quantity = #{quantity}, update_time = NOW() WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 批量删除购物车项
     * @param ids 购物车项ID列表
     * @return 删除数量
     */
    default int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            count += deleteById(id);
        }
        return count;
    }
}