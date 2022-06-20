package com.hxzhou.mall.seckill.scheduled;

import com.hxzhou.mall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架：
 *      每天晚上3点，上架最近三天需要秒杀的商品
 *      当天00:00:00 - 后天23:59:59
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    private static final String UPLOAD_LOCK = "seckill:upload:lock:";

    @Scheduled(cron = "0 0 3 * * ?")        // 凌晨三点上架三天内的秒杀商品
//    @Scheduled(cron = "*/3 * * * * ?")        // 为了测试，改成现在立马执行
    public void uploadSeckillSkuLatest3Days() {
        log.info("执行上架秒杀的商品信息！！！");
        // 1 重复上架无需处理【分布式锁】
        RLock lock = redissonClient.getLock(UPLOAD_LOCK);
        lock.lock(10, TimeUnit.SECONDS);

        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();
        }
    }
}
