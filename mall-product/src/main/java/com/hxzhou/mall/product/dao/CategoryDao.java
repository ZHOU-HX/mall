package com.hxzhou.mall.product.dao;

import com.hxzhou.mall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-24 15:38:07
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
