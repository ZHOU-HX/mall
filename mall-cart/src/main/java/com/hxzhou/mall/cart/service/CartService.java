package com.hxzhou.mall.cart.service;

import com.hxzhou.mall.cart.vo.Cart;
import com.hxzhou.mall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 将商品添加到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车中的某个购物项
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    /**
     * 获取购物车内容
     * @return
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车数据
     * @param cartKey
     */
    void clearCart(String cartKey);

    /**
     * 勾选购物项来更新购物车信息
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 更改购物车商品数量
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物车内商品
     * @param skuId
     */
    void deleteItem(Long skuId);

    List<CartItem> getUserCartItem();
}
