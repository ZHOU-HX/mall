package com.hxzhou.mall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@Controller
public class HelloController {

    @Value("${sso.server.url}")
    String ssoServerUrl;

    /**
     * 无需登录即可访问
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    /**
     * token用来识别此次访问是登录前还是登录后的操作
     * @param model
     * @param session
     * @param token
     * @return
     */
    @GetMapping("/employees")
    public String employees(Model model, HttpSession session,
                            @RequestParam(value = "token", required = false) String token) {
        // 没登陆，跳转到登录服务器登录
        if(!StringUtils.hasLength(token)) {
            // 跳转过去，使用url上的查询参数标识我们自己是哪个页面
            return "redirect:" + ssoServerUrl + "?redirect_url=http://client1.com:8081/employees";
        }

        // 登陆了才可以进一步操作
        // TODO 去ssoserver中获取当前token真正对应的用户信息
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> forEntity = restTemplate.getForEntity(
                "http://ssoserver.com:8080/userInfo?token=" + token, String.class);
        String body = forEntity.getBody();
        session.setAttribute("loginUser", body);

        ArrayList<String> emps = new ArrayList<>();
        emps.add("张三");
        emps.add("李四");
        model.addAttribute("emps", emps);

        return "list";
    }
}
