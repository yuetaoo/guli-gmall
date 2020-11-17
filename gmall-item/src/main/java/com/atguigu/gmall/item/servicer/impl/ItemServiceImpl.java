package com.atguigu.gmall.item.servicer.impl;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.servicer.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.entity.vo.GroupVo;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.entity.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public ItemVo loadData(Long skuId) {

        ItemVo itemVo = new ItemVo();
        itemVo.setSkuId(skuId);

        //sku表 sku详细信息
        CompletableFuture<SkuEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuResponseVo = pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuResponseVo.getData();
            if (skuEntity == null) {
                return null;
            }
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;
        },threadPoolExecutor);

        //sku_image表 根据skuId查询sku图片信息
        CompletableFuture<Void> skuImagesCF = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> responseVo = pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesList = responseVo.getData();
            if (!CollectionUtils.isEmpty(skuImagesList)) {
                itemVo.setImages(skuImagesList);
            }
        }, threadPoolExecutor);

        //category表 根据categoryId查询面包屑需要的字段(三级分类)
        CompletableFuture<Void> categoryCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> categoryByCid3Response = pmsClient.queryAllCategoryByCid3(skuEntity.getCatagoryId());
            List<CategoryEntity> categoryList = categoryByCid3Response.getData();
            if (!CollectionUtils.isEmpty(categoryList)) {
                itemVo.setCategories(categoryList);
            }
        }, threadPoolExecutor);

        //category_brand表 根据brandId查询商品信息
        CompletableFuture<Void> brandCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandResponseVo.getData();
            if (brandResponseVo != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        //spu表 根据spuId查询spu信息
        CompletableFuture<Void> spuCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuResponseVo = pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        //营销（优惠）信息 根据skuId查询营销信息
        CompletableFuture<Void> saleVoCF = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> saleSmsResponseVo = smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> saleVoList = saleSmsResponseVo.getData();
            if (!CollectionUtils.isEmpty(saleVoList)) {
                itemVo.setSales(saleVoList);
            }
        }, threadPoolExecutor);

        //是否有货 根据skuId查询库存信息
        CompletableFuture<Void> wareSkuCF = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                //anyMatch 流中是否有任何元素匹配
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        //根据spuid查询spu下的所有销售属性及值
        CompletableFuture<Void> spuAttrCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleResponseVo = pmsClient.querySaleAttrValuesByspuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValues = saleResponseVo.getData();
            if (!CollectionUtils.isEmpty(saleAttrValues)) {
                itemVo.setSaleAttrs(saleAttrValues);
            }
        }, threadPoolExecutor);

        //根据skuId查询当前sku的销售属性（当前商品的销售属性） {4:"暗夜黑", 5:"8G"}
        CompletableFuture<Void> skuAttrCF = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> saleBySkuIdResponse = pmsClient.querySaleBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValues = saleBySkuIdResponse.getData();
            if (!CollectionUtils.isEmpty(skuAttrValues)) {
                itemVo.setSaleAttr(skuAttrValues.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, threadPoolExecutor);

        //根据spuId查询spu下的销售属性组合和skuId的映射关系 {"暗夜黑","8G","128G":100, "皓雪白","6G","256G":101}
        CompletableFuture<Void> skuJsonCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> saleAttrValueResponse = pmsClient.querySaleAttrValuesMappingSkuId(skuEntity.getSpuId());
            String attrValue = saleAttrValueResponse.getData();
            if (StringUtils.isNotBlank(attrValue)) {
                itemVo.setSkuJson(attrValue);
            }
        }, threadPoolExecutor);

        //根据spuId查询spu商品介绍
        CompletableFuture<Void> spuDescCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> descResponseVo = pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity descSpuEntity = descResponseVo.getData();
            if (descSpuEntity != null) {
                String spuDecript = descSpuEntity.getDecript();
                List<String> spuDecriptList = Arrays.asList(StringUtils.split(spuDecript, ","));
                itemVo.setSpuImages(spuDecriptList);
            }
        }, threadPoolExecutor);

        //根据cId,skuId,spuId查询规格与包装
        CompletableFuture<Void> groupVoCF = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<GroupVo>> groupResponseVo = pmsClient.queryGroupsBySpuIdAndCid(skuEntity.getSpuId(), skuId, skuEntity.getCatagoryId());
            List<GroupVo> groupVoList = groupResponseVo.getData();
            itemVo.setGroups(groupVoList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(skuImagesCF, categoryCF, brandCF, spuCF, saleVoCF, wareSkuCF, spuAttrCF, skuAttrCF,
                skuJsonCF, spuDescCF, groupVoCF).join();
        return itemVo;
    }
}
