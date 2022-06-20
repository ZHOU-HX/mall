package com.hxzhou.mall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hxzhou.common.to.MemberRespVo;
import com.hxzhou.common.to.mq.SeckillOrderTo;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.seckill.feign.CouponFeignService;
import com.hxzhou.mall.seckill.feign.ProductFeignService;
import com.hxzhou.mall.seckill.interceptor.LoginUserInterceptor;
import com.hxzhou.mall.seckill.service.SeckillService;
import com.hxzhou.mall.seckill.to.SeckillSkuRedisTo;
import com.hxzhou.mall.seckill.vo.SeckillSessionsWithSkus;
import com.hxzhou.mall.seckill.vo.SeckillSkuVo;
import com.hxzhou.mall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.RabbitAccessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private static final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private static final String SKUKILL_CACHE_PREFIX = "seckill:skus:";

    private static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    /**
     * 上架最近三天需要秒杀的商品
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        /**
         * 1 扫描最近三天需要参与秒杀的活动
         */
        R session = couponFeignService.getLatest3DaySession();

        if(session.getCode() != 0) return ;             // 失败了

        /**
         * 2 否则，继续上架商品，将其缓存到redis
         */
        List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {});
        // 2.1 缓存活动信息
        saveSessionInfos(sessionData);
        // 2.2 缓存活动的关联商品信息
        saveSessionSkuInfos(sessionData);

    }

    /**
     * 缓存活动信息
     * @param sessions
     */
    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            // 缓存活动信息，同时如果有key，就不要做了
            Boolean hasKey = redisTemplate.hasKey(key);
            if(!hasKey) {
                List<String> collect = session.getRelationSkus().stream()
                    .map(item -> item.getPromotionSessionId().toString() + "_" +item.getSkuId().toString())
                    .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    /**
     * 缓存活动的关联商品信息
     * @param sessions
     */
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            // 准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

            // 缓存商品信息
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                String id = seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString();
                // 如果缓存中没有，才进行添加
                if(!ops.hasKey(id)) {
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();

                    // 1 sku的基本数据
                    R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfo);
                    }

                    // 2 sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);

                    // 3 设置上当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    // 4 商品的随机码【为了防止脚本的疯狂请求秒杀】
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);

                    // 5 上架商品信息
                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(id, jsonString);

                    // 6 上架库存信息，同时为了保持分布式下高并发的处理，将每个商品的秒杀数量当前信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
                }
            });
        });
    }

    /**
     * 从redis中获取秒杀的商品信息
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 1 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSION_CACHE_PREFIX, "");
            String[] s = replace.split("_");

            long start = Long.parseLong(s[0]);
            long end = Long.parseLong(s[1]);

            if(time >= start && time <= end) {
                // 2 获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);

                if(list != null) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redis = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
//                        redis.setRandomCode(null);      // 如果秒杀开始的话，需要随机码，否则不需要
                        return redis;
                    }).collect(Collectors.toList());

                    return collect;
                }

                break;
            }
        }

        return null;
    }

    /**
     * 根据skuid查询对应的秒杀商品信息
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 1 找到所有需要的秒杀商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        Set<String> keys = hashOps.keys();
        if(keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            // 遍历数据库每一个key
            for(String key : keys) {
                // 找到与skuid匹配的商品
                if(Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

                    // 如果是当前秒杀时间，才加上随机码，否则为null
                    long current = new Date().getTime();
                    if(current < skuRedisTo.getStartTime() || current > skuRedisTo.getEndTime()) skuRedisTo.setRandomCode(null);
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    /**
     * 根据秒杀信息生成秒杀订单号
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();

        // 1 获取秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        String json = hashOps.get(killId);
        // 2 如果获取到数据，需要校验合法性，否则返回null
        if(!StringUtils.hasLength(json)) return null;

        SeckillSkuRedisTo redis = JSON.parseObject(json, SeckillSkuRedisTo.class);

        // 3 校验时间合法性
        Long startTime = redis.getStartTime();
        Long endTime = redis.getEndTime();
        long time = new Date().getTime();
        long ttl = endTime - time;
        if(time < startTime || time > endTime) return null;

        // 4 校验随机码和商品id
        String randomCode = redis.getRandomCode();
        String skuId = redis.getPromotionSessionId().toString() + "_" + redis.getSkuId().toString();
        if(!randomCode.equals(key) || !skuId.equals(killId)) return null;

        // 5 校验购买数量是否合理
        if(num <= 0 || num > redis.getSeckillLimit().intValue()) return null;

        // 6 校验该用户是否购买过【幂等性，只要秒杀成功，就去redis中占位，用userId_SessionId_skuId，并设置过期时间】
        String redisKey = respVo.getId() + "_" + killId;
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
        if(!aBoolean) return null;

        // 7 真正的下订单，锁定信号量
        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);

        boolean b = semaphore.tryAcquire(num);
        if(!b) return null;

        // 8 秒杀成功，快速下订单，发送MQ消息
        String timeId = IdWorker.getTimeId();
        SeckillOrderTo orderTo = new SeckillOrderTo();
        orderTo.setOrderSn(timeId);
        orderTo.setMemberId(respVo.getId());
        orderTo.setNum(num);
        orderTo.setPromotionSessionId(redis.getPromotionSessionId());
        orderTo.setSkuId(redis.getSkuId());
        orderTo.setSeckillPrice(redis.getSeckillPrice());

        rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
        return timeId;
    }
}
