package com.atguigu.gmall.search.service;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, buildDsl(searchParamVo));
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析搜索的响应结果集
            SearchResponseVo responseVo = parseSearchResult(response);
            //分页参数在搜索的请求参数里
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchResponseVo parseSearchResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        //1.解析hits命中对象
        SearchHits hits = response.getHits();
//设置总记录数
        responseVo.setTotal(hits.getTotalHits());
        SearchHit[] hitsHits = hits.getHits();
        if(hitsHits.length == 0 || hitsHits == null){
            return null;
        }
//设置GoodsList            //把hitsHits数组转换为GoodsList集合
        responseVo.setGoodsList(Arrays.stream(hitsHits).map(hitsHit -> {
            try {
                //获取结果集里的_source 里的数据返回json字符串
                String json = hitsHit.getSourceAsString();
                Goods goods = MAPPER.readValue(json, Goods.class);

                //获取高亮  title设置给goods对象
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField highlightTitle = highlightFields.get("title");
                Text[] texts = highlightTitle.getFragments();
                goods.setTitle(texts[0].toString());
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList()));

        //2.解析聚合结果集
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
            //2.1解析品牌聚合结果集
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandBuckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(brandBuckets)){
//设置品牌集合                //转换品牌聚合结果集为brand集合set到responseVo中
            responseVo.setBrands(brandBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
    //设置品牌id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());

                    //获取品牌名称子聚合
                Map<String, Aggregation> subAggregationMap = bucket.getAggregations().asMap();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) subAggregationMap.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
                        //获取桶中的第一个元素
                    String brandName = nameAggBuckets.get(0).getKeyAsString();
    //设置品牌name
                    brandEntity.setName(brandName);
                }

                    //获取品牌logo子聚合
                ParsedStringTerms logoAgg = (ParsedStringTerms) subAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if(!CollectionUtils.isEmpty(logoAggBuckets)){
                    String brandLogo = logoAggBuckets.get(0).getKeyAsString();
    //设置品牌logo
                    brandEntity.setLogo(brandLogo);
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }

            //2.2解析分类聚合结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryBuckets)){
//设置分类集合
            responseVo.setCategories(categoryBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
    //设置分类id
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                //获取子聚合 分类名称
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)bucket.getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
    //设置分类name
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

            //2.3解析规格参数聚合结果集
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
                //获取规格参数聚合的子聚合
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
                    //获取子聚合里的桶集合
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrIdAggBuckets)){
//设置规格参数集合                    // 转化桶集合为SearchResponseAttrVo集合
            responseVo.setFilters(attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
    //设置规格参数id
                responseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                    //获取attrIdAgg聚合中的所有子聚合
                Map<String, Aggregation> subaggregationMap = bucket.getAggregations().asMap();
                        //获取attrNameAgg聚合
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) subaggregationMap.get("attrNameAgg");
                        //获取attrNameAgg聚合中的桶集合
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
    //设置规格参数name                    //获取第一个桶。并获取其中的key
                    responseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }
                        //获取value子聚合
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) subaggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> buckets = attrValueAgg.getBuckets();
                            //获取value子聚合的桶集合（获取每个桶的key）转换为新集合
                List<String> attrValue = buckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
    //设置规格参数value 是个集合
                responseAttrVo.setAttrValues(attrValue);
                return responseAttrVo;
            }).collect(Collectors.toList()));

        }

        return responseVo;
    }

    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //1.构建查询及过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
            //1.1构建匹配查询
        String keyword = searchParamVo.getKeyword();
        if(StringUtils.isBlank(keyword)){//关键字为空
            //TODO:广告
            return sourceBuilder;
        }
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
            //1.2构建过滤条件
                //1.2.1品牌过滤
        List<Long> brandId = searchParamVo.getBrandId();
        if(!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
                //1.2.2分类过滤
        List<Long> categoryId = searchParamVo.getCategoryId();
        if(!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryId));
        }
                //1.2.3价格区间过滤
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        if(priceFrom != null && priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if(priceFrom != null){
                rangeQuery.gte(priceFrom);//大于
            }
            if(priceTo != null){
                rangeQuery.lte(priceTo);//小于
            }
        }
                //1.2.4是否有货过滤
        Boolean store = searchParamVo.getStore();
        if(store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }
                //1.2.5规格参数的嵌套过滤
        List<String> props = searchParamVo.getProps();
        if(!CollectionUtils.isEmpty(props)){
                    // 参数格式 4:8G-12G
            props.forEach( prop -> {
                String[] attrs = StringUtils.split(prop, ":");
                if(attrs != null && attrs.length == 2){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    String attrValue = attrs[1];
                    String[] attrValues = StringUtils.split(attrValue, "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                    //ScoreMode.None不需要得分模式
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                }
            });
        }
        //2.构建排序条件  1.价格升序 2.价格降序 3.销量降序 4.新品降序  默认: 0 得分排序
        Integer sort = searchParamVo.getSort();
        if(sort != null){
            switch (sort){
                case 1: sourceBuilder.sort("price", SortOrder.ASC); break;
                case 2: sourceBuilder.sort("price", SortOrder.DESC); break;
                case 3: sourceBuilder.sort("sales", SortOrder.DESC); break;
                case 4: sourceBuilder.sort("createTime", SortOrder.DESC); break;
                default:
                    sourceBuilder.sort("_score", SortOrder.DESC);
                    break;
            }
        }

        //3.构建分页条件
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from( (pageNum - 1) * pageSize );
        sourceBuilder.size(pageSize);

        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder()
                .field("title")
                .preTags("<font style='color:red;'>")
                .postTags("</font>"));

        //5.构建聚合查询
            //5.1 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

            //5.2 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

            //5.3 规格参数聚合err
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

            //6.添加结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId","title","subTitle","defaultImage","price"}, null);
        return sourceBuilder;
    }
}
