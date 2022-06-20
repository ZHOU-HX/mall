package com.hxzhou.mall.order.vo;

import com.hxzhou.mall.order.entity.OrderEntity;
import com.hxzhou.mall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建的订单项
 */
@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;        // 订单计算的应付价格

    private BigDecimal fare;        // 运费
}
