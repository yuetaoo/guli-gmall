package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValueBySkuidAndCid(Long skuId, Long cid) {
        //先查询出检索类型的规格参数
        List<AttrEntity> attrEntities = attrMapper.selectList(new QueryWrapper<AttrEntity>()
                .eq("search_type", 1).eq("category_id", cid));
        if(CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        //查询规格参数的值
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
    }

    @Override
    public List<SkuAttrValueEntity> querySaleBySkuId(Long skuId) {
        List<SkuAttrValueEntity> attrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId));
        return attrValueEntities;
    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrValuesByspuId(Long spuId) {
        List<SkuEntity> skuEntities = skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if(!CollectionUtils.isEmpty(skuEntities)){
            List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
            List<SkuAttrValueEntity> skuAttrEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
            if(!CollectionUtils.isEmpty(skuAttrEntities)){
                //对所有销售属性进行分组指定key
                Map<Long, List<SkuAttrValueEntity>> attrIdGroupMap = skuAttrEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));

                List<SaleAttrValueVo> attrValueVos = new ArrayList<>();
                //遍历map 转换为 List<SaleAttrValueVo>集合
                attrIdGroupMap.forEach((attrId, attrValues) -> {
                    SaleAttrValueVo attrValueVo = new SaleAttrValueVo();
                    attrValueVo.setAttrId(attrId);
                    if(!CollectionUtils.isEmpty(attrValues)){
                        attrValueVo.setAttrName(attrValues.get(0).getAttrName());
                        //取attrIdGroupMap的value值(是个SkuAttrValueEntity类型集合)的value属性值的集合
                        attrValueVo.setAttrValues(attrValues.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
                    }
                    attrValueVos.add(attrValueVo);
                });
                return attrValueVos;
            }
        }
        return null;
    }

    @Override
    public String querySaleAttrValuesMappingSkuId(Long spuId) {
        List<Map<String, Object>> skuAttrList = skuAttrValueMapper.querySaleAttrValuesMappingSkuIdMapper(spuId);
        if(!CollectionUtils.isEmpty(skuAttrList)){
            //map相当于skuAttrMaps中的一个元素
            Map<String, String> skuAttrsMap = skuAttrList.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> map.get("sku_id").toString()));
            return JSON.toJSONString(skuAttrsMap);
        }
        return null;
    }

}