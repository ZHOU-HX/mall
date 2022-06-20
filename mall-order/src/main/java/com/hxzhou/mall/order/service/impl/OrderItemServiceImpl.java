package com.hxzhou.mall.order.service.impl;

import com.hxzhou.mall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.order.dao.OrderItemDao;
import com.hxzhou.mall.order.entity.OrderItemEntity;
import com.hxzhou.mall.order.service.OrderItemService;


@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 参数可以写以下类型
     *      1 Message message：原生消息详细信息，头+体
     *      2 OrderReturnReasonEntity content：T<发送的消息类型>
     *      3 Channel channel：当前传输数据的通道
     *
     * Queue：可以很多人都来监听，但是只要收到了消息之后，队列消息就会被及时删除，而且只能有一个收到该消息
     *
     * 场景：
     *      1 订单服务启动多个，只会有一个端口号的订单服务接收到这一条消息，并不会全部接收同一条消息【轮询接收】
     *      2 当接收到上一条消息后，在处理过程中，并不会接收消息队列的下一条消息，只会在处理完当前消息后再去接收并处理下一条消息【串行处理】
     */
//    @RabbitListener(queues = {"hello-java-queue"})
//    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) {
//        System.err.println("接收到消息：" + message + "\n内容是：" + content);
//    }

}