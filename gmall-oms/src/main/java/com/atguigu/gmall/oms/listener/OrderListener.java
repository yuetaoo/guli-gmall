package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String PRICE_PREFIX = "cart:price:";
    private static final String KEY_PREFIX = "cart:info:";

    /**
     * 监听订单状态修改的消息。同步redis中sku的实时价格缓存
     * @param channel
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "INVALID_ORDER_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.invalid"}
    ))
    public void invalidListener(String orderToken, Channel channel, Message message){
        try {
            if(StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }

            if(orderMapper.updataStatus(orderToken, 0, 5) == 1){
                //如果订单标记无效成功，发送消息给wms解锁库存
                rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 监听定时关单
     * @param channel
     * @param message
     */
    @RabbitListener(queues = "ORDER_DEAD_QUEUE")
    public void closeListener(String orderToken, Channel channel, Message message){
        try {
            if(StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }

            if(orderMapper.updataStatus(orderToken, 0, 4) == 1){
                //如果订单关闭成功，发送消息解锁库存
                rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
