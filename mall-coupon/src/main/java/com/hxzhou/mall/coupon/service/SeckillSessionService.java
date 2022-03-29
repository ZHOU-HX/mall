package com.hxzhou.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.coupon.entity.SeckillSessionEntity;

import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:28:02
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

