package com.hxzhou.mall.member.dao;

import com.hxzhou.mall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:36:50
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
