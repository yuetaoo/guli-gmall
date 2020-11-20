package com.atguigu.gmall.cart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration  //配置
public class CartThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(
            @Value("${threadPool.coreSize}")Integer coreSize,
            @Value("${threadPool.maxSize}")Integer maxSize,
            @Value("${threadPool.keepAlive}")Integer keepAlive,
            @Value("${threadPool.blockingSize}")Integer blockingSize
    ){
        return new ThreadPoolExecutor(coreSize,maxSize,keepAlive, TimeUnit.SECONDS,
                //ArrayBlockingQueue阻塞队列大小
                new ArrayBlockingQueue<>(blockingSize));
    }
}