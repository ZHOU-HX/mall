package com.hxzhou.common.to.mq;

import lombok.Data;


/**
 * 订单锁定
 */
@Data
public class StockLockedTo {

    private Long id;    // 库存工作单的id
    private StockDetailTo detail;        // 工作单详情的所有id
}
