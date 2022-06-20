package com.hxzhou.mall.order.web;

import com.hxzhou.common.exception.NoStockException;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.order.entity.OrderEntity;
import com.hxzhou.mall.order.service.OrderService;
import com.hxzhou.mall.order.vo.OrderConfirmVo;
import com.hxzhou.mall.order.vo.OrderSubmitResponseVo;
import com.hxzhou.mall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取订单页面所有详情信息
     * @param model
     * @return
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException  {
        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData", confirmVo);

        return "confirm";
    }

    /**
     * 下单功能，提交订单之后的数据处理
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {
        // 下单：验证令牌->创建订单->检验价格->保存订单->锁定库存
        try {
            OrderSubmitResponseVo responseVo = orderService.submitOrder(vo);

            // 下单成功跳转到支付页面
            if(responseVo.getCode() == 0) {
                model.addAttribute("orderSubmitResp", responseVo);
                return "pay";
            }
            // 下单失败跳回订单确认面重新确认订单信息
            else {
                String msg = "下单失败，原因：";
                switch (responseVo.getCode()) {
                    case 1 : msg += "订单信息过期，请刷新后再次进行提交！"; break;
                    case 2 : msg += "订单商品价格发生变化，请确认后再次进行提交！"; break;
                    case 3 : msg += "库存锁定失败，商品库存不足！"; break;
                    case 4 : msg += "远程服务出现错误！"; break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.mall.com/toTrade";
            }
        } catch (NoStockException e) {
            redirectAttributes.addFlashAttribute("msg", "商品id为" + e.getSkuId().toString() + "的库存不足");
            return "redirect:http://order.mall.com/toTrade";
        }
    }
}
