package com.hxzhou.mall.product.feign;

import com.hxzhou.common.to.SkuReductionTo;
import com.hxzhou.common.to.SpuBoundTo;
import com.hxzhou.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("mall-coupon")     // 声明调用哪个远程服务
public interface CouponFeignService {

    /**
     * SpringCloud远程调用的逻辑
     *      1 当请求方发送CouponFeignService.saveSpuBounds(spuBoundTo);请求时，@RequestBody将这个对象转为json
     *      2 找到mall-coupon服务，给coupon/spubounds/save发送请求，将上一步转到json放在请求体位置，发送请求
     *      3 对方服务收到请求，请求体里有json数据，利用@RequestBody可以再将json数据转为接受的对象类型
     * 总结：只要json数据模型是兼容的，双方服务无需使用同一个to
     * @param spuBoundTo
     * @return
     */
    @PostMapping("coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}