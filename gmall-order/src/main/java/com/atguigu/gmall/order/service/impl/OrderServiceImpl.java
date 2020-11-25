package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.vo.OrderSubmitVo;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.entity.vo.OrderItemVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.OrderLoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.entity.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.entity.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

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
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "order:token:";

    @Transactional
    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        //获取登录信息
        UserInfo userInfo = OrderLoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        //获取用户选中的购物车信息,判断是否为空
        ResponseVo<List<Cart>> CartResponseVo = cartClient.queryCartsByUserId(userId);
        List<Cart> cartList = CartResponseVo.getData();
        if(CollectionUtils.isEmpty(cartList)){
            throw new OrderException("您没有购物车记录");
        }
        confirmVo.setOrderItems(cartList.stream().map(cart -> {
            OrderItemVo orderitemVo = new OrderItemVo();
            orderitemVo.setCount(cart.getCount());

            ResponseVo<SkuEntity> skuResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuResponseVo.getData();
            if(skuEntity != null){
                orderitemVo.setSkuId(skuEntity.getId());
                orderitemVo.setTitle(skuEntity.getTitle());
                orderitemVo.setDefaultImage(skuEntity.getDefaultImage());
                orderitemVo.setWeight(skuEntity.getWeight());
                orderitemVo.setPrice(skuEntity.getPrice());
            }

            ResponseVo<List<WareSkuEntity>> wareResponseVo = wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuList = wareResponseVo.getData();
            if(!CollectionUtils.isEmpty(wareSkuList)){
                orderitemVo.setStore(wareSkuList.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = pmsClient.querySaleBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrList = skuAttrResponseVo.getData();
            orderitemVo.setSaleAttrs(skuAttrList);

            ResponseVo<List<ItemSaleVo>> saleResponseVo = smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = saleResponseVo.getData();
            orderitemVo.setSales(itemSaleVos);
            return orderitemVo;
        }).collect(Collectors.toList()));

        //获取用户收货地址列表
        ResponseVo<List<UserAddressEntity>> addressResponseVo = umsClient.queryAddressByUserId(userId);
        List<UserAddressEntity> userAddressList = addressResponseVo.getData();
        confirmVo.setAddresses(userAddressList);

        //获取用户购买积分
        ResponseVo<UserEntity> userResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userResponseVo.getData();
        if(userEntity != null){
            confirmVo.setBounds(userEntity.getIntegration());
        }

        //防重orderToken, IdWorker雪花算法的工具类
        String orderToken = IdWorker.getTimeId();
        confirmVo.setOrderToken(orderToken);
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken, 12, TimeUnit.HOURS);
        return confirmVo;
    }

    @Override
    public String submit(OrderSubmitVo submitVo) {
        //1.防重
        String orderToken = submitVo.getOrderToken();
        if(StringUtils.isBlank(orderToken)){
            throw new OrderException("请求不合法");
        }
        String script = "if(redis.call('exists', KEYS[1]) == 1) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), "0");
        if(!flag){
            throw new OrderException("请不要重复提交");
        }

        //2.验总价
        List<OrderItemVo> items = submitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("你没有选中的购物车记录");
        }
            //页面价格
        BigDecimal voTotalPrice = submitVo.getTotalPrice();
            //数据库的实时价格
        BigDecimal totalPrice = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuResponseVo = pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuResponseVo.getData();
            if (skuEntity != null) {
                return skuEntity.getPrice().multiply(item.getCount());
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        if(totalPrice.compareTo(voTotalPrice) != 0){
            throw new OrderException("页面已过期，请重试");
        }

        //3.验库存并锁定库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo lockVo = new SkuLockVo();
            lockVo.setSkuId(item.getSkuId());
            lockVo.setCount(item.getCount().intValue());
            return lockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> lockResponseVo = wmsClient.checkAndLock(lockVos, orderToken);
        List<SkuLockVo> skuLockVos = lockResponseVo.getData();
            //全部锁定成功返回null，否则返回锁定列表
        if(!CollectionUtils.isEmpty(skuLockVos)){
                //把锁定列表响应给前端
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }

//        int i = 1 / 0;

        //4.创建订单
        UserInfo userInfo = OrderLoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try {
            omsClient.saveOrder(submitVo, userId);
            //订单一创建成功，就发送消息定时关单
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", orderToken);
//            int i = 1 / 0;
        } catch (Exception e) {
            e.printStackTrace();
            //发送消息把当前订单标记为无效订单
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.invalid", orderToken);
            throw new OrderException("服务器错误，订单创建失败!");
        }

        //5.删除购物车
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", JSON.toJSONString(skuIds));
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.delCart", map);

        return orderToken;
    }
}
