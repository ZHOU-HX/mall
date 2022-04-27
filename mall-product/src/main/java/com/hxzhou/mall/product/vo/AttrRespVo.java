package com.hxzhou.mall.product.vo;

import lombok.Data;

@Data
public class AttrRespVo extends AttrVo {

    /**
     * 分类名字
     */
    private String catelogName;
    /**
     * 分组名字
     */
    private String groupName;

    /**
     * 分组路径
     */
    private Long[] catelogPath;
}
