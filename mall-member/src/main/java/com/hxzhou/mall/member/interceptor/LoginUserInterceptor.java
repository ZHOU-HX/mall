package com.hxzhou.mall.member.interceptor;

import com.hxzhou.common.constant.AuthServerConstant;
import com.hxzhou.common.to.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 所有远程服务调用的根据id查询用户收货地址/member/memberreceiveaddress/info/{id}无需登录直接放行
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/member/**", uri);
        if(match) return true;

        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);

        // 如果没有登录，无法跳转到订单界面，应该跳转到登录界面进行登录
        if(attribute == null) {
            request.getSession().setAttribute("msg", "请先进行登录！");
            response.sendRedirect("http://auth.mall.com/login.html");
            return false;
        }

        // 登录了就取出信息
        loginUser.set(attribute);
        return true;
    }
}
