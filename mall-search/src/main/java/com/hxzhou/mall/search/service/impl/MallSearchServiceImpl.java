package com.hxzhou.mall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.to.es.SkuEsModel;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.search.config.MallElasticSearchConfig;
import com.hxzhou.mall.search.constant.EsContant;
import com.hxzhou.mall.search.feign.ProductFeignService;
import com.hxzhou.mall.search.service.MallSearchService;
import com.hxzhou.mall.search.vo.AttrResponseVo;
import com.hxzhou.mall.search.vo.BrandVo;
import com.hxzhou.mall.search.vo.SearchParam;
import com.hxzhou.mall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    /**
     * @param searchParam 检索的所有参数
     * @return
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        // 动态构建出查询需要的DSL语句
        SearchResult result = null;

        // 1 准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        SearchResponse response = null;
        try {
            // 2 执行检索请求
            response = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);

            // 3 分析响应数据封装成我们需要的格式
            result = buildSearchResult(searchParam, response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 准备检索请求
     * @param param
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();          // 用来构建DSL语句

        /**
         * 一、查询：模糊匹配、过滤（按照属性、分类、品牌、价格区间、库存）
         */
        // 1 构建bool - query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2 添加检索条件
        // 2.1 must - 模糊匹配
        if(!StringUtils.isEmpty(param.getKeyword())) boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        // 2.2 filter - 按照三级分类id查询
        if(param.getCatalog3Id() != null) boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        // 2.3 filter - 按照品牌id查询
        if(param.getBrandIds() != null && param.getBrandIds().size() > 0) boolQuery.filter(QueryBuilders.termQuery("brandId", param.getBrandIds()));
        // 2.4 filter - 按照所有指定的属性进行查询
        if(param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                // attr=1_5寸:8寸&attr=2_16G:8G
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();

                // attr=1_5寸:8寸
                String[] s = attrStr.split("_");
                String attrId = s[0];       // 检索的属性id
                String[] attrValues = s[1].split(":");      // 这个属性的检索用的值

                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));

                // 每一个都必须要生成一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        // 2.5 filter - 按照是否有库存进行查询
        if(param.getHasStock() != null) boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        // 2.6 filter - 按照价格区间查询
        if(!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");

            String[] s = param.getSkuPrice().split("_");
            if(s.length == 1) rangeQuery.gte(s[0]);
            else {
                if(!s[0].equals("") && !s[1].equals("")) rangeQuery.gte(s[0]).lte(s[1]);
                else if(!s[0].equals("")) rangeQuery.gte(s[0]);
                else if(!s[1].equals("")) rangeQuery.lte(s[1]);
            }

            boolQuery.filter(rangeQuery);
        }
        // 3 把以前所有条件都拿来进行封装
        sourceBuilder.query(boolQuery);

        /**
         * 二、排序、分页、高亮
         */
        // 1 排序
        if(!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();

            // sort=hotScore_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        // 2 分页
        sourceBuilder.from((param.getPageNum() - 1) * EsContant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsContant.PRODUCT_PAGESIZE);

        // 3 高亮
        if(!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /**
         * 三、聚合分析
         */
        // 1 品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 1.1 品牌聚合下的名称聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        // 1.2 品牌聚合下的图片聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brandAgg);

        // 2 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogAgg);

        // 3 属性聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        // 3.1 子聚合：聚合出当前所有的attrid
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 3.1.1 子聚合：聚合分析出当前attrid对应的名字
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 3.1.2 子聚合：聚合分析出当前attrid对应的所有可能的属性值attrValue
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attrAgg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attrAgg);

        String s = sourceBuilder.toString();
        System.out.println("构建的DSL语句：" + s);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsContant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 构建结果数据
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchParam param, SearchResponse response) {
        SearchHits hits = response.getHits();
        SearchResult result = new SearchResult();

        /**
         * 1 返回所有查询到的商品
         */
        List<SkuEsModel> esModels = new ArrayList<>();
        if(hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                // 高亮值
                if(!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String s = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(s);
                }

                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        /**
         * 2 返回所有查询到的商品所涉及的相关信息
         */
        // 2.1 当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attrAgg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");

        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();

            // 得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);

            // 得到属性的名字
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            // 得到属性的所有值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = attrValueAgg.getBuckets().stream().map((item) -> {
                String keyAsString = ((Terms.Bucket) item).getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        // 2.2 当前所有商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = response.getAggregations().get("brand_agg");

        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();

            // 得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);

            // 得到品牌的名字
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            // 得到品牌的图片
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        // 2.3 当前所有商品涉及到的所有分类信息
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = response.getAggregations().get("catalog_agg");

        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();

            // 得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            // 得到分类名
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        /**
         * 3 返回所有查询到的分页信息
         */
        // 3.1 当前页码
        result.setPageNum(param.getPageNum());

        // 3.2 总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        // 3.3 总页码
        int totalPages = (int) total / EsContant.PRODUCT_PAGESIZE + (total % EsContant.PRODUCT_PAGESIZE == 0 ? 0 : 1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for(int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        /**
         * 4 构建面包屑导航功能
         */
        if(param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map((attr) -> {
                // 分析每一个attrs传过来的查询参数值
                SearchResult.NavVo navVo = new SearchResult.NavVo();

                String[] s = attr.split("_");
                result.getAttrIds().add(Long.parseLong(s[0]));
                navVo.setNavValue(s[1]);

                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                if(r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {});
                    navVo.setNavName(data.getAttrName());
                }
                else {
                    navVo.setNavName(s[0]);
                }

                // 取消面包屑以后，要跳转到哪个地方(即，将请求的地址url里面的当前属性置空)
                // 拿到所有查询条件，去掉当前
                String replace = replaceQueryString(param, attr, "attrs");
                if(replace.equals("")) navVo.setLink("http://search.mall.com/list.html");
                else navVo.setLink("http://search.mall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }

        // 品牌和分类也上面包屑导航，用来被点击后，只有在面包屑上显示，下面就不展示了
        if(param.getBrandIds() != null && param.getBrandIds().size() > 0) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();

            navVo.setNavName("品牌");
            // 远程调用查询所有品牌
            R r = productFeignService.brandsInfo(param.getBrandIds());
            if(r.getCode() == 0) {
                List<BrandVo> brands = r.getData("brands", new TypeReference<List<BrandVo>>() {});

                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brand : brands) {
                    buffer.append(brand.getBrandName() + ";");
                    replace = replaceQueryString(param, brand.getBrandId() + "", "brandId");
                }

                navVo.setNavValue(buffer.toString());
                if(replace.equals("")) navVo.setLink("http://search.mall.com/list.html");
                else navVo.setLink("http://search.mall.com/list.html?" + replace);
            }

            navs.add(navVo);
        }


        return result;
    }

    private String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode.replace("+", "%20"); // 浏览器对空格的编码和Java不一样
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        replace = replace.replace(key + "=" + encode, "");
        return replace;
    }
}
