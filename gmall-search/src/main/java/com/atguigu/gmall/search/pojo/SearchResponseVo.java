package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {
    //搜索结果集数据模型

    //品牌集合
    private List<BrandEntity> brands;

    //分类集合
    private List<CategoryEntity> categories;

    //规格参数集合
    private List<SearchResponseAttrVo> filters;

    //分页结果集
    private Integer pageNum;
    private Integer pageSize;
    //总记录数
    private Long total;

    //商品数据
    private List<Goods> goodsList;
}
