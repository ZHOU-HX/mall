package com.hxzhou.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.product.entity.AttrEntity;

import java.util.Map;

/**
 * 商品属性
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-24 15:38:07
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

