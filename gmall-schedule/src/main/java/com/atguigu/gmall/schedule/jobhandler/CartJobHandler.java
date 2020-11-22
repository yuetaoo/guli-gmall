package com.atguigu.gmall.schedule.jobhandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.schedule.entity.Cart;
import com.atguigu.gmall.schedule.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    private static final String EXCEPTION_KEY = "cart:exception";

    private static final String KEY_PREFIX = "cart:info:";

    @XxlJob("cartJobHandler")
    public ReturnT<String> handler(String param){

        BoundSetOperations<String, String> setOps = redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();//获取并删除一个随机元素，集合为空或不存在时返回null

        while(StringUtils.isNotBlank(userId)){
            //清空用户购物车
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));
            //读取redis中该用户的购物车记录
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            if(!CollectionUtils.isEmpty(cartJsons)){
                cartJsons.forEach(cartJson -> {
                    Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                    cartMapper.insert(cart);
                });
            }
            userId = setOps.pop();
        }
        return ReturnT.SUCCESS;
    }
}
