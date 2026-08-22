package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问接口
 * 继承 BaseMapper 后自动拥有以下方法：
 * - insert(T entity)：插入一条记录
 * - deleteById(Serializable id)：根据 ID 删除
 * - updateById(T entity)：根据 ID 更新
 * - selectById(Serializable id)：根据 ID 查询
 * - selectList(Wrapper<T> queryWrapper)：条件查询列表
 * - selectCount(Wrapper<T> queryWrapper)：条件查询总数
 * - selectPage(Page<T> page, Wrapper<T> queryWrapper)：分页查询
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * 注意：逻辑删除字段 is_deleted 会自动过滤
     * @param username 用户名
     * @return 用户对象
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     * 注意：逻辑删除字段 is_deleted 会自动过滤
     * @param phone 手机号
     * @return 用户对象
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户
     * 注意：逻辑删除字段 is_deleted 会自动过滤
     * @param email 邮箱
     * @return 用户对象
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(@Param("email") String email);
    
    // BaseMapper 已提供基础 CRUD 方法
    // 如需更多自定义 SQL，可在此添加方法并配合 XML 或注解使用
}