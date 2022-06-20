package com.hxzhou.mall.seckill.feign;

import com.hxzhou.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mall-coupon")
public interface CouponFeignService {

    /**
     * 获取最近三天秒杀的商品
     * @return
     */
    @GetMapping("/coupon/seckillsession/latest3DaySession")
    R getLatest3DaySession();
}
