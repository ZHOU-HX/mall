package com.hxzhou.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.member.entity.MemberLevelEntity;

import java.util.Map;

/**
 * 会员等级
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:36:51
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

