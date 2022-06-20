package com.hxzhou.mall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.exception.NoStockException;
import com.hxzhou.common.to.mq.OrderTo;
import com.hxzhou.common.to.mq.StockDetailTo;
import com.hxzhou.common.to.mq.StockLockedTo;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.ware.entity.WareOrderTaskDetailEntity;
import com.hxzhou.mall.ware.entity.WareOrderTaskEntity;
import com.hxzhou.mall.ware.feign.OrderFeignService;
import com.hxzhou.mall.ware.feign.ProductFeignService;
import com.hxzhou.common.to.SkuHasStockVo;
import com.hxzhou.mall.ware.service.WareOrderTaskDetailService;
import com.hxzhou.mall.ware.service.WareOrderTaskService;
import com.hxzhou.mall.ware.vo.LockStockResult;
import com.hxzhou.mall.ware.vo.OrderItemVo;
import com.hxzhou.mall.ware.vo.OrderVo;
import com.hxzhou.mall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.ware.dao.WareSkuDao;
import com.hxzhou.mall.ware.entity.WareSkuEntity;
import com.hxzhou.mall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 将成功采购的进行入库
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> entities = wareSkuDao.selectList(
                new QueryWrapper<WareSkuEntity>()
                        .eq("sku_id", skuId)
                        .eq("ware_id", wareId));

        // 如果没有这个库存记录，需要新增
        if(entities == null || entities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);

            // TODO 远程查询sku的名字，如果失败，整个事务不需要回滚
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode() == 0) skuEntity.setSkuName((String) skuInfo.get("skuName"));
            } catch (Exception e) {

            }

            wareSkuDao.insert(skuEntity);
        }
        // 否则更新即可
        else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * 查询sku是否有库存
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map((skuId) -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            // 查询当前sku的总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);

            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 将订单中的商品进行库存锁定，并返回成功与否
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单详情，为了追溯
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        // 按照下单的收货地址，找到一个就近仓库，锁定库存

        // 1 找到每一个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());

            // 查询这个商品在哪里有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 2 锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            Boolean skuStocked = false;

            if(wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }

            /**
             * 1 如果每一个商品都锁定成功，将当前商品锁定了几件的工作单记录发给MQ
             * 2 如果锁定失败，前面保存的工作单信息就回滚了，发出去的消息即使要解锁记录，由于去数据库查不到id，也就不需要解锁了
             *
             */
            for (Long wareId : wareIds) {
                // 成功就返回1，否则返回0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());

                // 锁定成功，直接返回，找下一个商品即可
                if(count == 1) {
                    skuStocked = true;

                    /**
                     * 告诉MQ，库存锁定成功
                     */
                    WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity(
                            null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(entity);

                    // 添加锁定库存的工作单信息
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(entity, stockDetailTo);
                    lockedTo.setDetail(stockDetailTo);      // 只发id不行，防止回滚以后找不到id

                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);

                    break;
                }
            }

            // 如果该商品没有库存足够的仓库锁住，直接报异常
            if(skuStocked == false) {
                throw new NoStockException(skuId);
            }
        }

        // 3 如果没抛异常就走到这里，说明全部锁成功了
        return true;
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

    /**
     * 从RabbitMQ中获取消息来解锁库存：如果解锁失败，一定要告诉服务解锁失败
     *
     * 库存解锁场景
     *      1 下订单成功，订单过期没有支付，被系统自动取消或者被用户手动取消，都要解锁库存
     *      2 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     */
    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();

        /**
         * 1 查询数据库关于这个订单的锁定库存信息
         */
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        // 如果没有，代表库存锁定失败了，库存回滚了，这种情况无需解锁
        if(byId == null) {
            // 由于库存锁定失败，不需要处理，所以告诉消息队列不需要保留该消息【删除消息队列内容】
            return ;
        }

        // 否则，进行解锁
        /**
         * 2 查询订单状态来确定解锁方式：
         *      如果没有这个订单，直接还原库存
         *      如果有这个订单，分为两种情况：
         *                          订单支付成功，将库存信息保存为删减之后的样子
         *                          订单取消，还原库存
         */
        Long id = to.getId();       // 库存工作单id
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
        // 根据订单号查询订单状态
        String orderSn = taskEntity.getOrderSn();
        R r = orderFeignService.getOrderStatus(orderSn);
        // 订单数据返回成功
        if(r.getCode() == 0) {
            OrderVo data = r.getData(new TypeReference<OrderVo>() {});
            // 如果订单不存在或者订单已经取消了，将库存内容还原
            if(data == null || data.getStatus() == 4) {
                if(byId.getLockStatus() == 1) {
                    // 只有锁定状态才可以解锁
                    unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                }
                // 解锁成功之后告诉消息队列不需要保留该消息【删除消息队列内容】
            }
        }
        else {
            // 远程服务出错，将告诉消息队列继续保留该消息，下次处理【抛异常】
            throw new RuntimeException("远程服务失败");
        }
    }

    /**
     * 将锁定的库存信息还原
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 库存解锁
        wareSkuDao.unlockStock(skuId, wareId, num);

        // 更新库存工作单中的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);        // 变成已经解锁
        wareOrderTaskDetailService.updateById(entity);
    }

    /**
     * 从RabbitMQ中获取消息来解锁库存：如果解锁失败，一定要告诉服务解锁失败
     *
     * 这个重载是为了解决订单取消的时间遇到突发情况，比库存是否解锁时间慢了，那么就在订单取消后主动再去告诉库存解锁
     *
     * 注意：来到这里的都是一定要解锁库存，释放库存的工作单
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();

        // 查询最新的库存状态，防止重复解锁库存
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();

        // 按照库存工作单，找到所有没有解锁的工作单
        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", id).eq("lock_status", 1));

        // 解除库存
        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }

}