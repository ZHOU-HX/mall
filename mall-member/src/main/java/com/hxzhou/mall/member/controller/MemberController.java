package com.hxzhou.mall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.hxzhou.common.exception.BizCodeEnum;
import com.hxzhou.mall.member.exception.EmailExistException;
import com.hxzhou.mall.member.exception.PhoneExistException;
import com.hxzhou.mall.member.exception.UsernameExistException;
import com.hxzhou.mall.member.feign.CouponFeignService;
import com.hxzhou.mall.member.vo.MemberLoginVo;
import com.hxzhou.mall.member.vo.MemberRegistVo;
import com.hxzhou.mall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import com.hxzhou.mall.member.entity.MemberEntity;
import com.hxzhou.mall.member.service.MemberService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.R;



/**
 * 会员
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:36:50
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;

    /**
     * 测试feign功能：远程调用优惠券模块中的功能，返回结果
     * @return
     */
    @RequestMapping("/coupons")
    public R test() {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");

        R memberCoupons = couponFeignService.memberCoupons();
        return R.ok().put("member", memberEntity).put("coupons", memberCoupons.get("coupons"));
    }

    /**
     * 会员注册功能
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo memberRegistVo) {

        try {
            memberService.regist(memberRegistVo);
        } catch (PhoneExistException e) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        } catch (EmailExistException e) {
            return R.error(BizCodeEnum.EMAIL_EXIST_EXCEPTION.getCode(), BizCodeEnum.EMAIL_EXIST_EXCEPTION.getMsg());
        }

        return R.ok();
    }

    /**
     * 会员登录功能
     */
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVo) {
        MemberEntity entity = memberService.login(memberLoginVo);

        // 登陆失败
        if(entity == null) return R.error(BizCodeEnum.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getCode(),
                BizCodeEnum.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getMsg());

        return R.ok().setData(entity);
    }

    /**
     * 第三方社交账户登陆时，会员自动注册并登录功能
     */
    @PostMapping("oauth2/login")
    public R oauth2login(@RequestBody SocialUser socialUser) throws Exception {
        MemberEntity entity = memberService.login(socialUser);

        // 登陆失败
        if(entity == null) return R.error(BizCodeEnum.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getCode(),
                BizCodeEnum.LOGINACCOUNT_PASSWORD_INVAILD_EXCEPTION.getMsg());

        return R.ok().setData(entity);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
