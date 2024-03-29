package com.hxzhou.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.coupon.entity.HomeSubjectEntity;

import java.util.Map;

/**
 * 首页专题表【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:28:02
 */
public interface HomeSubjectService extends IService<HomeSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

