package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.entity.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 订单
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-31 11:49:00
 */
public interface OrderService extends IService<OrderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId);
}

