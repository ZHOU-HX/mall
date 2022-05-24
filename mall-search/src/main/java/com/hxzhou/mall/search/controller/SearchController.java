package com.hxzhou.mall.search.controller;

import com.hxzhou.mall.search.service.MallSearchService;
import com.hxzhou.mall.search.vo.SearchParam;
import com.hxzhou.mall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 自动将页面提交过来的所有请求查询参数封装成指定的对象
     * @param searchParam
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest request) {
        searchParam.set_queryString(request.getQueryString());

        // 1 根据传递来的页面的查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(searchParam);

        // 2 将获得的结果存储在model中
        model.addAttribute("result", result);

        return "list";
    }
}
