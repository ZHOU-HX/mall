package com.hxzhou.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.to.mq.OrderTo;
import com.hxzhou.common.to.mq.StockLockedTo;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.ware.entity.WareSkuEntity;
import com.hxzhou.common.to.SkuHasStockVo;
import com.hxzhou.mall.ware.vo.LockStockResult;
import com.hxzhou.mall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:55:44
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);
}

