package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId")Long skuId){
        cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @GetMapping("cart.html")    //购物车展示页面
    public String queryCarts(Model model){
        List<Cart> carts =  this.cartService.queryCarts();
        model.addAttribute("carts" , carts);
        return "cart";
    }

    /**
     * 加入购物车，重定向到新增购物车成功页面
     * @param cart
     * @return
     */
    @GetMapping
    public String addCart(Cart cart){
        if(cart == null || cart.getSkuId() == null){
            throw new RuntimeException("没有选择添加到购物车的商品信息!");
        }
        cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }

    /**
     * 新增购物车成功页面
     * @param
     * @return
     */
    @GetMapping("addCart.html")
    public String addCart(@RequestParam("skuId")Long skuId, Model model){
        Cart cart = cartService.queryCartByskuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    @GetMapping("test")
    public String test(HttpServletRequest request){
        System.out.println("Handler中拿到的数据:" + LoginInterceptor.getUserInfo());
        return "hell interceptors";
    }
}
