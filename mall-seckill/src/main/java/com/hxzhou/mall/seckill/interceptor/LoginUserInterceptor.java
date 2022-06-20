package com.hxzhou.mall.seckill.interceptor;

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
        // 判断 /kill 秒杀请求的用户是否登陆了
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/kill", uri);

        // 如果是 /kill 请求就要判断登录状态，只有登录了才放行
        if(match) {
            MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);

            // 如果没有登录，无法跳转到秒杀界面，应该跳转到登录界面进行登录
            if (attribute == null) {
                request.getSession().setAttribute("msg", "请先进行登录！");
                response.sendRedirect("http://auth.mall.com/login.html");
                return false;
            }

            // 登录了就取出信息
            loginUser.set(attribute);
            return true;
        }

        // 其他服务直接放行
        return true;
    }
}
