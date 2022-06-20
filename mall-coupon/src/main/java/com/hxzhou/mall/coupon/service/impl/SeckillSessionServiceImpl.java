package com.hxzhou.mall.coupon.service.impl;

import com.hxzhou.mall.coupon.entity.SeckillSkuRelationEntity;
import com.hxzhou.mall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.coupon.dao.SeckillSessionDao;
import com.hxzhou.mall.coupon.entity.SeckillSessionEntity;
import com.hxzhou.mall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询最近三天需要秒杀的商品信息
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getLatest3DaySession() {
        /**
         * 1 计算最近三天的秒杀
         */
        LocalDate now = LocalDate.now();

        // 最小时间
        LocalTime min = LocalTime.MIN;
        LocalDateTime startOf = LocalDateTime.of(now, min);
        String start = startOf.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 最大时间
        LocalTime max = LocalTime.MAX;
        LocalDateTime endOf = LocalDateTime.of(now.plusDays(2), max);
        String end = endOf.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 查询秒杀开始的时间在最小时间和最大时间之间
        List<SeckillSessionEntity> list = this.list(
                new QueryWrapper<SeckillSessionEntity>().between("start_time", start, end));

        /**
         * 2 根据秒杀id查询关联的商品
         */
        if(list != null && list.size() > 0) {
            List<SeckillSessionEntity> collect = list.stream().map(session -> {
                Long id = session.getId();
                List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(
                        new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id));
                session.setRelationSkus(relationEntities);
                return session;
            }).collect(Collectors.toList());

            return collect;
        }

        return null;
    }

}