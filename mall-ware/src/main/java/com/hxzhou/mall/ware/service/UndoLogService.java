package com.hxzhou.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.mall.ware.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author hxzhou
 * @email hxzhou1998@163.com
 * @date 2022-03-25 20:55:44
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

