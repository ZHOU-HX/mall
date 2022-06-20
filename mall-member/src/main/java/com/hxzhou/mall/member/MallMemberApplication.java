package com.hxzhou.mall.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 想要调用远程服务
 *      1 引入open-feign【在porm中进行】
 *      2 编写一个接口，告诉springcloud这个接口需要调用远程服务【在feign文件夹中couponfeignservice接口中进行】
 *          声明接口的每一个方法都是调用哪个远程服务的哪个请求
 *      3 开启远程调用功能【在启动类中进行】
 */
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.hxzhou.mall.member.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class MallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberApplication.class, args);
    }

}
