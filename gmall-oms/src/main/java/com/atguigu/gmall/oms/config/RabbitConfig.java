package com.atguigu.gmall.oms.config;

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
    public void init(){
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack){
                log.error("消息没有到达交换机：" + cause);
            }
        });
        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息发送失败。交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, new String(message.getBody()));
        });
    }

    //声明死信队列
    @Bean
    public Queue deadQueue(){
        return QueueBuilder.durable("ORDER_DEAD_QUEUE").build();
    }

    //死信队列绑定交换机
    @Bean
    public Binding deadBinding(){
        return new Binding("ORDER_DEAD_QUEUE", Binding.DestinationType.QUEUE, "ORDER_EXCHANGE", "order.dead", null);
    }
}
