package com.hxzhou.mall.search;

import com.alibaba.fastjson.JSON;
import com.hxzhou.mall.search.config.MallElasticSearchConfig;
import com.hxzhou.mall.search.pojo.BankAccount;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Map;

//@RunWith(SpringRunner.class)
@SpringBootTest
public class MallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

    /**
     * 测试存储数据到es
     */
    @Test
    public void indexData() throws IOException {
        // 1 创建数据
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");

//        // 第一种注入数据的方式
//        indexRequest.source("username", "zhangsan", "age", 18, "gender", "男");

        // 2 注入数据：第二种注入数据的方式
        User user = new User();
        user.setUsername("西翎晴");
        user.setAge(18);
        user.setGender("女");
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);

        // 3 提交数据：执行插入操作
        IndexResponse index = client.index(indexRequest, MallElasticSearchConfig.COMMON_OPTIONS);

        // 4 提取有用的数据
        System.out.println(index);
    }

    @Data
    class User{
        private String username;
        private String gender;
        private Integer age;
    }

    /**
     * 按照年龄聚合，并且请求这些年龄段的这些人的平均薪资
     */
    @Test
    public void searchData() throws IOException {
        /**
         *  1 创建检索请求
         */
        SearchRequest searchRequest = new SearchRequest();

        /**
         *  2 指定检索条件
         */
        searchRequest.indices("bank");      // 指定索引
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();      // 指定DSL，检索条件
        // 2.1 构造检索条件
        sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        // 2.2 按照年龄的值分布进行聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);
        // 2.3 计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);

        System.out.println("检索条件：" + sourceBuilder.toString());
        searchRequest.source(sourceBuilder);

        /**
         *  3 执行检索
         */
        SearchResponse searchResponse = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);

        /**
         *  4 分析结果
         */
        System.out.println(searchResponse);
        // 4.1 获取所有查到的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for(SearchHit hit : searchHits) {
            String string = hit.getSourceAsString();
            BankAccount bankAccount = JSON.parseObject(string, BankAccount.class);
            System.out.println("bankAccount: " + bankAccount);
        }
        // 4.2 获取检索到的分析信息
        Aggregations aggregations = searchResponse.getAggregations();
        Terms ageAgg1 = aggregations.get("ageAgg");
        for(Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄：" + keyAsString);
        }

        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均薪资：" + balanceAvg1.getValue());
    }

}
