package com.hxzhou.mall.product.feign;

import com.hxzhou.common.to.SkuHasStockVo;
import com.hxzhou.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mall-ware")
public interface WareFeignService {

    /**
     * 对于返回值类型匹配问题：
     *      1 可以设置R的泛型，即R<T>这种形式
     *      2 可以直接返回我们想要的结果数据，即将返回的R变成我们想要的数据类型
     *      3 自己封装解析结果
     * @param skuIds
     * @return
     */
    @PostMapping("/ware/waresku/hasstock")
    R getSkusHasStock(@RequestBody List<Long> skuIds);
}
