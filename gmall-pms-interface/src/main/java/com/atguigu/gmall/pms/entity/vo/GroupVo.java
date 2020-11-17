package com.atguigu.gmall.pms.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupVo {

    private Long groupId;
    private String groupName;
    //规格参数属性 值
    private List<AttrVo> attrValues;
}
