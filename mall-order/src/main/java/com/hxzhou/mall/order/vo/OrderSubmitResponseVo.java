package com.hxzhou.mall.order.vo;

import com.hxzhou.mall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 下单成功后返回的数据
 */
@Data
public class OrderSubmitResponseVo {

    private OrderEntity order;      // 订单信息
    private Integer code;       // 0代表成功；其他代表错误的状态码
}
