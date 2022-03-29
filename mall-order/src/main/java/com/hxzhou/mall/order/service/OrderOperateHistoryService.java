package com.hxzhou.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.order.entity.OrderOperateHistoryEntity;

import java.util.Map;

/**
 * 订单操作历史记录
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:48:25
 */
public interface OrderOperateHistoryService extends IService<OrderOperateHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

