package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.entity.vo.OrderItemVo;
import com.atguigu.gmall.oms.entity.vo.OrderSubmitVo;
import com.atguigu.gmall.oms.feign.*;
import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.service.OrderService;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private OrderItemService itemService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {
        OrderEntity orderEntity = new OrderEntity();
        //保存订单表
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice());
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());

        UserAddressEntity address = submitVo.getAddress();
        if(address != null){
            orderEntity.setReceiverAddress(address.getAddress());
            orderEntity.setReceiverRegion(address.getRegion());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverName(address.getName());
        }
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        ResponseVo<UserEntity> userResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userResponseVo.getData();
        if(userEntity != null){
            orderEntity.setUsername(userEntity.getUsername());
        }
//保存
        this.save(orderEntity);
        Long orderId = orderEntity.getId();

        //保存订单详情
        List<OrderItemVo> items = submitVo.getItems();
        List<OrderItemEntity> itemEntities = items.stream().map(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrderId(orderId);

            ResponseVo<SkuEntity> skuResponseVo = pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuResponseVo.getData();
            if(skuEntity != null){
                itemEntity.setSkuId(skuEntity.getId());
                itemEntity.setSkuPic(skuEntity.getDefaultImage());
                itemEntity.setSkuPrice(skuEntity.getPrice());
                itemEntity.setSkuQuantity(item.getCount().intValue());
                itemEntity.setSkuName(skuEntity.getName());
            }

            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = pmsClient.querySaleBySkuId(item.getSkuId());
            List<SkuAttrValueEntity> skuAttrEntities = skuAttrResponseVo.getData();
            itemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrEntities));

            ResponseVo<SpuEntity> spuResponseVo = pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuResponseVo.getData();
            if(spuEntity != null){
                itemEntity.setSpuId(spuEntity.getId());
                itemEntity.setSpuName(spuEntity.getName());
                itemEntity.setCategoryId(spuEntity.getCategoryId());
            }

            ResponseVo<SpuDescEntity> spuDescResponseVo = pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescResponseVo.getData();
            if(spuDescEntity != null){
                itemEntity.setSkuPic(spuDescEntity.getDecript());
            }
            return itemEntity;
        }).collect(Collectors.toList());
//保存
        itemService.saveBatch(itemEntities);
        return null;
    }

}