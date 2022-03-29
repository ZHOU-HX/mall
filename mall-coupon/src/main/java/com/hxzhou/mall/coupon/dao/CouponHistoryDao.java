package com.hxzhou.mall.coupon.dao;

import com.hxzhou.mall.coupon.entity.CouponHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取历史记录
 * 
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:28:01
 */
@Mapper
public interface CouponHistoryDao extends BaseMapper<CouponHistoryEntity> {
	
}
