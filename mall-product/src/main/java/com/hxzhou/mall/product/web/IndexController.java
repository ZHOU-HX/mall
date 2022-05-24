package com.hxzhou.mall.product.web;

import com.hxzhou.mall.product.entity.CategoryEntity;
import com.hxzhou.mall.product.service.CategoryService;
import com.hxzhou.mall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        // 1 查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();

        model.addAttribute("categorys", categoryEntities);
        // 视图解析器进行拼串：classpath:/template/ + 返回值 + .html
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        // 1 获取一把锁，只要是锁的名字是一样的，就是同一把锁
        RLock lock = redisson.getLock("my-lock");

        /**
         * 1 锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s，不用担心业务时间长，锁自动过期被删掉
         * 2 加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s以后自动删除
         */
        // 2 加锁
        lock.lock();        // 阻塞式等待（默认加的锁存活时长为30s）
//        lock.lock(10, TimeUnit.SECONDS);        // 10秒后自动解锁【自动解锁时间一定要大于业务的执行时间；同时锁时间到了之后，不会自动续期】
        /**
         * 1 如果我们传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是我们指定的时间
         * 2 如果我们没有指定锁的超时时间，就使用30*1000【LockWatchdogTimeout看门狗默认时间】
         *      只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】
         *      三分之一的看门狗时间【10s】后进行续期
         *
         * 最佳实战：手动设置时间，即使没有看门狗，但是可以把时间设置大一点，然后手动解锁
         */
        try {
            System.out.println("加锁成功，执行业务" + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            // 3 解锁
            System.out.println("释放锁" + Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }

    // 保证一定能读到最新数据，修改期间，写锁是一个排它锁（互斥锁、独享锁），读锁是一个共享锁
    // 写锁没释放就必须等待
    /**
     * 读 + 读 ： 相当于无锁，并发读，只会在redis中记录好所有当前的读锁，他们都会同时加锁成功
     * 写 + 读 ： 等待写锁释放
     * 读 + 写 ： 等待读锁释放
     * 写 + 写 ： 阻塞方式
     * 只要有写，都必须等待
     * @return
     */
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";

        RLock rLock = lock.writeLock();
        try {
            // 改数据加写锁，读数据加读锁
            rLock.lock();
            System.out.println("写锁加锁成功——" + Thread.currentThread().getId());

            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            stringRedisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("写锁释放——" + Thread.currentThread().getId());
        }

        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";

        RLock rLock = lock.readLock();
        try {
            rLock.lock();
            System.out.println("读锁加锁成功——" + Thread.currentThread().getId());

            s = stringRedisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("读锁释放——" + Thread.currentThread().getId());
        }

        return s;
    }

    /**
     * 模拟车库停车，有三个车位
     * 信号量也可以用作分布式限流
     */
    // 停车
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
//        park.acquire();     // 获取一个信号，获取一个值，占一个车位
        boolean b = park.tryAcquire();
        if(b) {
            return "ok";
        }
        else {
            return "error";
        }
    }

    // 离开
    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        park.release();     // 释放一个信号，释放一个车位

        return "ok";
    }

    /**
     * 放假锁门：一共五个班级，所有人都走光了才能锁大门
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();       // 等待闭锁都完成

        return "放假咯！！";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();   // 计数减一

        return id + "班的人都走了！";
    }

}
