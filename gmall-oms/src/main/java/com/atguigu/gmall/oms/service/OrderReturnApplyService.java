package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.oms.entity.OrderReturnApplyEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 订单退货申请
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-31 11:49:00
 */
public interface OrderReturnApplyService extends IService<OrderReturnApplyEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

