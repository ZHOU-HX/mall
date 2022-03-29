package com.hxzhou.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.ware.entity.WareOrderTaskDetailEntity;

import java.util.Map;

/**
 * 库存工作单
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:55:44
 */
public interface WareOrderTaskDetailService extends IService<WareOrderTaskDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

