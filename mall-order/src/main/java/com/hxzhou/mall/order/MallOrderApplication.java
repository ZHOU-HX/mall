package com.hxzhou.mall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用RabbitMQ
 *  1 引入amqp场景：RabbitAutoConfiguration就会自动生效
 *  2 给容器中自动配置了：RabbitTemplate、AmqpAdmin、CachingConnectionFactory、RabbitMessagingTemplate
 *  3 @EnableRabbit
 *  4 所有属性都是 spring.rabbitmq，因此只需要在配置文件中配置以 spring.rabbitmq 开头的东东即可
 *          @ConfigurationProperties(prefix = "spring.rabbitmq")
 *          public class RabbitProperties
 *  5 监听消息：使用@RabbitListener注解
 *      @RabbitListener：标注在类或者方法上（监听哪些队列即可）
 *      @RabbitHandler：只能标注在方法上（重载区分不同的消息）
 *      使用场景：当消息队列中有不同的类型消息，可以将@RabbitListener标注在类上来指定接受的消息队列名称
 *                  然后将@RabbitHandler标注在不同方法上，方法里面的接收参数就是消息队列中的不同类型，
 *                  这样就会通过方法中接收参数类型，来分配消息队列内的对应的消息
 *
 *  本地事务失效问题
 *      现象：同一个对象内事务方法互调默认失效，原因在于绕过了代理对象，事务使用代理对象来控制的
 *      解决：使用代理对象来调用事务方法
 *          1 引入aop-starter：spring-boot-starter-aop引入了aspectj
 *          2 @EnableAspectJAutoProxy：开启aspectj动态代理功能，以后所有的动态代理都是aspectj创建的（即使没有接口也可以创建动态代理）
 *              @EnableAspectJAutoProxy(exposeProxy = true):对外暴露动态代理对象
 *          3 用调用对象本类互调
 *
 *  seata控制分布式事务
 *      1 每一个微服务先必须创建undo_log
 *      2 安装事务协调器：seata-server：https://github.com/seata/seata/releases
 *      3 整合
 *          3.1 导入依赖    spring-cloud-starter-alibaba-seata 同时还导入了一个seata-all
 *          3.2 解压并启动seata-server：最好与seata-all的版本对应
 *              registry.conf：注册中心配置【修改registry type=nacos】
 *              file.conf：配置中心配置
 *          3.3 在每一个事务上加上@GlobalTransactional注解
 *          3.4 所有想要用到的分布式事务的微服务使用seata DataSourceProxy 代理自己的数据源
 *          3.5 每一个微服务，都必须导入
 *                  【registry.conf】
 *                  【file.conf】：中的service.vgroup_mapping配置必须和spring.application.name一致
 *                                  即：vgroup_mapping.mall-order-fescar-service-group="default"
 *          3.6 启动测试分布式事务
 *              每一个大事务路口标注 @GlobalTransactional
 *              每一个远程的小事务用 @Transactional 即可
 */
@EnableAspectJAutoProxy(exposeProxy = true)     // 开启动态代理
@EnableRedisHttpSession     // 整合redis作为session存储
@EnableFeignClients
@EnableDiscoveryClient
@EnableRabbit
@SpringBootApplication
public class MallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOrderApplication.class, args);
    }

}
