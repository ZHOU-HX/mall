package com.hxzhou.mall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 使用JSON序列化机制，进行消息转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     *  1 服务器收到消息就回调
     *      1.1 spring.rabbitmq.publisher-confirms=true【注意：前者被弃用，需要采用spring.rabbitmq.publisher-confirm-type=correlated】
     *      1.2 设置确认回调ConfirmCallback()
     *  2 消息正确抵达队列进行回调
     *      2.1 spring.rabbitmq.publisher-returns=true
     *          spring.rabbitmq.template.mandatory=true
     *      2.2 设置确认回调ReturnsCallback()
     *  3 消费端确认（保证每一个消息都能被真正的消费执行，此时才可以broker删除这个消息）
     *      3.1 默认是自动确认的，只有这个消息被正确接收到，客户端会自动确认，服务端才会移除这个消息
     *          但是会出现一个问题：我们收到很多消息，自动回复给服务器ack，只有一个消息处理成功，然后宕机了，会出现其他还没来得及执行的消息都丢失了
     *          可以手动确认来解决【spring.rabbitmq.listener.simple.acknowledge-mode=manual】
     *              只要我们没有明确告诉MQ该消息被接收，就没有ack，消息就会一直是unacked状态，即使客户端宕机了，消息也不会丢失，会重新变为ready状态
     *      3.2 在打开手动确认选项后，要在每次接收消息后来告诉MQ该消息是否被接收
     *          long deliveryTag = message.getMessageProperties().getDeliveryTag();
     *          channel.basicAck(deliveryTag, false);       // 确认收到消息，false代表非批量确认
     *      3.3 也可以拒绝接收：channel.basicNack(deliveryTag, false, false);【第一个false代表是否批量执行，第二个false代表是否重新入队】
     */
    @PostConstruct      // MyRabbitConfig对象创建完成之后，执行这个方法
    public void initRabbitTemplate() {
        // 设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *  只要消息抵达Broker就为true，不管这个消息是否被处理
             * @param correlationData   当前消息的唯一关联数据（这个是消息的唯一id）
             * @param b     消息的是否成功收到
             * @param s     失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("confirm...correlationData[" + correlationData + "]===>ack[" + b + "]===>cause[" + s + "]");
            }
        });

        // 设置消息抵达队列的确认回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            /**
             * 只有消息没有投递给指定的队列，才会触发这个失败回调
             * @param returnedMessage
             * public class ReturnedMessage {
             *     private final Message message;       投递失败的消息详情信息
             *     private final int replyCode;         回复的状态码
             *     private final String replyText;      回复的文本内容
             *     private final String exchange;       当时这个消息发给哪个交换机
             *     private final String routingKey;     当时这个消息用哪个路由键
             */
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                System.out.println("Fail Massage[" + returnedMessage.getMessage() + "]"
                        + "\n===>replyCode[" + returnedMessage.getReplyCode() + "]"
                        + "\n===>exchange[" + returnedMessage.getExchange() + "]"
                        + "\n===>routingKey[" + returnedMessage.getRoutingKey() + "]"
                        + "\n===>replyText[" + returnedMessage.getReplyText() + "]");
            }
        });

    }
}
