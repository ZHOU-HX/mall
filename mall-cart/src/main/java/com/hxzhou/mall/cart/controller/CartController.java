package com.hxzhou.mall.cart.controller;

import com.hxzhou.mall.cart.interceptor.CartInterceptor;
import com.hxzhou.mall.cart.service.CartService;
import com.hxzhou.mall.cart.vo.Cart;
import com.hxzhou.mall.cart.vo.CartItem;
import com.hxzhou.mall.cart.vo.UserInfoTo;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 获取用户的购物车数据
     */
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems() {
        return cartService.getUserCartItem();
    }

    /**
     * 删除购物车内商品
     * @param skuId
     * @return
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);

        return "redirect:http://cart.mall.com/cart.html";
    }

    /**
     * 更改购物车内商品数量
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId, num);

        return "redirect:http://cart.mall.com/cart.html";
    }

    /**
     * 勾选购物项来更新购物车
     * @param skuId
     * @param check
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check) {
        cartService.checkItem(skuId, check);

        return "redirect:http://cart.mall.com/cart.html";
    }

    /**
     * 浏览器有一个cookie：user-key【它用来标识用户身份，一个月后过期】
     * 如果第一次使用jd的购物车功能，都会带上一个临时的用户身份
     * 浏览器以后保存，每次访问都会带上这个cookie
     *
     * 登录：session有
     * 没登录：按照cookie里面带来的user-key来做
     * 第一次：如果没有临时用户，帮忙创建一个临时用户
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        // 1 快速得到用户信息 id和userkey，可以使用ThreadLocal来线程共享
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);

        return "cartList";
    }

    /**
     * 添加商品到购物车
     *      RedirectAttributes ra
     *          ra.addAttribute：将数据放在url后面
     *          ra.addFlashAttribute：将数据放在session里面可以在页面取出，但是只能取一次
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra,
                            HttpSession session) throws ExecutionException, InterruptedException {

        CartItem cartItem = cartService.addToCart(skuId, num);

//        model.addAttribute("item", cartItem);

        ra.addAttribute("skuId", skuId);
        ra.addAttribute("num", num);
        return "redirect:http://cart.mall.com/addToCartSuccess.html";

//        ra.addFlashAttribute("item", cartItem);
//        session.setAttribute("cartItem", cartItem);
//        return "redirect:http://cart.mall.com/addToCartSuccess1.html";
    }

    /**
     * 添加物品成功到购物车后，重定向成功界面
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) {
        CartItem cartItem = cartService.getCartItem(skuId);
        if(cartItem != null) cartItem.setCount(num);

        model.addAttribute("item", cartItem);

        return "success";
    }

    /**
     * 添加物品成功到购物车后，重定向成功界面【利用session来共享】
     * @return
     */
    @GetMapping("/addToCartSuccess1.html")
    public String addToCartSuccessPage1(HttpSession session, Model model) {
        CartItem cartItem = (CartItem) session.getAttribute("cartItem");
//        System.out.println(cartItem);
        model.addAttribute("item", cartItem);

        return "success";
    }
}
