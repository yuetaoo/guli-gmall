package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.pms.entity.vo.AttrVo;
import com.atguigu.gmall.pms.entity.vo.GroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private AttrGroupMapper attrGroupMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> queryByCid(Long catId) {
        // 查询所有的分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>()
                .eq("category_id", catId));
        if(CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        //遍历分组，查询每组下的规格参数
        groupEntities.forEach(group -> {
            List<AttrEntity> list = attrMapper.selectList(new QueryWrapper<AttrEntity>()
                    .eq("group_id", group.getId()).eq("type",1));
            group.setAttrEntities(list);
        });
        return groupEntities;
    }

    @Override
    public List<GroupVo> queryGroupsBySpuSkuCid(Long spuId, Long skuId, Long cId){
        List<AttrGroupEntity> attrGroupList = attrGroupMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("category_id", cId));
        if(CollectionUtils.isEmpty(attrGroupList)){
            return null;
        }
        return attrGroupList.stream().map(attrGroup -> {
            GroupVo groupVo = new GroupVo();
            groupVo.setGroupId(attrGroup.getId());
            groupVo.setGroupName(attrGroup.getName());
            List<AttrEntity> attrList = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroup.getId()));
            if(!CollectionUtils.isEmpty(attrList)){
                List<AttrVo> attrVo = new ArrayList<>();
                //获取attr的id集合
                List<Long> atttrIds = attrList.stream().map(AttrEntity::getId).collect(Collectors.toList());

                //查询spuAttrValue
                List<SpuAttrValueEntity> spuAttrs = spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", atttrIds));
                if(!CollectionUtils.isEmpty(spuAttrs)){
                    attrVo.addAll(spuAttrs.stream().map(spuAttrEntity -> {
                        AttrVo spuAttrVo = new AttrVo();
                        BeanUtils.copyProperties(spuAttrEntity, spuAttrVo);
                        return spuAttrVo;
                    }).collect(Collectors.toList()));
                }

                //查询skuAttrValue
                List<SkuAttrValueEntity> skuAttrs = skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", atttrIds));
                if(!CollectionUtils.isEmpty(skuAttrs)){
                    attrVo.addAll(skuAttrs.stream().map(skuAttr -> {
                        AttrVo skuAttrVo = new AttrVo();
                        BeanUtils.copyProperties(skuAttr, skuAttrVo);
                        return skuAttrVo;
                    }).collect(Collectors.toList()));
                }
                groupVo.setAttrValues(attrVo);
            }
            return groupVo;
        }).collect(Collectors.toList());
    }

}