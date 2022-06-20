package com.hxzhou.mall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.utils.HttpUtils;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.auth.feign.MemberFeignService;
import com.hxzhou.common.to.MemberRespVo;
import com.hxzhou.mall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        /**
         * 1 封装code获取第三方账户accessToken的所需信息
         */
        Map<String, String> header = new HashMap<>();
        Map<String, String> query = new HashMap<>();

        Map<String, String> map = new HashMap<>();
        map.put("client_id", "43083698");
        map.put("client_secret", "f9847a6b4483f57337082f544a661f38");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.mall.com/oauth2/weibo/success");
        map.put("code", code);

        // 1.1 根据code换取accessToken
        HttpResponse response = HttpUtils.doPost(
                "https://api.weibo.com",
                "/oauth2/access_token",
                "post", header, query, map);

        // 1.2 如果没有获取到accessToken，就报错，重新返回登录界面
        if(response.getStatusLine().getStatusCode() != 200) {
            return "redirect:http://auth.mall.com/login.html";
        }


        /**
         * 2 将获取到的accessToken进行对用户登录的处理（登录、注册【若第一次登录该网站才执行此操作】、并获取自己网站该账户对应的会员信息）
         */
        // 2.1 获取到accessToken就拿着它向微博发送请求，获取用户信息
        String json = EntityUtils.toString(response.getEntity());
        SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

        // 2.2 如果是用户第一次登录该网站，自动注册（为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定的会员）
        R oauth2login = memberFeignService.oauth2login(socialUser);

        // 2.3 登录出错，重新返回登录界面
        if(oauth2login.getCode() != 0) {
            return "redirect:http://auth.mall.com/login.html";
        }

        /**
         * 3 登录成功，存储用户信息并跳转到首页
         */
        // 3.1 存储用户信息
        MemberRespVo data = oauth2login.getData("data", new TypeReference<MemberRespVo>() {});
        // TODO 1 默认发的令牌。session=xxx。作用域是当前域；（解决子域session共享问题）
        // TODO 2 默认存储到redis使用的是jdk的序列化机制，每个bean都要实现Serializable接口，太麻烦；（使用json序列化的方式存储对象数据到redis中）
        session.setAttribute("loginUser", data);
        log.info("登录成功，用户信息：{}", data.toString());

        // 3.2 跳转到首页
        return "redirect:http://mall.com";
    }

    /**
     * 取消授权
     */
    @GetMapping("/fail")
    public String unWeibo() {
        return "redirect:http://auth.mall.com/login.html";
    }
}
