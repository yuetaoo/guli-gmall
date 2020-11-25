package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.entity.vo.SkuLockVo;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "stock:lock";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(String orderToken,List<SkuLockVo> lockVos) {
        if(CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("没有要购买的商品");
        }
        //购物清单所有商品都要进行库存锁定
        lockVos.forEach(lockVo -> {
            this.checkLock(lockVo);
        });

        //判断集合是否有库存锁定失败的
        if(lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())){
            //有，则解锁锁定成功的库存
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            successLockVos.forEach(lockVo -> {
                wareSkuMapper.unLock(lockVo.getWareId(), lockVo.getCount());
            });
            return lockVos;
        }

        //锁定成功的库存信息缓存到redis,方便将来解锁库存
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));
        //发送延时消息定时解锁库存
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);

        //都锁定成功
        return null;
    }

    private void checkLock(SkuLockVo lockVo){
        RLock fairLock = redissonClient.getFairLock("stock:" + lockVo.getSkuId());

        //查询库存信息
        List<WareSkuEntity> wareSkuEntities = wareSkuMapper.checkLock(lockVo.getSkuId(), lockVo.getCount());
        if(CollectionUtils.isEmpty(wareSkuEntities)){
            //没有仓库满足购买需求，锁定失败
            fairLock.unlock();
            lockVo.setLock(false);
            return;
        }

        //锁定库存信息
        WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
        int lock = wareSkuMapper.lock(wareSkuEntity.getId(), lockVo.getCount());
        if(lock == 1){
            //锁定成功
            lockVo.setLock(true);
            lockVo.setWareId(wareSkuEntity.getId());
            fairLock.unlock();
        }
        fairLock.unlock();
    }

}