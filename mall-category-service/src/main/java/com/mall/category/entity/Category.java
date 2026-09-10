package com.mall.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 分类名称 */
    private String name;
    
    /** 父分类ID */
    private Long parentId;
    
    /** 分类层级 */
    private Integer level;
    
    /** 排序 */
    private Integer sort;
    
    /** 图标 */
    private String icon;
    
    /** 状态：0-禁用 1-启用 */
    private Integer status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer isDeleted;

    /** 子分类列表（非数据库字段） */
    @TableField(exist = false)
    private List<Category> children;
}
