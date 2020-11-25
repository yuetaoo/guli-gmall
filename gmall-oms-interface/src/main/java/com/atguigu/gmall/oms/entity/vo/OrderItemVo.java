package com.atguigu.gmall.oms.entity.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.entity.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs; // 销售属性：List<SkuAttrValueEntity>的json格式
    private BigDecimal price; // 价格
    private BigDecimal count = new BigDecimal(1);
    private Boolean store = false; // 是否有货
    private List<ItemSaleVo> sales; // 营销信息: List<ItemSaleVo>的json格式
    private Integer weight;//重量

}
