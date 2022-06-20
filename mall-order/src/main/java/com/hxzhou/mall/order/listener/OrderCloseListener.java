package com.hxzhou.mall.order.listener;

import com.hxzhou.mall.order.entity.OrderEntity;
import com.hxzhou.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "order.release.order.queue")
public class OrderCloseListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void listener(OrderEntity entity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期订单，准备关闭订单：" + entity.getOrderSn());

        try {
            orderService.closeOrder(entity);
            // TODO 手动调用支付宝的收单功能


            // 订单关闭成功，将其从队列中删除
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 订单关闭失败，将其重新放入普通队列中
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
