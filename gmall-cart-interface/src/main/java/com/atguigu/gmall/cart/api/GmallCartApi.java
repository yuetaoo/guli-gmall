package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GmallCartApi {

    @GetMapping("user/{userId}")
    public ResponseVo<List<Cart>> queryCartsByUserId(@PathVariable("userId")Long userId);
}
