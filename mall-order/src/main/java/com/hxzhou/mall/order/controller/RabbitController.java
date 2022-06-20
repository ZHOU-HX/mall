package com.hxzhou.mall.order.controller;

import com.hxzhou.mall.order.entity.OrderEntity;
import com.hxzhou.mall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RabbitListener(queues = {"hello-java-queue"})
@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 向同一个消息队列发送不同类型的消息
     * @param num
     * @return
     */
    @GetMapping("/sendMQ")
    public String sendMQ(@RequestParam(value = "num", defaultValue = "10") Integer num) {
        for(int i = 0; i < num; i++) {
            if(i % 2 == 0) {
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setName("西里晴00" + i);
                rabbitTemplate.convertAndSend(
                        "hello-java-exchange",
                        "hello.java",
                        reasonEntity,
                        new CorrelationData(UUID.randomUUID().toString()));
            }
            else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend(
                        "hello-java-exchange",
                        "hello.java",
                        orderEntity,
                        new CorrelationData(UUID.randomUUID().toString()));
            }
        }
        return "ok";
    }

    /**
     * 只接收OrderReturnReasonEntity类型的消息
     */
    @RabbitHandler
    public void receiveOrderReturnReasonEntityMessage(Message message, OrderReturnReasonEntity content, Channel channel) {
        // channel内按顺序自增
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("receiveOrderReturnReasonEntityMessage方法接收到[" + deliveryTag + "]消息：" + content);

        try {
            // 确认收到消息，false代表非批量确认
            channel.basicAck(deliveryTag, false);

//            // 也可以进行拒绝接收该消息【第一个false代表是否批量执行，第二个false代表是否重新入队】
//            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            // 网络中断
        }
    }

    /**
     * 只接收OrderReturnReasonEntity类型的消息
     */
    @RabbitHandler
    public void receiveOrderEntityMessage(Message message, OrderEntity content, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.err.println("receiveOrderEntityMessage方法接收到[" + deliveryTag + "]消息：" + content);

        try {
            // 确认收到消息，false代表非批量确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 网络中断
        }
    }
}
