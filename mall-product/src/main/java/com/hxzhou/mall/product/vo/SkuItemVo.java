package com.hxzhou.mall.product.vo;

import com.hxzhou.mall.product.entity.SkuImagesEntity;
import com.hxzhou.mall.product.entity.SkuInfoEntity;
import com.hxzhou.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {

    private SkuInfoEntity info;                     // sku基本信息

    private List<SkuImagesEntity> images;           // sku的图片信息

    private List<SkuItemSaleAttrVo> saleAttr;       // spu的销售属性组合

    private SpuInfoDescEntity desp;                 // spu的介绍

    private List<SpuItemAttrGroupVo> groupAttrs;    // spu的规格参数信息

    private boolean hasStock = true;                // 是否有货

    // =====================================================================================

    @ToString
    @Data
    public static class SkuItemSaleAttrVo {
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @ToString
    @Data
    public static class AttrValueWithSkuIdVo {
        private String attrValue;
        private String skuIds;
    }

    // =======================LLL========================================================

    @ToString
    @Data
    public static class SpuItemAttrGroupVo {
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @ToString
    @Data
    public static class SpuBaseAttrVo {
        private String attrName;
        private String attrValue;
    }
}
