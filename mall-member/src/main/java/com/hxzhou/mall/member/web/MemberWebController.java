package com.hxzhou.mall.member.web;

import com.hxzhou.common.utils.R;
import com.hxzhou.mall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Model model) {
        Map<String, Object> page = new HashMap<>();
        page.put("page", pageNum.toString());

        // 查询出当前登录用户的所有订单列表数据
        R r = orderFeignService.listWithItem(page);
        model.addAttribute("orders", r);

        return "orderList";
    }
}
