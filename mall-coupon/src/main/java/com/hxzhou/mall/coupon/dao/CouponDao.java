package com.hxzhou.mall.coupon.dao;

import com.hxzhou.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:28:02
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
