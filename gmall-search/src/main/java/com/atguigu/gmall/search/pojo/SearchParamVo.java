package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    //搜索条件数据模型

    //搜索关键字
    private String keyword;
    //品牌
    private List<Long> brandId;
    //分类
    private List<Long> categoryId;
    //规格参数的过滤: props=4:8G-12G&props=5:128G-256G
    private List<String> props;
    //排序字段: 1.价格升序 2.价格降序 3.销量降序 4.新品降序  默认: 0 得分排序
    private Integer sort = 0;
    //价格区间过滤
    private Double priceTo;
    private Double priceFrom;
    //是否有货
    private Boolean store;
    //页码，默认第一页
    private Integer pageNum = 1;
    //每页记录数
    private final Integer pageSize = 20;
}
