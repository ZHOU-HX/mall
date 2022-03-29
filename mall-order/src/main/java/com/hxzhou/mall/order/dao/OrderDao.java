package com.hxzhou.mall.order.dao;

import com.hxzhou.mall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:48:25
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
