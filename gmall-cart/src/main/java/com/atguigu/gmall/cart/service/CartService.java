package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;

import java.util.List;

public interface CartService {

    void addCart(Cart cart);

    Cart queryCartByskuId(Long skuId);

    List<Cart> queryCarts();

    void updateNum(Cart cart);

    void deleteCart(Long skuId);

    List<Cart> queryCheckCartsByUserId(Long userId);
}
