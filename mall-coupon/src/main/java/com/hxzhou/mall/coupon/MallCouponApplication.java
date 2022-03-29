package com.hxzhou.mall.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 配置中心的设置流程：
 *      1 引入依赖【在公共模块mall-common中的pom.xml进行】
 *      2 创建一个bootstrap.properties文件【在自己的配置中，与application.properties同级】
 *      3 需要给配置中心默认添加一个叫 数据集（Data Id）mall-coupon.properties【默认规则；应用名.properties】【在配置中心进行】
 *      4 给 应用名.properties 添加任何配置
 *      5 动态获取配置【在controller中进行】
 *          5.1 @RefreashScope：动态获取并刷新配置
 *          5.2 @Value("${配置项的名}")：获取到配置
 *      注：如果配置中心和当前应用的配置文件中都配置了相同的项，优先使用配置中心的配置。
 *
 * 细节
 *      1 命名空间：配置隔离
 *          1.1 默认：public（保留空间），默认新增的所有配置都在public空间。
 *          1.2 开发、测试、生产：利用命名空间来做环境隔离
 *              注意：在bootstrap.properties配置中，需要使用哪个命名空间下的配置，
 *                    即：spring.cloud.nacos.config.namespace=b52da667-1f63-4bd9-87cc-77f602ca4c9f
 *          1.3 coupon、member、order、product、ware：按照功能模块来做环境隔离
 *              每一个微服务之间互相隔离配置，每一个微服务都创建自己的命名空间，只加载自己命名空间下的所有配置
 *      2 配置集：所有配置的集合
 *      3 配置集ID：类似文件名
 *          Data ID：就是配置文件名
 *      4 配置分组：默认所有的配置集都属于：DEFAULT_GROUP
 *
 * 项目中的使用：每个微服务创建自己的命名空间，使用配置分组区分环境：dev、test、prod
 *
 * 同时加载多个配置集
 *      1 微服务任何配置信息，任何配置文件都可以放在配置中心中
 *      2 只需要在bootstrap.properties中说明加载配置中心中哪些配置文件即可
 *      3 以前SpringBoot任何方法从配置文件中获取值，都能使用。例如：@Value、@ConfigurationProperties...
 *      4 配置中心有的优先使用配置中心的
 */
@EnableDiscoveryClient
@SpringBootApplication
public class MallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallCouponApplication.class, args);
    }

}
