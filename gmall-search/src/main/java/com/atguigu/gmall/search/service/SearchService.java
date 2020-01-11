package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Case;
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
import org.elasticsearch.search.aggregations.Aggregations;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient highLevelClient;

    public SearchResponseVO search(SearchParam searchParam) throws IOException {

        SearchResponse searchResponse = this.highLevelClient.search(new SearchRequest(new String[]{"goods"}, buildDSL(searchParam)), RequestOptions.DEFAULT);

        SearchResponseVO responseVO = this.parseSearchResult(searchResponse);

        responseVO.setPageNum(searchParam.getPageNum());
        responseVO.setPageSize(searchParam.getPageSize());
        return responseVO;
    }

    private SearchResponseVO parseSearchResult(SearchResponse searchResponse){
        SearchResponseVO responseVO = new SearchResponseVO();

        // 查询结果集的封装
        SearchHits hits = searchResponse.getHits();
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit hitsHit : hitsHits) {
            // 获取_source反序列化为goods
            String goodsJson = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(goodsJson, Goods.class);

            // 获取高亮结果集，覆盖普通的skuTitle
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("skuTitle");
            goods.setSkuTitle(highlightField.getFragments()[0].string());

            goodsList.add(goods);
        }
        responseVO.setProducts(goodsList);

        // 解析品牌的聚合结果集
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        SearchResponseAttrVO brandVO = new SearchResponseAttrVO();
        brandVO.setProductAttributeId(null);
        brandVO.setName("品牌");
        // 获取聚合中的桶
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)){

            // 把每个桶转化成json字符串：{id:100,name:华为}
            List<String> brandValues = buckets.stream().map(bucket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ((Terms.Bucket) bucket).getKeyAsNumber());
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().asMap().get("brandNameAgg");
                map.put("name",  brandNameAgg.getBuckets().get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());

            brandVO.setValue(brandValues);
            responseVO.setBrand(brandVO);
        }

        // 解析分类的聚合结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        SearchResponseAttrVO categoryVO = new SearchResponseAttrVO();
        categoryVO.setProductAttributeId(null);
        categoryVO.setName("分类");
        // 获取聚合中的桶
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryBuckets)){

            // 把每个桶转化成json字符串：{id:100,name:华为}
            List<String> categoryValues = categoryBuckets.stream().map(bucket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ((Terms.Bucket) bucket).getKeyAsNumber());
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().asMap().get("categoryNameAgg");
                map.put("name",  categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());

            categoryVO.setValue(categoryValues);
            responseVO.setCatelog(categoryVO);
        }

        // 解析规格参数的聚合结果集
        ParsedNested attrsAgg = (ParsedNested)aggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> idBuckets = attrIdAgg.getBuckets();
        List<SearchResponseAttrVO> attrVOS = idBuckets.stream().map(bucket -> {
            SearchResponseAttrVO attrVO = new SearchResponseAttrVO();
            attrVO.setProductAttributeId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
            // 获取规格参数名子聚合，解析出规格参数名
            ParsedStringTerms attrNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            attrVO.setName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            // 获取规格参数值子聚合，解析出规格参数值集合
            ParsedStringTerms attrValueAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
            List<String> values = valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVO.setValue(values);
            return attrVO;
        }).collect(Collectors.toList());
        responseVO.setAttrs(attrVOS);

        // 总记录数
        responseVO.setTotal(hits.getTotalHits());

        return responseVO;
    }

    private SearchSourceBuilder buildDSL(SearchParam searchParam){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.fetchSource(new String[]{"skuId", "skuTitle", "skuSubTitle", "price", "defaultImage"}, null);

        String key = searchParam.getKey();
        if (StringUtils.isEmpty(key)) {
            // 显示默认的商品列表
            return sourceBuilder;
        }

        // 1. 构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1. 构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", key).operator(Operator.AND));

        // 1.2. 构建过滤条件
        // 1.2.1. 品牌的过滤
        Long[] brandIds = searchParam.getBrand();
        if (brandIds != null && brandIds.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandIds));
        }

        // 1.2.2. 分类的过滤
        Long[] catelog3 = searchParam.getCatelog3();
        if (catelog3 != null && catelog3.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", catelog3));
        }

        // 1.2.3. 价格区间的过滤
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Double priceFrom = searchParam.getPriceFrom();
        if (priceFrom != null){
            rangeQueryBuilder.gte(priceFrom);
        }
        Double priceTo = searchParam.getPriceTo();
        if (priceTo != null) {
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);

        // 1.2.4. 规格属性的过滤
        List<String> props = searchParam.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2) {
                    String attrId = attr[0];
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                    boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", boolQuery, ScoreMode.None));
                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);

        // 2. 构建排序
        String order = searchParam.getOrder();
        if (StringUtils.isNotBlank(order)){
            String[] orders = StringUtils.split(":");
            if (orders != null && orders.length == 2) {
                String orderField = orders[0];
                String orderBy = orders[1];
                switch (orderField) {
                    case "0": orderField = "_score"; break;
                    case "1": orderField = "sale"; break;
                    case "2": orderField = "price"; break;
                    default: orderField = "_score"; break;
                }
                sourceBuilder.sort(orderField, StringUtils.equals(orderBy, "asc") ? SortOrder.ASC : SortOrder.DESC);
            }
        }

        // 3. 构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        // 4. 构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("skuTitle").preTags("<span style='color:red;'>").postTags("</span>"));

        // 5. 构建聚合
        // 5.1. 品牌的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("brandIdAgg").field("brandId").subAggregation(
                        AggregationBuilders.terms("brandNameAgg").field("brandName")
                )
        );

        // 5.2. 分类的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(
                        AggregationBuilders.terms("categoryNameAgg").field("categoryName")
                )
        );

        // 5.3. 规格属性的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.nested("attrsAgg", "attrs").subAggregation(
                        AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").subAggregation(
                                AggregationBuilders.terms("attrNameAgg").field("attrs.attrName")
                        ).subAggregation(
                                AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")
                        )
                )
        );

//        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }
}
