package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsynService {

    @Autowired
    private CartMapper cartMapper;

    @Async
    public void updateCart(String userId, Cart cart) {
        cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", cart.getSkuId()));
    }

    @Async
    public void insertCart(String userId, Cart cart) {
        int a = 1 / 0;
        cartMapper.insert(cart);
    }

    @Async
    public void deleteCart(String userKey) {
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userKey));
    }

    @Async
    public void deleteCartBySkuIdUserid(String userId, Long skuId) {
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}
