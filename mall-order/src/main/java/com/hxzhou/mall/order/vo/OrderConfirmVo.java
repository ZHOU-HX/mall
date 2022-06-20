package com.hxzhou.mall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要用到的数据
 */
public class OrderConfirmVo {

    @Setter
    @Getter
    private String orderToken;      // 订单防重令牌

    @Setter
    @Getter
    private List<MemberAddressVo> address;      // 收货地址：ums_member_receive_address表

    @Setter
    @Getter
    private List<OrderItemVo> items;        // 所有选中的购物项

    @Setter
    @Getter
    private Integer integration;            // 优惠券信息

    @Setter
    @Getter
    Map<Long, Boolean> stocks;          // 每个商品id对应的库存信息

//    private BigDecimal total;           // 订单总额
    // 订单总额
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");

        if(items != null) {
            for(OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }

        return sum;
    }

//    private BigDecimal payPrice;        // 实际付款金额
    // 实际付款金额
    public BigDecimal getPayPrice() {
        return getTotal();
    }

    public Integer getCount() {
        Integer i = 0;
        if(items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }
}
