package com.atguigu.gmall.index.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Thread thread;

    public Boolean tryLock(String lock ,String uuid, Integer expireTime){
        String script = "if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('hincrby',KEYS[1], ARGV[1], 1); redis.call('expire', KEYS[1], ARGV[2]); return 1; else return 0; end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lock), uuid, expireTime.toString());
        if(!flag){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tryLock(lock,uuid,expireTime);
        }
        renewTime(lock , uuid, expireTime);//调用锁的续期线程
        return true;
    }

    public void unlock(String lock ,String uuid){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return nil; elseif(redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0) then redis.call('del', KEYS[1]) return 1; else return 0;end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lock), uuid);
        if(flag == null){
            throw new RuntimeException("您要释放的锁不存在");
        }
        //释放锁时销毁续期锁的线程
        thread.interrupt();
    }

    //自动续期锁的过期时间
    private void renewTime(String lockName, String uuid, Integer time){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) ==1) then redis.call('expire',KEYS[1], ARGV[2]); return 1;else return 0; end";
        thread = new Thread(() -> {
            while(true){
                try {
                    Thread.sleep(time * 2000/3);
                    redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, time.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
