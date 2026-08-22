package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    // ========== 以下为 BaseMapper 已覆盖的方法（无需重写） ==========
    // insert(category)     → BaseMapper.insert
    // updateById(category) → BaseMapper.updateById
    // deleteById(id)       → BaseMapper.deleteById (配合 @TableLogic 自动逻辑删除)
    // selectById(id)       → BaseMapper.selectById
    // selectList(null)     → BaseMapper.selectList (查全部)
    // =================================================================

    @Select("SELECT * FROM category WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY sort ASC, id ASC")
    List<Category> findByParentId(Long parentId);

    @Select("SELECT * FROM category WHERE level = #{level} AND is_deleted = 0 ORDER BY sort ASC, id ASC")
    List<Category> findByLevel(Integer level);

    @Select("SELECT * FROM category WHERE name = #{name} AND is_deleted = 0 LIMIT 1")
    Category findByName(String name);

    @Select("SELECT COUNT(*) FROM category WHERE parent_id = #{parentId} AND is_deleted = 0")
    int countByParentId(Long parentId);

    @Update("UPDATE category SET status = #{status}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
