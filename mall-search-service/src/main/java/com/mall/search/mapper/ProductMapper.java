package com.mall.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.search.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
