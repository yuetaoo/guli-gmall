package com.atguigu.gmall.wms.entity.vo;

import lombok.Data;

@Data
public class SkuLockVo {

    private Long skuId;

    private Integer count;
    //当前sku库存锁定状态
    private Boolean lock;
    //当前sku锁定的仓库id
    private Long wareId;
}
