package com.hxzhou.mall.order.interceptor;

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
        // 放行远程调用的 /order/order/status  或者支付宝回调函数 /payed/notify
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match1 = antPathMatcher.match("/order/order/status/**", uri);
        boolean match2 = antPathMatcher.match("/payed/**", uri);
        if(match1 || match2) return true;

        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);

        // 如果没有登录，无法跳转到支付界面，应该跳转到登录界面进行登录
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
