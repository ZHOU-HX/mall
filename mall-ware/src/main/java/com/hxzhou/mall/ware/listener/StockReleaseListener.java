package com.hxzhou.mall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.to.mq.OrderTo;
import com.hxzhou.common.to.mq.StockDetailTo;
import com.hxzhou.common.to.mq.StockLockedTo;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.ware.entity.WareOrderTaskDetailEntity;
import com.hxzhou.mall.ware.entity.WareOrderTaskEntity;
import com.hxzhou.mall.ware.service.WareSkuService;
import com.hxzhou.mall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 从RabbitMQ中获取消息来解锁库存：如果解锁失败，一定要告诉服务解锁失败
     *
     * 库存解锁场景
     *      1 下订单成功，订单过期没有支付，被系统自动取消或者被用户手动取消，都要解锁库存
     *      2 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.err.println("收到库存解锁消息");
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

//        try {
//            System.out.println("收到库存解锁消息");
//            StockDetailTo detail = to.getDetail();
//            Long detailId = detail.getId();
//
//            /**
//             * 1 查询数据库关于这个订单的锁定库存信息
//             */
//            WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
//            // 如果没有，代表库存锁定失败了，库存回滚了，这种情况无需解锁
//            if(byId == null) {
//                // 由于库存锁定失败，不需要处理，所以告诉消息队列不需要保留该消息
//                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//                return ;
//            }
//
//            // 否则，进行解锁
//            /**
//             * 2 查询订单状态来确定解锁方式：
//             *      如果没有这个订单，直接还原库存
//             *      如果有这个订单，分为两种情况：
//             *                          订单支付成功，将库存信息保存为删减之后的样子
//             *                          订单取消，还原库存
//             */
//            Long id = to.getId();       // 库存工作单id
//            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
//            // 根据订单号查询订单状态
//            String orderSn = taskEntity.getOrderSn();
//            R r = orderFeignService.getOrderStatus(orderSn);
//            // 订单数据返回成功
//            if(r.getCode() == 0) {
//                OrderVo data = r.getData(new TypeReference<OrderVo>() {});
//                // 如果订单不存在或者订单已经取消了，将库存内容还原
//                if(data == null || data.getStatus() == 4) {
//                    unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
//                    // 解锁成功之后告诉消息队列不需要保留该消息
//                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//                }
//            }
//            else {
//                // 远程服务出错，将告诉消息队列继续保留该消息，下次处理
//                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
//            }
//        } catch (Exception e) {
//            // 出现如何异常，都要保留消息队列中的消息
//            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
//        }
    }

    @RabbitHandler
    public void handleStockLockedRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭，准备解锁库存！");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
