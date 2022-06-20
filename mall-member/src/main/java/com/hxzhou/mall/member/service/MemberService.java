package com.hxzhou.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.member.entity.MemberEntity;
import com.hxzhou.mall.member.exception.EmailExistException;
import com.hxzhou.mall.member.exception.PhoneExistException;
import com.hxzhou.mall.member.exception.UsernameExistException;
import com.hxzhou.mall.member.vo.MemberLoginVo;
import com.hxzhou.mall.member.vo.MemberRegistVo;
import com.hxzhou.mall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:36:50
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo memberRegistVo);

    void checkEmailUnique(String email) throws EmailExistException;

    void checkUsernameUnique(String username) throws UsernameExistException;

    void checkPhoneUnique(String phone) throws PhoneExistException;

    MemberEntity login(MemberLoginVo memberLoginVo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

