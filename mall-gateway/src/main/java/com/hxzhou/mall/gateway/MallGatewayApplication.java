package com.hxzhou.mall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1 开启网关的注册发现【在启动类中进行，用注解@EnableDiscoveryClient】
 * 2 配置项目模块的名称和nacos注册中心的地址【在Application.properties中进行】
 * 3 配置nacos的配置中心的地址和服务空间id【在bootstrap.properties中进行】
 * 4 创建新的命名空间，用来存放gateway模块的配置文件【在nacos客户端中进行】
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallGatewayApplication.class, args);
    }

}
