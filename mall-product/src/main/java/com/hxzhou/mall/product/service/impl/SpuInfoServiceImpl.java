package com.hxzhou.mall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.constant.ProductConstant;
import com.hxzhou.common.to.MemberPrice;
import com.hxzhou.common.to.SkuHasStockVo;
import com.hxzhou.common.to.SkuReductionTo;
import com.hxzhou.common.to.SpuBoundTo;
import com.hxzhou.common.to.es.SkuEsModel;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.product.entity.*;
import com.hxzhou.mall.product.feign.CouponFeignService;
import com.hxzhou.mall.product.feign.SearchFeignService;
import com.hxzhou.mall.product.feign.WareFeignService;
import com.hxzhou.mall.product.service.*;
import com.hxzhou.mall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 大保存操作
     * @param vo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        /**
         * 1 保存spu基本信息【在pms_spu_info表中进行】
         */
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        /**
         * 2 保存spu的描述信息【在pms_spu_info_desc表中进行】
         */
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", decript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        /**
         * 3 保存spu的图片集【在pms_spu_images表中进行】
         */
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(), images);

        /**
         * 4 保存spu的规格参数【在pms_product_attr_value表中进行】
         */
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map((attr) -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();

            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity id = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(id.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(infoEntity.getId());

            return valueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        /**
         * 5 保存spu的积分信息【在mall_sms数据库中的sms_spu_bounds表中进行】【需要调用远程服务】
         */
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r.getCode() != 0) {
            log.error("远程保存spu积分信息失败！");
        }

        /**
         * 6 保存当前spu对应的所有sku信息
         */
        List<Skus> skus = vo.getSkus();
        if(skus != null && skus.size() > 0) {
            skus.forEach((item) -> {
                // 6.1 sku的基本信息【在pms_sku_info表中进行】
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) defaultImg = image.getImgUrl();
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                // 6.2 sku的图片信息【在pms_sku_images表中进行】
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map((img) -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter((entity) -> {
                    // 没有图片的，路径无需保存(返回true就是需要，返回false就是剔除)
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntities);


                // 6.3 sku的销售属性信息【在pms_sku_sale_attr_value表中进行】
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map((it) -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(it, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);

                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                // 6.4 sku的优惠、满减等信息【在mall_sms数据库中的sms_sku_ladder/sms_sku_full_reduction/sms_member_price表中进行】
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                List<MemberPrice> memberPrice = item.getMemberPrice();
                skuReductionTo.setMemberPrice(memberPrice);
                skuReductionTo.setSkuId(skuId);
                if(skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(BigDecimal.ZERO) == 1) {
                    // 如果有优惠信息再去执行操作
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode() != 0) {
                        log.error("远程保存spu优惠信息失败！");
                    }
                }

            });
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        this.baseMapper.insert(infoEntity);
    }

    /**
     * 根据条件查询SPU商品信息
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 商品上架功能
     * @param spuId
     */
    @Override
    public void up(Long spuId) {
        /**
         * 1 查出当前spuid对应的所有sku信息、品牌的名字
         */
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        /**
         * 2 查询当前sku的所有可以被用来检索的规格属性
         */
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = baseAttrs.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);     // 在指定的所有属性集合里面，挑出检索属性
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter((item) -> {
            return idSet.contains(item.getAttrId());
        }).map((item) -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        /**
         * 3 发送远程调用，库存系统查询是否有库存
         */
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        try {
            R skusHasStock = wareFeignService.getSkusHasStock(skuIdList);

            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<>() {};

            stockMap = skusHasStock.getData(typeReference).stream().collect(
                    Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        } catch (Exception e) {
            log.error("库存服务查询异常：原因{}", e);
        }

        /**
         * 4 封装每个sku信息
         */
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map((sku) -> {
            // 组装需要的数据
            SkuEsModel esModel = new SkuEsModel();

            // 1、拷贝信息，同时将名称不同，但是含义相同的一起设置拷贝出来
            BeanUtils.copyProperties(sku, esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            // 2、设置库存信息
            esModel.setHasStock(finalStockMap.getOrDefault(sku.getSkuId(), true));

            // 3、热度评分。默认为0
            esModel.setHotScore(0L);

            // 4、查询品牌的名字信息
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            // 5、查询分类的名字信息
            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());

            // 6、设置检索属性
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        /**
         * 5 将数据发送给es进行存储
         */
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0) {
            // 远程调用成功
            /**
             * 6 修改当前spu状态【变为已上架】
             */
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }
        else {
            // 远程调用失败
            /**
             * TODO 7 重复调用问题，接口幂等性，重试机制
             */
            /**
             * Feign调用流程
             *      1 构造请求数据，将对象转为json
             *          RequestTemplate template = buildTemplateFromArgs.create(argv);
             *      2 发送请求进行执行（执行成功会解码响应数据）
             *          executeAndDecode(template);
             *      3 执行请求会有重试机制
             *          while(true) {
             *              try {
             *                  executeAndDecode(template);
             *              } catch () {
             *                  try {
             *                      retryer.continueOrPropagate(e);
             *                  } catch () {
             *                      throw ex;
             *                  }
             *                  continue;
             *              }
             *          }
             */
        }
    }

}