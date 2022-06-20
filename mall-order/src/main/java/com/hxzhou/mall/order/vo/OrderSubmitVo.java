package com.hxzhou.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交信息
 */
@Data
public class OrderSubmitVo {

    private Long addrId;            // 收货地址的id
    private Integer payType;        // 支付方式
    private String orderToken;      // 防重令牌
    private BigDecimal payPrice;    // 应付价格，为了验证价格是否发生了变化
    private String note;            // 订单备注
    // 注：无需提交需要购买的商品。需要去购物车再获取一遍

    // 用户相关信息，直接去session中去取
}
