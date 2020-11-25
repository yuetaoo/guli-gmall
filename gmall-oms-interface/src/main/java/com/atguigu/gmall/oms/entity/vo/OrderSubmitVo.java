package com.atguigu.gmall.oms.entity.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken;
    //总价
    private BigDecimal totalPrice;
    //收货人地址
    private UserAddressEntity address;
    //积分
    private Integer bounds;
    //支付方式
    private Integer payType;
    //配送方式
    private String deliveryCompany;
    //商品清单
    private List<OrderItemVo> items;

}
