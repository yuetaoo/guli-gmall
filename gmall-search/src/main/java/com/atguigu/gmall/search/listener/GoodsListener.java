package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_SAVE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))  //通过消息队列同步mysql数据库和es中的数据
    public void listener(Long spuId , Channel channel, Message message) throws IOException {
        try {
            ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spuId);
            List<SkuEntity> skuEntities = skuResponseVo.getData();
            //同步spu下所有sku数据
            if(!CollectionUtils.isEmpty(skuEntities)){
                List<Goods> goodsList = skuEntities.stream().map(sku -> {
                    Goods goods = new Goods();
                    //设置sku相关信息
                    goods.setSkuId(sku.getId());
                    goods.setTitle(sku.getTitle());
                    goods.setSubTitle(sku.getSubtitle());
                    goods.setPrice(sku.getPrice().doubleValue());
                    goods.setDefaultImage(sku.getDefaultImage());

                    //spu相关信息
                    ResponseVo<SpuEntity> responseVo = pmsClient.querySpuById(spuId);
                    SpuEntity spu = responseVo.getData();
                    if(spu != null){
                        goods.setCreateTime(spu.getCreateTime());
                    }

                    //查询库存相关信息
                    ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsClient
                            .queryWareSkuBySkuId(sku.getId());
                    List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                    if(!CollectionUtils.isEmpty(wareSkuEntities)){
                        //设置库存相关
                        goods.setStore(wareSkuEntities.stream().anyMatch( wareSku ->
                                //已有库存减锁定库存大于0 表示有货
                                wareSku.getStock() - wareSku.getStockLocked() > 0 ));

                        //设置销量
                        goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales)
                                .reduce((a , b) -> a + b).get());
                    }
                    //设置品牌相关
                    ResponseVo<BrandEntity> brandResponseVo = pmsClient.queryBrandById(sku.getBrandId());
                    BrandEntity brandEntity = brandResponseVo.getData();
                    if (brandEntity != null) {
                        goods.setBrandId(brandEntity.getId());
                        goods.setBrandName(brandEntity.getName());
                        goods.setLogo(brandEntity.getLogo());
                    }
                    //设置分类相关
                    ResponseVo<CategoryEntity> categoryResponseVo = pmsClient.queryCategoryById(sku.getCatagoryId());
                    CategoryEntity categoryEntity = categoryResponseVo.getData();
                    if(categoryEntity != null){
                        goods.setCategoryId(categoryEntity.getId());
                        goods.setCategoryName(categoryEntity.getName());
                    }
                    //设置规格参数相关
                    List<SearchAttrValue> searchAttrValues = new ArrayList<>();//创建最终集合
                    //销售类型的检索规格参数
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = pmsClient
                            .querySearchAttrValueBySkuidAndCid(sku.getId(), sku.getCatagoryId());
                    List<SkuAttrValueEntity> skuAttrValueList = skuAttrValueResponseVo.getData();
                    if(!CollectionUtils.isEmpty(skuAttrValueList)){
                        //SkuAttrValueEntity类型集合转换成skuAttrValueList类型并放到最终集合里
                        searchAttrValues.addAll(skuAttrValueList.stream().map(skuValue -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            BeanUtils.copyProperties(skuValue, searchAttrValue);
                            return searchAttrValue;
                        }).collect(Collectors.toList()));
                    }
                    //基本类型的检索规格参数
                    ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVo = pmsClient
                            .querySearchSpuAttrValueBySpuIdAndCid(sku.getSpuId(), sku.getCatagoryId());
                    List<SpuAttrValueEntity> spuAttrValueList = spuAttrValueResponseVo.getData();
                    if(!CollectionUtils.isEmpty(spuAttrValueList)){
                        searchAttrValues.addAll(spuAttrValueList.stream().map(spuAttrValue -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            BeanUtils.copyProperties(spuAttrValue, searchAttrValue);
                            return searchAttrValue;
                        }).collect(Collectors.toList()));
                    }
                    goods.setSearchAttrs(searchAttrValues);
                    return goods;
                }).collect(Collectors.toList());
                this.goodsRepository.saveAll(goodsList);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}
