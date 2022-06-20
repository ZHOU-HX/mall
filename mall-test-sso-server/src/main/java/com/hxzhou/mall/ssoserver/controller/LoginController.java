package com.hxzhou.mall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 通过token获取登陆用户的所有信息
     * @param token
     * @return
     */
    @ResponseBody
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("token") String token) {
        return redisTemplate.opsForValue().get(token);
    }

    /**
     * 用来判断请求转发：如果以前登录过，直接跳回原地址；否则跳转
     * @param redirect_url
     * @param model
     * @param sso_token
     * @return
     */
    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String redirect_url, Model model,
                            @CookieValue(value = "sso_token", required = false) String sso_token) {
        // 将源地址放入请求域中
        model.addAttribute("redirect_url", redirect_url);

        // 如果以前有人登陆过了，cookie一定会有值的，所以直接跳回源地址，无需再次登录
        if(sso_token != null) {
            return "redirect:" + redirect_url + "?token=" + sso_token;
        }

        return "login";
    }

    /**
     * 用来登录，登陆成功后，存储登录用户信息以及用户唯一标识符，并将用户唯一标识符返回给原地址；登陆失败仍然在此界面
     * @param username
     * @param password
     * @param redirect_url
     * @param response
     * @return
     */
    @PostMapping("/doLogin")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam(value = "redirect_url", required = false) String redirect_url,
                          HttpServletResponse response) {

        // 登录失败，展示登录页
        if(!StringUtils.hasLength(username) || !StringUtils.hasLength(password)) {
            return "login";
        }

        // 把登录成功的用户存储起来
        String uuid = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(uuid, username);

        // 用cookie存储用户uuid，以便下次别的网站请求登录时，记录已经登陆过就直接免登录了
        Cookie sso_token = new Cookie("sso_token", uuid);
        response.addCookie(sso_token);

        // 登录成功，跳回之前的页面
        return "redirect:" + redirect_url + "?token=" + uuid;
    }
}
