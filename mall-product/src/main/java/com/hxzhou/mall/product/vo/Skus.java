/**
  * Copyright 2022 json.cn 
  */
package com.hxzhou.mall.product.vo;
import com.hxzhou.common.to.MemberPrice;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Auto-generated: 2022-04-23 17:27:58
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Skus {

    private List<Attr> attr;
    private String skuName;
    private BigDecimal price;
    private String skuTitle;
    private String skuSubtitle;
    private List<Images> images;
    private List<String> descar;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    // 注意：这个memberprice一定要跟skureductionTo中的一致，否则beanutils.copyProperties拷贝不了
    private List<MemberPrice> memberPrice;
}