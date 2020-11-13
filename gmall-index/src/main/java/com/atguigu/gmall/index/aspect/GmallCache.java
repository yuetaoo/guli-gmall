package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented//添加到文档
public @interface GmallCache {

    /**
     * 缓存key的前缀 , 模块名:实例名:
     * @return
     */
    String prefix()default "gmall:cache:";

    /**
     * 缓存过期时间，单位分钟
     * @return
     */
    long timeout()default 5;

    /**
     * 防止缓存雪崩，给缓存时间添加随机值
     * 这里指定随机值范围
     * @return
     */
    long random()default 5l;

    /**
     * 防止缓存击穿，给缓存添加分布式锁
     * 这里指定分布式锁的前缀, lock:方法参数
     * @return
     */
    String lock()default "lock:";
}
