package com.atguigu.gmall.sms.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.entity.vo.ItemSaleVo;
import com.atguigu.gmall.sms.entity.vo.SkuSaleVo;
import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuFullReductionMapper reductionMapper;

    @Autowired
    private SkuLadderMapper ladderMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public void saveSkuSaleInfo(SkuSaleVo skuSaleVo) {
        // 3.1. 积分优惠
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo, skuBoundsEntity);
        // 数据库保存的是整数0-15，页面绑定是0000-1111
        List<Integer> work = skuSaleVo.getWork();
        if (!CollectionUtils.isEmpty(work)){
            //数据库是tinyint类型数据,需要转换成十进制去保存
            skuBoundsEntity.setWork(work.get(0) * 8 + work.get(1) * 4 + work.get(2) * 2 + work.get(3));
        }
        this.save(skuBoundsEntity);

        // 3.2. 保存满减优惠
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo,reductionEntity);
        reductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        reductionMapper.insert(reductionEntity);

        // 3.3. 保存数量折扣
        SkuLadderEntity ladderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo, ladderEntity);
        ladderEntity.setAddOther(skuSaleVo.getLaddOther());
        ladderMapper.insert(ladderEntity);
    }

    @Override
    public List<ItemSaleVo> querySalesBySkuId(Long skuId) {
        //查询积分优惠
        SkuBoundsEntity boundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        ArrayList<ItemSaleVo> itemSaleVos = new ArrayList<>();
        if (boundsEntity != null) {
            ItemSaleVo itemSaleVo1 = new ItemSaleVo();
            itemSaleVo1.setType("积分");
            itemSaleVo1.setDesc("送" + boundsEntity.getGrowBounds() + "成长积分,送" + boundsEntity.getBuyBounds() + "购物积分");
            itemSaleVos.add(itemSaleVo1);
        }
        //查询满减优惠
        SkuFullReductionEntity reductionEntity = reductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if(reductionEntity != null){
            ItemSaleVo itemSaleVo2 = new ItemSaleVo();
            itemSaleVo2.setType("满减");
            itemSaleVo2.setDesc("满" + reductionEntity.getFullPrice() + "减" + reductionEntity.getReducePrice());
            itemSaleVos.add(itemSaleVo2);
        }
        //查询打折优惠
        SkuLadderEntity ladderEntity = ladderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(ladderEntity != null){
            ItemSaleVo itemSaleVo3 = new ItemSaleVo();
            itemSaleVo3.setType("打折");
            itemSaleVo3.setDesc("满" + ladderEntity.getFullCount() + "件,打" + ladderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            itemSaleVos.add(itemSaleVo3);
        }
        return itemSaleVos;
    }

}