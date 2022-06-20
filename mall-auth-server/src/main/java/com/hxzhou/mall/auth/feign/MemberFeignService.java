package com.hxzhou.mall.auth.feign;

import com.hxzhou.common.utils.R;
import com.hxzhou.mall.auth.vo.SocialUser;
import com.hxzhou.mall.auth.vo.UserLoginVo;
import com.hxzhou.mall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("mall-member")
public interface MemberFeignService {

    /**
     * 注册功能
     */
    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo userRegistVo);

    /**
     * 登录功能
     */
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo userLoginVo);


    /**
     * 第三方社交账户登陆时，会员自动注册并登录功能
     */
    @PostMapping("/member/member/oauth2/login")
    R oauth2login(@RequestBody SocialUser socialUser) throws Exception;
}
