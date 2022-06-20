package com.hxzhou.mall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class MallFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                // 1 RequestContextHolder拿到刚进来的这个请求
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if(attributes != null) {
                    HttpServletRequest request = attributes.getRequest();       // 老请求内的数据

                    if (request != null) {
                        // 2 同步请求头数据，Cookie
                        String cookie = request.getHeader("Cookie");

                        // 3 给新请求同步了老请求的cookie
                        requestTemplate.header("Cookie", cookie);           // 新请求内的数据
                    }
                }
            }
        };
    }
}
