package com.hxzhou.mall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hxzhou.common.utils.HttpUtils;
import com.hxzhou.mall.member.dao.MemberLevelDao;
import com.hxzhou.mall.member.entity.MemberLevelEntity;
import com.hxzhou.mall.member.exception.EmailExistException;
import com.hxzhou.mall.member.exception.PhoneExistException;
import com.hxzhou.mall.member.exception.UsernameExistException;
import com.hxzhou.mall.member.vo.MemberLoginVo;
import com.hxzhou.mall.member.vo.MemberRegistVo;
import com.hxzhou.mall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.member.dao.MemberDao;
import com.hxzhou.mall.member.entity.MemberEntity;
import com.hxzhou.mall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册功能
     * @param memberRegistVo
     */
    @Override
    public void regist(MemberRegistVo memberRegistVo) {
        MemberEntity entity = new MemberEntity();

        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        // 检查用户名和手机号是否唯一。为了让controller能感知异常，可以使用异常机制
        checkPhoneUnique(memberRegistVo.getPhone());
        checkUsernameUnique(memberRegistVo.getUsername());

        entity.setMobile(memberRegistVo.getPhone());
        entity.setUsername(memberRegistVo.getUsername());

        // 密码需要进行加密操作
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(memberRegistVo.getPassword());
        entity.setPassword(encode);

        // 其他的默认信息
        entity.setNickname(memberRegistVo.getUsername());

        // 保存
        this.baseMapper.insert(entity);
    }

    /**
     * 检查邮箱是否唯一
     * @param email
     * @return
     */
    @Override
    public void checkEmailUnique(String email) throws EmailExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("email", email));
        if(count > 0) {
            throw new EmailExistException();
        }
    }

    /**
     * 检查用户名是否唯一
     * @param username
     * @return
     */
    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(count > 0) {
            throw new UsernameExistException();
        }
    }

    /**
     * 检查手机号是否唯一
     * @param phone
     * @return
     */
    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count > 0) {
            throw new PhoneExistException();
        }
    }

    /**
     * 用户登录功能
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo memberLoginVo) {
        String loginAccount = memberLoginVo.getLoginAccount();
        String password = memberLoginVo.getPassword();

        // 1 去数据库查询（手机号、用户名、邮箱皆可）
        MemberEntity entity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginAccount)
                .or().eq("mobile", loginAccount).or().eq("email", loginAccount));

        // 2 判断登录成功与否
        if(entity != null) {
            // 获取数据库的密码与用户提交的密码进行比对
            String passwordDB = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, passwordDB);

            // 登录成功
            if(matches) return entity;
        }

        // entity为空，或者密码匹配失败。最终登录失败
        return null;
    }

    /**
     * 第三方社交账户登录（登录并注册（如果没有登录的情况））
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        String uid = socialUser.getUid();

        // 判断当前社交用户是否已经登陆过该系统
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));

        if(memberEntity != null) {
            // 如果以前登陆过，不需要注册，只需要更新令牌即可
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            this.baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }
        else {
            // 如果是第一次登录该系统，需要注册
            MemberEntity regist = new MemberEntity();

            try {
                // 去微博查询当前社交用户的信息（用户名、性别等）
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUser.getAccess_token());
                query.put("uid", socialUser.getUid());
                HttpResponse response = HttpUtils.doGet(
                        "https://api.weibo.com",
                        "/2/users/show.json",
                        "get", new HashMap<String, String>(), query);

                // 查询成功
                if(response.getStatusLine().getStatusCode() == 200) {
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);

                    // 昵称等信息
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");

                    // 存储注册信息
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);
                }
            } catch (Exception e) {}

            // 存储第三方信息
            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());
            this.baseMapper.insert(regist);

            return regist;
        }
    }

}