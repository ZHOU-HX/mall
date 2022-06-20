package com.hxzhou.mall.ware.vo;

import lombok.Data;

/**
 * 库存锁定结果：订单中每个商品锁定库存的成功与否
 */
@Data
public class LockStockResult {

    private Long skuId;
    private Integer num;
    private Boolean locked;
}
