package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.entity.vo.SkuVo;
import com.atguigu.gmall.pms.entity.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.entity.vo.SpuVo;
import com.atguigu.gmall.pms.feign.GmallSmsClientPms;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.service.SpuService;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService attrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClientPms smsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuInfo(PageParamVo pageParamVo, Long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        // 如果分类id不为0，要根据分类id查，否则查全部
        if(categoryId != 0){
            wrapper.eq("category_id", categoryId);
        }
        // 如果用户输入了检索条件，根据检索条件查
        String key = pageParamVo.getKey();
        if(StringUtils.isNotBlank(key)){
            wrapper.and( r -> r.eq("id", key).or().like("name",key ));
        }
        IPage<SpuEntity> page = this.page(pageParamVo.getPage(), wrapper);
        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spuVo) {
        // 1.1 保存spu基本信息
        spuVo.setPublishStatus(1);// 默认是已上架
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime()); // 新增时，更新时间和创建时间一致
        this.save(spuVo);//保存spu
        Long spuId = spuVo.getId(); // 获取新增后的spuId

        // 1.2. 保存spu的描述信息 spu_desc
        List<String> spuImages = spuVo.getSpuImages();
        saveSpuDesc(spuId, spuImages);

        // 1.3. 保存spu_attr_value的规格参数信息
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        saveSpuAttrValue(spuId, baseAttrs);

        // 2. 保存sku相关信息
        List<SkuVo> skus = spuVo.getSkus();
        if(!CollectionUtils.isEmpty(skus)){
            // 2.1保存sku表
            skus.forEach(skuVo -> {
                skuVo.setSpuId(spuId);
                skuVo.setBrandId(spuVo.getBrandId());//品牌的id
                skuVo.setCatagoryId(spuVo.getCategoryId());//分类的id
                List<String> images = skuVo.getImages();
                if(!CollectionUtils.isEmpty(images)){
                    //判断skuVo是否有默认图片没有就取images的第一张图片
                    skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ?
                            skuVo.getDefaultImage() : images.get(0));
                }
                skuMapper.insert(skuVo);
                Long skuId = skuVo.getId();

                // 2.2保存sku图片表
                saveSkuImages(skuVo, images, skuId);

                // 2.3. 保存sku的规格参数（销售属性）
                List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
                if(!CollectionUtils.isEmpty(saleAttrs)){
                    saleAttrs.forEach(skuAttr -> { skuAttr.setSkuId(skuId); });
                    skuAttrValueService.saveBatch(saleAttrs);
                }

                // 3. 保存营销相关信息，需要远程调用gmall-sms
                SkuSaleVo saleVo = new SkuSaleVo();
                BeanUtils.copyProperties(skuVo,saleVo);
                saleVo.setSkuId(skuId);
                smsClient.saveSkuSaleInfo(saleVo);
            });
        }
        //保存完发送消息
        rabbitTemplate.convertAndSend("PMS_SPU_EXCHANGE", "item.insert", spuId);
    }

    private void saveSkuImages(SkuVo skuVo, List<String> images, Long skuId) {
        if(!CollectionUtils.isEmpty(images)){
            List<SkuImagesEntity> imagesEntities = images.stream().map(image -> {
                    SkuImagesEntity imagesEntity = new SkuImagesEntity();
                                                // 判断当前图片是否是默认图片，0 - 不是默认图，1 - 是默认图
                    imagesEntity.setDefaultStatus(StringUtils.equals(image,skuVo.getDefaultImage()) ? 1 : 0);
                    imagesEntity.setSkuId(skuId);
                    imagesEntity.setSort(0);
                    imagesEntity.setUrl(image);
                    return imagesEntity;
                }).collect(Collectors.toList());
            imagesService.saveBatch(imagesEntities);
        }
    }

    private void saveSpuAttrValue(Long spuId, List<SpuAttrValueVo> baseAttrs) {
        if(!CollectionUtils.isEmpty(baseAttrs)){
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs
                    .stream().map(SpuAttrValueVo -> {//把SpuAttrValueVo的集合转换为SpuAttrValueEntity类型的集合
                        SpuAttrValueVo.setSpuId(spuId);
                        SpuAttrValueVo.setSort(0);
                        return SpuAttrValueVo;//用父类类型接收子类类型
            }).collect(Collectors.toList());
            //saveBatch批量保存方法
            attrValueService.saveBatch(spuAttrValueEntities);
        }
    }

    private void saveSpuDesc(Long spuId, List<String> spuImages) {
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDesc = new SpuDescEntity();
            // 注意：spu_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
            spuDesc.setSpuId(spuId);
            // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
            spuDesc.setDecript(StringUtils.join(spuImages,","));
            descMapper.insert(spuDesc);
        }
    }
}