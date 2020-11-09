package com.atguigu.gmall.oms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.oms.entity.OrderSettingEntity;
import com.atguigu.gmall.oms.mapper.OrderSettingMapper;
import com.atguigu.gmall.oms.service.OrderSettingService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service("orderSettingService")
public class OrderSettingServiceImpl extends ServiceImpl<OrderSettingMapper, OrderSettingEntity> implements OrderSettingService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderSettingEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderSettingEntity>()
        );

        return new PageResultVo(page);
    }

}