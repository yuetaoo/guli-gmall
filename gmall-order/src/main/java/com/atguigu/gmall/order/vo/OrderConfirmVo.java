package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.entity.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data   //订单确认页模型
public class OrderConfirmVo {

    //收货地址
    private List<UserAddressEntity> addresses;

    //送货清单，根据购物车页面传递过来的skuIds查询
    private List<OrderItemVo> orderItems;

    // 用户的购物积分信息，ums_member表中的integration字段
    private Integer bounds;

    // 防重的唯一标识
    private String orderToken;
}

//订单确认需要的接口
//1.根据用户id获取用户收货地址列表
//2.根据用户id查询用户选中的购物车记录
//3.根据skuId查询sku相关信息（销售属性，库存信息，营销信息）
//4.根据用户id查询用户购物积分

