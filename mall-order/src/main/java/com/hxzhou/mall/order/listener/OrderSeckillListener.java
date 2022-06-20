package com.hxzhou.mall.order.listener;

import com.hxzhou.common.to.mq.SeckillOrderTo;
import com.hxzhou.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RabbitListener(queues = "order.seckill.order.queue")
public class OrderSeckillListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void listener(SeckillOrderTo entity, Channel channel, Message message) throws IOException {
        try {
            // 创建秒杀单的详细信息
            log.info("准备创建秒杀单的详细信息");
            orderService.createSeckillOrder(entity);

            // 订单关闭成功，将其从队列中删除
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 订单关闭失败，将其重新放入普通队列中
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
