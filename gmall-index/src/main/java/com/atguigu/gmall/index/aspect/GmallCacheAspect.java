package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter<String> bloomFilter;

    @Around("@annotation(GmallCache)") //annotation指定要切的注解
    public Object around(ProceedingJoinPoint join) throws Throwable{
        // 环绕前通知
            //获取目标方法对象
        MethodSignature signature = (MethodSignature) join.getSignature();
        Method method = signature.getMethod();
            //获取目标方法的注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
            //获取目标方法的参数列表
        List<Object> args = Arrays.asList(join.getArgs());
            //组装缓存的key
        String key = prefix + args;
            //通过布隆过滤器检查数据库中是否存在这个key
        boolean flag = bloomFilter.contains(key);
        if(!flag){
            return null;
        }
        //先查询缓存
        String json = redisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, method.getReturnType());
        }
        //加分布式锁
            //获取注解lock属性
        String lock = gmallCache.lock();
        RLock fairLock = redissonClient.getFairLock(lock + args);
        fairLock.lock();
        Object result;
        try {
            //再查缓存
            String json1 = redisTemplate.opsForValue().get(key);
            if(StringUtils.isNotBlank(json1)){
                return JSON.parseObject(json1, method.getReturnType());
            }

            //执行目标方法
            result = join.proceed(join.getArgs());

            //环绕后通知
                //把结果放入缓存，如果result为null为了防止缓存穿透结果依然放入缓存（时间很短）
            if(result == null){
//                redisTemplate.opsForValue().set(key, null,1, TimeUnit.MINUTES);//使用布隆过滤器解决
            } else {
                //给缓存添加随机值,防止缓存雪崩
                long timeOut = gmallCache.timeout() + new Random().nextInt((int) gmallCache.random());
                redisTemplate.opsForValue().set(key,JSON.toJSONString(result), timeOut, TimeUnit.MINUTES);
            }
        } finally {
            fairLock.unlock();
        }

        return result;
    }

}
