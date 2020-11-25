package com.atguigu.gmall.order.service;

import com.atguigu.gmall.oms.entity.vo.OrderSubmitVo;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

public interface OrderService {
    OrderConfirmVo confirm();

    String submit(OrderSubmitVo submitVo);
}
