package com.hxzhou.mall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
 *
 *
 * 二、逻辑删除
 *      1 配置全局的逻辑删除规则【在application.yml中进行】
 *      2 配置逻辑删除的组件【MyBatisPlus 3.1.1版本之后可以省略】
 *      3 给Bean加上逻辑删除注解@TableLogic【在CategoryEntity中的showStatus变量进行】
 *
 * 三、JSP303（主要用于表单校验）
 *      1 给Bean添加校验注解，并定义自己的message提示【在entity中CategoryEntity中进行】【注解存在于javax.validation.constraints包中】
 *      2 开启校验功能@Valid【在controller中进行】
 *      3 给检验的bean后紧跟一个BindingResult，就可以获取到检验的结果【在controller中进行】
 *      4 分组校验（多场景的复杂校验）
 *          4.1 给注解上标注什么情况需要进行校验【在entity实体类中进行】
 *          4.2 在controller上进行指定情况【在controller类上进行】
 *          4.3 默认没有指定分组的校验注解@NotBlank，在分组校验的时候不生效，只会在@Validated不指定分组时生效
 *      5 自定义校验
 *          5.1 编写一个自定义的校验注解【在entity实体类对象上进行】
 *          5.2 编写一个自定义的校验器【在common模块中的ListValueConstraintValidator中进行】
 *          5.3 关联自定义的校验器和自定义的校验注解【在common模块中的ListValue中进行】
 *
 * 四、统一异常处理 @ControllerAdvice
 *      1 编写异常处理类，使用@ControllerAdvice【在exception包下进行】
 *      2 使用@ExceptionHandler标注方法可以处理的异常【在exception包下进行】
 *
 * 五、模板引擎
 *      1 thymeleaf-starter：关闭缓存
 *      2 静态资源都放在static文件夹下就可以按照路径直接访问
 *      3 页面放在template下，直接访问
 *          原因：SpringBoot访问项目时，默认会找index
 *      4 页面修改不重启服务器实时更新
 *          4.1 引入dev-tools
 *          4.2 修改完界面，还需要编译界面，即在界面中ctrl+shift+F9
 *
 * 六、整合redis
 *      1 引入data-redis-starter
 *      2 简单配置redis的host等信息
 *      3 使用springboot自动配置好的StringRedisTemplate来操作redis
 *
 * 七、整合redisson
 *      1 导入依赖【在pom.xml中进行】
 *      2 配置redisson【在MyRedissonConfig中进行】
 *
 * 八、整合SpringCache简化缓存开发
 *      1 引入依赖【在pom.xml中进行】
 *          spring-boot-starter-cache和spring-boot-starter-data-redis
 *      2 写配置
 *          2.1 自动配置
 *              CacheAutoConfiguration会导入RedisCacheConfiguration
 *              自动配好了缓存管理器RedisCacheManager
 *          2.2 配置使用redis作为缓存【在application.properties文件下进行】
 *      3 开启缓存功能【在启动类中进行】
 *      4 测试使用缓存【只需要使用注解就可以使用缓存操作】
 *          @Cacheable：触发将数据保存到缓存的操作
 *          @CacheEvict：触发将数据从缓存删除的操作
 *          @CachePut：不影响方法执行更新缓存
 *          @Caching：组合以上多个操作
 *          @CacheConfig：在类级别共享缓存的相同配置
 *      5 原理
 *          CacheAutoConfiguration -> RedisCacheConfiguration -> 自动配置了RedisCacheManager
 *          -> 初始化所有的缓存 -> 每个缓存决定使用什么配置 -> 如果redisCacheConfiguration有就用已有的，没有就用默认配置
 *          -> 想改缓存的配置，只需要给容器中放一个RedisCacheConfiguration即可
 *          -> 就会应用到当前RedisCacheManager管理的所有缓存分区中
 *
 */
@EnableFeignClients(basePackages = "com.hxzhou.mall.product.feign")        // 开启远程调用功能
@EnableDiscoveryClient
@MapperScan("com.hxzhou.mall.product.dao")
@SpringBootApplication
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }

}
