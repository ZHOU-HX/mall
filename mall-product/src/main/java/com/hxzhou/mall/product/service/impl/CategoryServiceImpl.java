package com.hxzhou.mall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hxzhou.mall.product.service.CategoryBrandRelationService;
import com.hxzhou.mall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.product.dao.CategoryDao;
import com.hxzhou.mall.product.entity.CategoryEntity;
import com.hxzhou.mall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    private CategoryDao categoryDao;
//    private Map<String, Object> cache = new HashMap<>();

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1、查询所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2、组装成父子的树形结构
        // 2.1 找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    // 递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            // 1 找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 2 菜单排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 1、检查当前删除的菜单，是否被别的地方引用

        // 逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 根据id查询完整路径
     *
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();

        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }

    // 递归查询父id
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        // 收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) findParentPath(byId.getParentCid(), paths);
        return paths;
    }

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 级联更新所有关联的数据
     *
     * @CacheEvict：失效模式
     * @Caching：同时进行多种缓存操作
     * 存储同一类型数据，都可以指定成同一分区。分区名默认就是缓存的前缀
     * @param category
     */
//    @Caching(evict = {
//            @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'"),
//            @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
//    })
    @CacheEvict(value = {"category"}, allEntries = true)    // 指定删除某个分区下的所有数据
//    @CachePut       // 双写模式可以用这个接口
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 查询所有的一级分类
     *
     * @return
     */
    /**
     * 1 每一个需要缓存的数据我们都来指定要放到哪个名字的缓存【缓存的分区（按照业务类型分区）】
     * 2 @Cacheable({"category"})
     *      代表当前方法的结果需要缓存，如果缓存中有，方法不用调用；
     *      如果缓存中没有，会调用方法，最后将方法结果放入缓存中。
     * 3 默认行为
     *      3.1 如果缓存中有，方法不用调用
     *      3.2 key默认自动生成：缓存的名字::SimpleKey []
     *      3.3 缓存的value值。默认使用jdk序列化机制，将序列化后的数据存到redis
     *      3.4 默认ttl时间 -1：永不过期
     * 4 需要自定义
     *      4.1 指定生成的缓存使用的key：key属性指定，接收一个spEL表达式
     *      4.2 指定缓存的数据存活时间：在配置文件中修改ttl
     *      4.3 将数据保存为json格式
     * 5 Spring-Cache的不足：
     *      5.1 读模式：
     *          缓存穿透：查询一个null数据。解决：缓存空数据【spring.cache.redis.cache-null-values=true】
     *          缓存击穿：大量并发进来同时查询一个正好过期的数据。解决：加锁，但是SpringCache默认是不加锁的，可以设置sync = true
     *          缓存雪崩：大量的key同时过期。解决：加随机时间、加上过期时间【spring.cache.redis.time-to-live=3600000】
     *      5.2 写模式：（缓存和数据库一致）
     *          5.2.1 读写加锁
     *          5.2.2 引入Canal，感知到MySQL的更新去更新数据库
     *          5.2.3 读少写多，直接去数据库查询就行
     *      5.3 总结
     *          常规数据（读多写少，即时性、一致性要求不高的数据）：完全可以用Spring-Cache；写模式（只要缓存的数据有过期时间就足够了）
     *          特殊数据：特殊设计
     * 6 原理
     *      CacheManager(RedisCachaManager) -> Cache(RedisCache) -> Cache负责缓存的读写
     *
     */
    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("调用方法，查询数据库！！！");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(
                new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    @Cacheable(value = {"category"}, key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库");

        /**
         * 优化数据库查询：
         *      1 将数据库的多次查询变成一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 1 查出所有一级分类
        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);

        // 2 封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 2.1 每一个的一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            // 2.2 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map((l2) -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(
                            v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    // 2.2.1 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map((l3) -> {
                            // 2.2.2 封装成指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(
                                    l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());

                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        return parentCid;
    }

    /**
     * 查出一级路径下所有分类并按照要求封装（加入缓存机制的）
     * <p>
     * TODO 产生堆外内存溢出 OutOfDirectMemoryError
     * 1 springboot2.0以后默认使用lettuce作为操作redis的客户端，他使用netty进行网络通信
     * 2 lettuce的bug导致netty堆外内存溢出；netty如果没有指定堆外内存，默认使用-Xms300m
     * 可以通过-Dio.netty.maxDirectMemory进行设置
     * 解决方案：不能使用-Dio.netty.maxDirectMemory只去调大堆外内存
     * 1 升级lettuce客户端
     * 2 切换使用jedis
     * redisTemplate:
     * lettuce、jedis操作redis的底层客户端。Spring再次封装redisTemplate
     */
//    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        // 给缓存中放json字符串，拿出的字符串，还用逆转为能用的对象类型；【序列化与反序列化】

        /**
         * 1、空结果缓存：解决缓存穿透
         * 2、设置过期时间（加随机值）：解决缓存雪崩
         * 3、加锁：解决缓存击穿
         */

        // 加入缓存机制，缓存中存的数据是json字符串
        // JSON跨语言，跨平台兼容
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");

        if (StringUtils.isEmpty(catalogJSON)) {
            // 缓存中没有，查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithRedisLock();

            return catalogJsonFromDB;
        }

        // 转为我们指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    /**
     *
     * @return
     */
    // Redisson分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedissonLock() {
        // 1 获取锁，其中锁的名字代表锁的粒度，越细越好
        // 锁的粒度：具体缓存的是某个数据，例如11号商品，可以是 product-11-lock
        RLock lock = redisson.getLock("CatalogJson-lock");
        lock.lock();

        // 加锁成功，执行业务
        Map<String, List<Catelog2Vo>> dataFromDB;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }

        return dataFromDB;
    }

    // Redis分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedisLock() {
        // 占分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if(lock) {
            // 设置过期时间：但是一定要保证占锁和设置过期时间是原子的
//            stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            System.out.println("获取分布式锁成功");

            // 加锁成功，执行业务
            Map<String, List<Catelog2Vo>> dataFromDB;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                // 获取值对比+对比成功后删除=原子操作       lua脚本操作解锁
//            String lockValue = stringRedisTemplate.opsForValue().get("lock");
//            if(uuid.equals(lockValue)) {
//                // 业务执行完毕，释放自己的锁
//                stringRedisTemplate.delete("lock");
//            }
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 原子删除锁
                Long lock1 = stringRedisTemplate.execute(
                        new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }

            return dataFromDB;
        }
        else {
            // 加锁失败，重试
            // 休眠100ms重试
            System.err.println("获取分布式锁失败，等待重试！");
            try {
                Thread.sleep(200);
            } catch (Exception e) {}
            return getCatalogJsonFromDBWithRedisLock();     // 自旋的方式
        }
    }

    /**
     * 查出一级路径下所有分类并按照要求封装（未加入缓存的，仅仅是从数据库查询）
     *
     * @return
     */
    // 本地锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithLocalLock() {

//        Map<String, List<Catelog2Vo>> catalogJson = (Map<String, List<Catelog2Vo>>) cache.get("catalogJson");
//
//        // 如果缓存中有，直接返回缓存数据
//        if(catalogJson != null) return catalogJson;

        /**
         * 加锁来解决缓存击穿问题
         * 只要是同一把锁，就能锁住需要这个锁的所有线程
         *      1 synchronized (this)：SpringBoot所有的组件在容器中都是单例的
         *          本地锁：synchronized，JUC（Lock），在分布式情况下，想要锁住所有，必须使用分布式锁
         */
        synchronized (this) {
            // 得到锁之后，再次去缓存中确定一下，如果没有才进行查询
            return getDataFromDB();
        }
//        // 1 查出所有一级分类
//        List<CategoryEntity> level1Categorys = getLevel1Categorys();
//
//        // 2 封装数据
//        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
//            // 2.1 每一个的一级分类，查到这个一级分类的二级分类
//            List<CategoryEntity> categoryEntities = baseMapper.selectList(
//                    new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
//
//            // 2.2 封装上面的结果
//            List<Catelog2Vo> catelog2Vos = null;
//            if (categoryEntities != null) {
//                catelog2Vos = categoryEntities.stream().map((l2) -> {
//                    Catelog2Vo catelog2Vo = new Catelog2Vo(
//                            v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
//
//                    // 2.2.1 找当前二级分类的三级分类封装成vo
//                    List<CategoryEntity> level3Catelog = baseMapper.selectList(
//                            new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
//                    if(level3Catelog != null) {
//                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map((l3) -> {
//                            // 2.2.2 封装成指定格式
//                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(
//                                    l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
//
//                            return catelog3Vo;
//                        }).collect(Collectors.toList());
//
//                        catelog2Vo.setCatalog3List(collect);
//                    }
//
//                    return catelog2Vo;
//                }).collect(Collectors.toList());
//            }
//
//            return catelog2Vos;
//        }));
//
//        return parentCid;
    }

    // 从数据库查询一级分类信息
    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            // 如果缓存不为空，直接return
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        System.out.println("查询了数据库");

        /**
         * 优化数据库查询：
         *      1 将数据库的多次查询变成一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 1 查出所有一级分类
        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);

        // 2 封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 2.1 每一个的一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            // 2.2 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map((l2) -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(
                            v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    // 2.2.1 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map((l3) -> {
                            // 2.2.2 封装成指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(
                                    l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());

                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

//        cache.put("catalogJson", parentCid);
        // 查到的数据再放入缓存，将对象转为json放在缓存中
        String s = JSON.toJSONString(parentCid);
        stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parentCid;
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        List<CategoryEntity> collect = selectList.stream()
                .filter(item -> item.getParentCid() == parentCid)
                .collect(Collectors.toList());

        return collect;
    }

}