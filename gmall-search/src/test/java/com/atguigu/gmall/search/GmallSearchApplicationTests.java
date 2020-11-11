package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Test
    void contextLoads() {

        //初始化索引库和映射
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);

        //分页查询spu
        Integer pageNum = 1;
        Integer pageSize = 100;
        do {
            PageParamVo pageVo = new PageParamVo();
            pageVo.setPageNum(pageNum);
            pageVo.setPageSize(pageSize);
            ResponseVo<List<SpuEntity>> spuResponseVo = pmsClient.querySpuByPageJson(pageVo);
            List<SpuEntity> spuEntityes = spuResponseVo.getData();
            if(CollectionUtils.isEmpty(spuEntityes)){
                break;
            }
            //查询spu下所有sku集合
            spuEntityes.forEach(spu -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spu.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
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
                        goods.setCreateTime(spu.getCreateTime());

                        //查询库存相关信息
                        ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsClient
                                .queryWareSkuBySkuId(spu.getId());
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
                                .querySearchSpuAttrValueBySpuIdAndCid(sku.getId(), sku.getCatagoryId());
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
            });
            pageNum++;
            pageSize = spuEntityes.size();
        } while (pageSize == 100);
    }

}
