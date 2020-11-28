package com.atguigu.gmall.index.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LockTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public Boolean tryLock(){
        String script = "";

        return null;
    }
}
