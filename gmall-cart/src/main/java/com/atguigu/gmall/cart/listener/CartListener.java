package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.service.impl.CartAsynService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsynService asynService;

    private static final String PRICE_PREFIX = "cart:price:";
    private static final String KEY_PREFIX = "cart:info:";

    /**
     * 监听spu修改消息。同步redis中sku的实时价格缓存
     * @param spuId
     * @param channel
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_PRICE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message){
        try {
            if(spuId == null){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }
            ResponseVo<List<SkuEntity>> responseVo = pmsClient.querySkuBySpuId(spuId);
            List<SkuEntity> skuEntities = responseVo.getData();
            if(!CollectionUtils.isEmpty(skuEntities)){
                skuEntities.forEach(skuEntity -> {
                    //setIfPresent 如果存在才设置
                    redisTemplate.opsForValue().setIfPresent(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                });
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 接收订单服务发送的删除购物车消息
     * @param map 接收userId ,和要删除的skuIds
     * @param channel
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_DELETE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.delCart"}
    ))
    public void orderDelCart(Map<String, Object> map, Channel channel, Message message){
        try {
            if(CollectionUtils.isEmpty(map)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }
            String userId = map.get("userId").toString();
            String skuIdsJson = map.get("skuIds").toString();
            if(StringUtils.isBlank(skuIdsJson)){    //如果接收的参数为空，直接确认
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }
            List<String> skuIds = JSON.parseArray(skuIdsJson, String.class);

            //删除redis
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            hashOps.delete(skuIds.toArray());

            //删除mysql
            asynService.deleteCartBySkuidsAndUserid(userId, skuIds);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
