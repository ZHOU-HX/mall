package com.hxzhou.mall.ware.feign;

import com.hxzhou.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mall-product")
public interface ProductFeignService {

    /**
     * 远程调用可以有两种方式，第一种是给网关，第二种是直接调用
     *      一、让所有请求过网关
     *          1 @FeignClient("mall-gateway")：给mall-gateway所在的机器发送请求
     *          2 @RequestMapping("api/product/skuinfo/info/{skuId}")
     *      二、直接让后台指定服务处理
     *          1 @FeignClient("mall-product")
     *          2 @RequestMapping("product/skuinfo/info/{skuId}")
     *
     */
    @RequestMapping("product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
