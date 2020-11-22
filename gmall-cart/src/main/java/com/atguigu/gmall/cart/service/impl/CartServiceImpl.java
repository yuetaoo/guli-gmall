package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.entity.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsynService cartAsynService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

    @Override
    public void addCart(Cart cart) {
        //1.获取用户登录信息,用户登录了获取userId未登录获取userKey
        String userId = getUserId();

        //2.查询redis，获取用户购物车集合。 boundHashOps通过外层key获取内层的map结构
        BigDecimal count = cart.getCount();//要添加的商品数量
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        //3.判断用户购物车中是否包含当前商品
        if(hashOps.hasKey(cart.getSkuId().toString())){
            //包含，获取cart对象并修改数量
            String json = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(json, Cart.class);
            cart.setCount(cart.getCount().add(count));
            //修改完成写回redis

            //异步写mysql
            this.cartAsynService.updateCart(userId, cart);
            //写入redis
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        } else {
            //不包含,添加cart其它属性,新增一条记录

            Cart finalCart = cart;
            finalCart.setUserId(userId);
                //sku信息
            CompletableFuture<Void> skuFuture = null;
            try {
                skuFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<SkuEntity> skuById = pmsClient.querySkuById(finalCart.getSkuId());
                    SkuEntity skuEntity = skuById.getData();
                    if (skuEntity != null) {
                        finalCart.setTitle(skuEntity.getTitle());
                        finalCart.setPrice(skuEntity.getPrice());
                        finalCart.setDefaultImage(skuEntity.getDefaultImage());
                    } else {
                        throw new CartException("要添加的商品不存在！");
                    }
                }, threadPoolExecutor);
            } catch (Exception e) {
                skuFuture.completeExceptionally(e);//抛出子线程的异常
            }
            //库存信息
            CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
                ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = wmsClient.queryWareSkuBySkuId(finalCart.getSkuId());
                List<WareSkuEntity> wareSkuList = wareSkuBySkuId.getData();
                if (!CollectionUtils.isEmpty(wareSkuList)) {
                    finalCart.setStore(wareSkuList.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }
            }, threadPoolExecutor);
            //销售属性
            CompletableFuture<Void> attrFuture = CompletableFuture.runAsync(() -> {
                ResponseVo<List<SkuAttrValueEntity>> querySaleBySkuId = pmsClient.querySaleBySkuId(finalCart.getSkuId());
                List<SkuAttrValueEntity> skuAttr = querySaleBySkuId.getData();
                finalCart.setSaleAttrs(JSON.toJSONString(skuAttr));
            }, threadPoolExecutor);
            //营销信息
            CompletableFuture<Void> saleFuture = CompletableFuture.runAsync(() -> {
                ResponseVo<List<ItemSaleVo>> salesBySkuId = smsClient.querySalesBySkuId(finalCart.getSkuId());
                List<ItemSaleVo> sales = salesBySkuId.getData();
                finalCart.setSales(JSON.toJSONString(sales));
            }, threadPoolExecutor);
            finalCart.setCheck(true);
            CompletableFuture.allOf(skuFuture, wareFuture, attrFuture, saleFuture).join();
                //异步写mysql
            this.cartAsynService.insertCart(userId, finalCart);
            //添加价格缓存
            redisTemplate.opsForValue().set(PRICE_PREFIX + finalCart.getId(), finalCart.getPrice().toString());
            //写入redis
            hashOps.put(finalCart.getSkuId().toString(), JSON.toJSONString(finalCart));
        }
    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if(userInfo.getUserId() == null){
            userId = userInfo.getUserKey();
        } else {
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    @Override
    public Cart queryCartByskuId(Long skuId) {

        String userId = getUserId();
        //获取内层map
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        }
        throw new CartException("此用户不存在这条购物车记录");
    }

    @Override   //查询购物车
    public List<Cart> queryCarts() {
        //1.获取userKey,查询未登录购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> unLoginHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userKey);
        List<Object> unLoginCartJsons = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if(!CollectionUtils.isEmpty(unLoginCartJsons)){
            //cartJson类型是object 需要转换未string
            unLoginCarts = unLoginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //设置商品最新价格,
                cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        //2.获取userId, 判断是否为空，为空直接返回未登录购物车。
        Long userId = userInfo.getUserId();
        if(userId == null){
            return unLoginCarts;
        }

        //3.userId 不为空就把未登录的购物车合并到登录状态的购物车中
        BoundHashOperations<String, Object, Object> loginHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            //未登录状态购物车不为空
        if(!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal unLoginCount = cart.getCount();
                //登录状态购物车包含该记录，更新数量
                if(loginHashOps.hasKey(skuId)){
                    String cartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);//注意cart对象已经刷新
                    cart.setCount(cart.getCount().add(unLoginCount));
                    //异步更新mysql
                    cartAsynService.updateCart( userId.toString(), cart);
                } else {
                    //登录状态购物车不包含该记录，新增记录
                    cart.setUserId(userId.toString());
                    cartAsynService.insertCart(userId.toString(), cart);
                }
                //保存到redis
                loginHashOps.put(skuId, JSON.toJSONString(cart));
            });

            //4.删除未登录的购物车
            redisTemplate.delete(KEY_PREFIX + userKey);
            cartAsynService.deleteCart(userKey);//异步删除mysql未登录状态购物车
        }

        //5.查询登录状态的购物车并返回
        List<Object> loginCartJsons = loginHashOps.values();
        if(!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //设置实时价格
                cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    @Override
    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        BigDecimal count = cart.getCount();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            cartAsynService.updateCart(userId, cart);
        }
        throw new CartException("用户购物车不包含该条记录");
    }

    @Override
    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
            cartAsynService.deleteCartBySkuIdUserid(userId, skuId );
            return;
        }
        throw new CartException("用户购物车不存在该商品");
    }
}
