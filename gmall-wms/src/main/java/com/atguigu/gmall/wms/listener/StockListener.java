package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.entity.vo.SkuLockVo;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


@Component
public class StockListener {

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "stock:lock";

    /**
     * 监听处理解锁库存的消息。
     * @param channel
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS_UNLOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlockListener(String orderToken, Channel channel, Message message){
        try {
            if(StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }
            //获取redis中锁定的库存信息
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
            if(StringUtils.isBlank(json)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }
            List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
            //遍历解锁库存
            skuLockVos.forEach(skuLockVo -> {
                wareSkuMapper.unLock(skuLockVo.getWareId(), skuLockVo.getCount());
            });
            //删除redis缓存的库存锁定数据
            redisTemplate.delete(KEY_PREFIX + orderToken);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
