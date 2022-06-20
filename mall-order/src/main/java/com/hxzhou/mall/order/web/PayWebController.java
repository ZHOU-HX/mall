package com.hxzhou.mall.order.web;

import com.alipay.api.AlipayApiException;
import com.hxzhou.mall.order.config.AlipayTemplate;
import com.hxzhou.mall.order.service.OrderService;
import com.hxzhou.mall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/payOrder", produces = {"text/html;charset=UTF-8"})
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPay(orderSn);

        // 返回的是一个支付界面。将此页面直接交给浏览器就行
        String pay = alipayTemplate.pay(payVo);

        return pay;
    }
}
