package com.atguigu.gmall.index.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.lock.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private DistributedLock distributedLock;//自定义分布式锁

    @Autowired
    private RedissonClient redissonClient;//redisson框架提供的分布式锁

    private static final String KEY_PREFIX = "index:cates:";

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> categories = pmsClient.queryCategory(0L);
        return categories.getData();
    }

    //aop注解方式整合缓存
    @GmallCache(prefix = KEY_PREFIX, timeout = 129600L, random = 14400L, lock = "lock:cates:")
    public List<CategoryEntity> quueryLv2CategoryWithSubsByPid(Long pId){

        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.quueryLv2CategorySubsByPid(pId);
        return responseVo.getData();
    }

    //手动实现缓存
    public List<CategoryEntity> quueryLv2CategoryWithSubsByPid1(Long pId){
        //先查询缓存
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + pId);
        if(StringUtils.isNotBlank(json)){
            //命中缓存直接返回
            try {
                return MAPPER.readValue(json, new TypeReference<List<CategoryEntity>>(){});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        RLock lock = redissonClient.getLock("lock:" + pId);
        lock.lock();
        try {

            //再次去查询缓存，防止同时并发的多个请求再次查询数据库并写入缓存
            String json2 = redisTemplate.opsForValue().get(KEY_PREFIX + pId);
            if(StringUtils.isNotBlank(json2)){
                //命中缓存直接返回，同时在finally里释放锁
                try {
                    return MAPPER.readValue(json, new TypeReference<List<CategoryEntity>>(){});
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            //没有命中执行业务远程调用。最后放入缓存
            ResponseVo<List<CategoryEntity>> responseVo = pmsClient.quueryLv2CategorySubsByPid(pId);
            //放入缓存
            String categoryEntities = null;
            try {
                categoryEntities = MAPPER.writeValueAsString(responseVo.getData());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pId, categoryEntities);
            return responseVo.getData();
        } finally {
            lock.unlock();
        }
    }

    //redisson框架的分布式锁
    public void testLock(){
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        try {
            String numString = redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(numString)){
                return;
            }
            int num = Integer.parseInt(numString);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        } finally {
            lock.unlock();
        }
    }

    //自己实现分布式锁
    public void testLock2(){
        String uuid = UUID.randomUUID().toString();
        //获取锁
        Boolean tryLock = distributedLock.tryLock("lock", uuid, 30);
        try {
            String numString = redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(numString)){
                return;
            }
            int num = Integer.parseInt(numString);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
//            testSub("lock", uuid);//测试可重入
        } finally {
            distributedLock.unlock("lock", uuid);
        }
    }

    public void testSub(String lockName, String uuid){
        distributedLock.tryLock(lockName,uuid,30);
        System.out.println("测试可重入的分布式锁");
        distributedLock.unlock(lockName,uuid);
    }

    public void testLock1(){//简单实现分布式锁
        //尝试获取锁 。setIfAbsent如果不存在就执行。 设置过期时间解决服务器宕机产生的死锁问题
        String uuid = UUID.randomUUID().toString();
        Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", uuid,30, TimeUnit.SECONDS);
        if(!flag){
            try {
                Thread.sleep(50);
                testLock1();//重试
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            String numString = redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(numString)){
                return;
            }
            int num = Integer.parseInt(numString);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //释放锁, 删除之前判断是否是自己的锁，防止误删（使用lua脚本保证原子性）
            String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
                //注意 DefaultRedisScript需要指定返回值类型否则会报错
            redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
        }
    }
}
