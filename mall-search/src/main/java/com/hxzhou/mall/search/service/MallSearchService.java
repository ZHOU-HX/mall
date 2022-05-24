package com.hxzhou.mall.search.service;

import com.hxzhou.mall.search.vo.SearchParam;
import com.hxzhou.mall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param searchParam 检索的所有参数
     * @return 返回检索结果，里面包含页面需要的所有信息
     */
    SearchResult search(SearchParam searchParam);
}
