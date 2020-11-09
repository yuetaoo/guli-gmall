package com.atguigu.gmall.pms.entity.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class SpuAttrValueVo extends SpuAttrValueEntity {

    public void setValueSelected(List<String> valueSelected) {
        if (CollectionUtils.isEmpty(valueSelected)){
            return;
        }       //把集合中的元素以 ，进行拼接
        this.setAttrValue(StringUtils.join(valueSelected, ","));
    }
}
