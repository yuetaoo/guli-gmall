package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-31 11:49:00
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    int updataStatus(@Param("orderToken") String orderToken, @Param("expect")Integer expect, @Param("target")Integer target);
}
