package com.hxzhou.mall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hxzhou.common.exception.NoStockException;
import com.hxzhou.common.to.MemberRespVo;
import com.hxzhou.common.to.mq.OrderTo;
import com.hxzhou.common.to.mq.SeckillOrderTo;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.order.constant.OrderConstant;
import com.hxzhou.mall.order.controller.RabbitController;
import com.hxzhou.mall.order.dao.OrderItemDao;
import com.hxzhou.mall.order.entity.OrderItemEntity;
import com.hxzhou.mall.order.entity.PaymentInfoEntity;
import com.hxzhou.mall.order.enume.OrderStatusEnum;
import com.hxzhou.mall.order.feign.CartFeignService;
import com.hxzhou.mall.order.feign.MemberFeignService;
import com.hxzhou.mall.order.feign.ProductFeignService;
import com.hxzhou.mall.order.feign.WareFeignService;
import com.hxzhou.mall.order.interceptor.LoginUserInterceptor;
import com.hxzhou.mall.order.service.OrderItemService;
import com.hxzhou.mall.order.service.PaymentInfoService;
import com.hxzhou.mall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.order.dao.OrderDao;
import com.hxzhou.mall.order.entity.OrderEntity;
import com.hxzhou.mall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认页返回需要用的数据
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        // 能来到这里的，都是登录后的用户，直接在session中取出会员信息就好
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        // 开始封装所需信息
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        /**
         * 获取主线程的请求信息
         */
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        /**
         * feign在远程调用之前要构造请求，调用很多拦截器
         */
        // 1 远程查询所有的收货地址
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            /**
             * 将主线程的请求信息copy一份给线程池中当前线程
             */
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);

        // 2 远程查询购物车内被选中的购物项信息
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            /**
             * 将主线程的请求信息copy一份给线程池中当前线程
             */
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<OrderItemVo> cartItems = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(cartItems);
        }, executor).thenRunAsync(() -> {
            // 远程调用ware服务查询是否有库存
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());

            R hasStock = wareFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {});

            if(data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);

        // 3 查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        // 4 其他数据自动计算

        // TODO 5 订单防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(),
                token,
                30,
                TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

    /**
     * 下单操作：验证令牌->创建订单->检验价格->保存订单->锁定库存
     * 注意：
     *      Transactional：本地事务，在分布式系统，只能控制住自己的回滚，控制不了其他服务的回滚
     *      所以要使用分布式事务
     * @param vo
     * @return
     */
//    @GlobalTransactional
    @Transactional
    @Override
    public OrderSubmitResponseVo submitOrder(OrderSubmitVo vo) {
        OrderSubmitResponseVo responseVo = new OrderSubmitResponseVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        confirmVoThreadLocal.set(vo);

        /**
         * 1 验证令牌【令牌的对比和删除必须保证原子性】
         */
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        // 原子验证令牌和删除令牌
        Long result = redisTemplate.execute(
                new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()),
                orderToken);
        // 令牌验证失败
        if(result == 0L) {
            responseVo.setCode(1);
            return responseVo;
        }

        // 令牌验证成功
        /**
         * 2 创建订单项
         */
        OrderCreateTo order = createOrder();

        /**
         * 3 检验价格
         */
        BigDecimal payAmount = order.getOrder().getPayAmount();
        BigDecimal payPrice = vo.getPayPrice();
        // 金额对比失败
        if(Math.abs(payAmount.subtract(payPrice).doubleValue()) >= 0.01) {
            responseVo.setCode(2);
            return responseVo;
        }

        // 金额对比成功
        /**
         * 4 保存订单
         */
        saveOrder(order);

        /**
         * 5 锁定库存。只要有异常，就回滚订单数据【需要订单号、所有订单项（skuID、skuName、num）】
         */
        WareSkuLockVo lockVo = new WareSkuLockVo();
        // 订单号
        lockVo.setOrderSn(order.getOrder().getOrderSn());
        // 所有订单项
        List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
            OrderItemVo itemVo = new OrderItemVo();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setCount(item.getSkuQuantity());
            itemVo.setTitle(item.getSkuName());
            return itemVo;
        }).collect(Collectors.toList());
        lockVo.setLocks(locks);
        // 为了保证高并发，库存服务自己回滚，可以发消息给库存服务；库存服务也可以使用自动解锁模式  【消息队列】来解决
        R r = wareFeignService.orderLockStock(lockVo);

        // 锁定失败
        if(r.getCode() != 0) {
            responseVo.setCode(3);
            throw new NoStockException(r.getData(new TypeReference<Long>() {}));
//            return responseVo;
        }

//        /**
//         * 6 模拟远程调用积分服务出现异常
//         */
//        int i = 10 / 0;         // 订单回滚，库存不回滚

        /**
         * 7 订单创建成功，将其放入延时队列中
         */
        rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());

        responseVo.setCode(0);
        responseVo.setOrder(order.getOrder());
        return responseVo;
    }

    /**
     * 下单操作步骤二开始：创建订单项======================================================================================
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();

        /**
         * 一、随机生成订单号并创建订单
         */
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);

        /**
         * 二、获取所有订单项信息
         */
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        /**
         * 三、计算价格、积分等相关信息
         */
        computePrice(orderEntity, itemEntities);

        /**
         * 四、将上述计算好的信息进行存储
         */
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);

        return orderCreateTo;
    }

    /**
     * 创建订单项一、构建订单
     * @param orderSn
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();

        // 1 创建订单
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(respVo.getId());

        // 2 获取收货地址信息
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wareFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {});

        // 3 获取运费金额
        entity.setFreightAmount(fareResp.getFare());

        // 4 设置收货人信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        // 5 设置订单的相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);

        return entity;
    }

    /**
     * 创建订单项二：构建所有订单项数据
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();

        if(currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);

                return itemEntity;
            }).collect(Collectors.toList());

            return itemEntities;
        }

        return null;
    }

    /**
     * 创建订单项二：构建所有订单项数据一：构建某一个订单项
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();

        // 1 订单信息：订单号【在外面已经构建好了】

        // 2 商品的SPU信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {});
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        // 3 商品的SKU信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());

        // 4 优惠信息【忽略】

        // 5 积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // 6 订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = origin.subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);    // 当前订单项的实际金额：总额-各种优惠

        return itemEntity;
    }

    /**
     * 创建订单项三：计算价格、积分等相关信息
     * @param orderEntity
     * @param itemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");

        // 订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity entity : itemEntities) {
            total = total.add(entity.getRealAmount());
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }

        // 订单价格
        orderEntity.setTotalAmount(total);

        // 应付价格：订单价格+运费
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        // 各项优惠金额
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);

        // 积分信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        orderEntity.setDeleteStatus(0);         // 未删除
    }
    /**
     * 下单操作步骤二结束：创建订单项======================================================================================
     */

    /**
     * 下单操作步骤四开始：保存订单========================================================================================
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        // 保存订单
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        // 保存订单项
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 下单操作步骤四结束：保存订单========================================================================================
     */

    /**
     * 根据订单号查询订单信息
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * 关闭订单功能
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        // 1 查询当前这个订单的状态信息
        OrderEntity orderEntity = this.getById(entity.getId());

        // 2 只有待付款状态超时了才会关闭订单
        if(orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);

            // 防止库存提前锁定【意外情况】，在关闭订单成功后，向ware服务中的普通消息队列再次发送一条订单关闭消息
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            orderTo.setStatus(OrderStatusEnum.CANCLED.getCode());

            try {
                // TODO 保证消息一定会发送出去，每一个消息都可以做好日志记录（给数据库保存每一个消息的详情信息）
                // TODO 定期扫描数据库将失败的消息再发送一次
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other.ware", orderTo);
            } catch (Exception e) {
                // TODO 将没发送成功的消息进行重试发送
            }

        }
    }

    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        OrderEntity order = this.getOrderByOrderSn(orderSn);
//        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, RoundingMode.UP);

        PayVo payVo = new PayVo();
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(order.getOrderSn());

        List<OrderItemEntity> entities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = entities.get(0);
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());

        return payVo;
    }

    /**
     * 查询当前用户下的订单列表数据
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id"));

        List<OrderEntity> orderSn = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(
                    new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(orderSn);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝的支付结果
     * @param vo
     * @return
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 1 保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(infoEntity);

        // 2 修改订单的状态信息
        if(vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            // 支付成功状态
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }

        // 3 执行成功返回"success"
        return "success";
    }

    /**
     * 创建秒杀单的详细信息
     * @param entity
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo entity) {
        // TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(entity.getOrderSn());
        orderEntity.setMemberId(entity.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        BigDecimal multiply = entity.getSeckillPrice().multiply(new BigDecimal("" + entity.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);

        // TODO 保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(entity.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(entity.getNum());
        orderItemService.save(orderItemEntity);

        // TODO 获取当前SKU的详细信息进行设置        productFeignService.getSpuInfoBySkuId()
    }
}