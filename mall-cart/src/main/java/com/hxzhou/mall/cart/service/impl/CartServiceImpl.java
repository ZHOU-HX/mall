package com.hxzhou.mall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.cart.feign.ProductFeignService;
import com.hxzhou.mall.cart.interceptor.CartInterceptor;
import com.hxzhou.mall.cart.service.CartService;
import com.hxzhou.mall.cart.vo.Cart;
import com.hxzhou.mall.cart.vo.CartItem;
import com.hxzhou.mall.cart.vo.SkuInfoVo;
import com.hxzhou.mall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "mall:cart:";

    /**
     * 添加商品到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());

        // 如果购物车原先就有该商品
        if(!StringUtils.isEmpty(res)) {
            // 更新商品数量即可
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);

            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }

        // 否则，购物车中没有该商品，则需要去数据库查询商品信息
        CartItem cartItem = new CartItem();

        // 1 远程查询当前要添加的商品信息，并保存起来
        CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
            R skuInfo = productFeignService.getSkuInfo(skuId);
            SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
            });

            cartItem.setCheck(true);
            cartItem.setCount(num);
            cartItem.setImage(data.getSkuDefaultImg());
            cartItem.setTitle(data.getSkuTitle());
            cartItem.setSkuId(skuId);
            cartItem.setPrice(data.getPrice());
        }, executor);


        // 2 远程查询sku的组合属性信息，并保存起来
        CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
            List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
            cartItem.setSkuAttr(values);
        }, executor);

        // 当上述都执行完毕才可以往后执行
        CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();

        // 3 商品添加到购物车
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);

        return cartItem;
    }

    /**
     * 通过skuid获取购物车内对应商品信息
     * @param skuId
     * @return
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());

        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 获取购物车内容
     * @return
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        // 判断是否是用户登录还是临时登录
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();

        // 1 无论是否登录，都要获取临时购物车内容
        String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
        List<CartItem> tempCartItems = getCartItems(tempCartKey);
        List<CartItem> cartItems = tempCartItems;                   // 目前为止总购物车

        // 2 如果用户登录，获取用户购物车并合并临时购物车
        if(userInfoTo.getUserId() != null) {
            // 2.1 获取用户购物车
            String userCartId = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> userCartItems = getCartItems(userCartId);

            // 2.2 合并临时购物车和用户购物车，并清空临时购物车
            if(tempCartItems != null) {
                for(CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                clearCart(tempCartKey);
            }

            // 2.3 重新获取购物车内容
            cartItems = getCartItems(userCartId);
        }

        cart.setItems(cartItems);
        return cart;
    }

    /**
     * 清空购物车数据
     * @param cartKey
     */
    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    /**
     * 勾选购物项来更新购物车信息
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);

        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    /**
     * 更改购物车商品数量
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 删除购物车内商品
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());

    }

    /**
     * 获取当前用户购物车内的数据
     * @return
     */
    @Override
    public List<CartItem> getUserCartItem() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        // 如果没有登录。直接返回null
        if(userInfoTo.getUserId() == null) return null;

        // 否则查询信息
        String cartKey  =CART_PREFIX + userInfoTo.getUserId();
        List<CartItem> cartItems = getCartItems(cartKey);

        // 获取仅仅被选中的购物项
        List<CartItem> collect = cartItems.stream()
                .filter(item -> item.getCheck())
                .map(item -> {
                    // 更新购物车被选中的购物项的最新价格
                    R price = productFeignService.getPrice(item.getSkuId());
                    String data = (String) price.get("data");
                    item.setPrice(new BigDecimal(data));
                    return item;
                })
                .collect(Collectors.toList());

        return collect;
    }

    /**
     * 根据购物车id获取所拥有的购物车内商品信息
     * @param cartKey
     * @return
     */
    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);

        List<Object> values = hashOps.values();
        if(values != null && values.size() > 0) {
            List<CartItem> collect = values.stream().map((obj) -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);

                return cartItem;
            }).collect(Collectors.toList());

            return collect;
        }
        return null;
    }

    /**
     * 获取到我们要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        // 1 获取用户或者临时用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if(userInfoTo.getUserId() != null) cartKey = CART_PREFIX + userInfoTo.getUserId();
        else cartKey = CART_PREFIX + userInfoTo.getUserKey();

        // 2 查找当前购物车中是否已经存在该商品
        return redisTemplate.boundHashOps(cartKey);
    }
}
