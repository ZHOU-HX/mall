package com.hxzhou.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuReductionTo {

    private Long skuId;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    // 注意：这个memberprice一定要跟Skus中的一致，否则beanutils.copyProperties拷贝不了
    private List<MemberPrice> memberPrice;
}
