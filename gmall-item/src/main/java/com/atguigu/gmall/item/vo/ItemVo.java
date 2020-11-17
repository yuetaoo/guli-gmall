package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.vo.GroupVo;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.entity.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    //面包屑需要的字段(三级分类)
    private List<CategoryEntity> categories;
    private Long brandId;
    private String brandName;
    private Long spuId;
    private String spuName;

    //sku详细信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;
    private List<SkuImagesEntity> images;

    //营销（优惠）信息
    private List<ItemSaleVo> sales;

    //是否有货
    private Boolean store = false;

    //spu下的所有销售属性及值
    // [{attrId: 4, attrName: 颜色, attrValues: ["暗夜黑", "白天白"]},{attrId: 5, attrName: 内存, attrValues: ["6G", "8G"]}]
    private List<SaleAttrValueVo> saleAttrs;

    //当前sku的销售属性（当前商品的销售属性） {4:"暗夜黑", 5:"8G"}
    private Map<Long, String> saleAttr;

    //销售属性组合和skuId的映射关系 {"暗夜黑","8G","128G":100, "皓雪白","6G","256G":101}
    private String skuJson;

    //商品介绍
    private List<String> spuImages;

    //规格与包装
    private List<GroupVo> groups;
}
//需要的接口
//1.根据skuId查询sku信息  y
//2.根据sku表中的三级分类ID查询一二级分类  y
//3.根据sku表中的品牌Id查询品牌信息  y
//4.根据sku表的spuId查询spu信息  y
//5.根据skuId查询sku图片信息  y
//6.根据skuId查询sku的营销信息 y
//7.根据skuId查询sku库存信息  y
//8.根据spuId查询spu下所有sku的销售属性  y
//9.根据skuId查询sku的销售属性  y
//10.根据spuId查询spu下所有销售属性和skuId的对应关系  y
//11.根据spuId查询spuDesc  y
//12.根据三级分类id,skuId,spuId查询出分组及组下规格参数和值  y
