package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

//baseMapper包含insert delete等方法
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据分类ID查询商品列表
     */
    @Select("SELECT * FROM product WHERE category_id = #{categoryId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 根据商品名称模糊查询
     */
    @Select("SELECT * FROM product WHERE name LIKE CONCAT('%', #{name}, '%') AND is_deleted = 0")
    List<Product> findByNameLike(@Param("name") String name);

    /**
     * 查询热门商品
     */
    @Select("SELECT * FROM product WHERE is_deleted = 0 AND status = 1 ORDER BY sales DESC LIMIT #{limit}")
    List<Product> findHotProducts(@Param("limit") int limit);

    /**
     * 查询推荐商品
     */
    @Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE product SET sales = sales + #{quantity} WHERE id = #{productId}")
    int increaseSales(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}