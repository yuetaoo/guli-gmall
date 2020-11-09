package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseAttrVo {

    //规格参数对象
    private Long attrId;
    private String attrName;
    private List<String> attrValues;
}
