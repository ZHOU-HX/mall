package com.hxzhou.mall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 一、整合mybatis-plus
 *      1 导入依赖【在maven中进行】
 *      2 配置
 *          2.1 配置数据源
 *              2.1.1 导入数据库的驱动【在maven中进行】
 *              2.1.2 配置数据源【在application.yml文件中进行】
 *          2.2 配置MyBatis-Plus
 *              2.2.1 添加注解MapperScan来扫描所有的dao文件【在启动项类中进行，即当前类中】
 *              2.2.2 告诉MyBatis-Plus，Sql映射文件位置【在application.yml文件中进行】
 *              2.2.3 设置主键自增【在application.yml文件中进行】
 *      3 测试【在test中进行】
 */
@MapperScan("com.hxzhou.mall.product.dao")
@SpringBootApplication
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }

}
