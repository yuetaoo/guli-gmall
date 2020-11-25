package com.atguigu.gmall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){ //设置生产者确认的回调
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息发送失败。交换机：{}, 路由键：{}, 消息内容：{}", exchange, routingKey, message);
        });
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if(ack){
                log.error("消息没有到达交换机：" + cause);
            }
        });
    }

    /**
     * 定时关单的延时队列
     * @return
     */
    @Bean
    public Queue ttlQueue(){
        return QueueBuilder.durable("ORDER_TTL_QUEUE")
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", "ORDER_EXCHANGE")
                .withArgument("x-dead-letter-routing-key", "order.dead").build();
    }

    //绑定延时队列
    @Bean
    public Binding ttlBinding(){
        return new Binding("ORDER_TTL_QUEUE", Binding.DestinationType.QUEUE, "ORDER_EXCHANGE", "order.ttl", null);
    }

}
