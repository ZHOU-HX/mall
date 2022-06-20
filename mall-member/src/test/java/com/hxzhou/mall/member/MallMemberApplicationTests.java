package com.hxzhou.mall.member;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class MallMemberApplicationTests {

    @Test
    void contextLoads() {

        // e10adc3949ba59abbe56e057f20f883e
        // 抗修改性：彩虹表
        String s = DigestUtils.md5Hex("123456");

        // MD5不能直接进行密码的加密存储
        // 可以用盐值加密：随机值  加盐：$1$+8位字符
//        String s1 = Md5Crypt.md5Crypt("123456".getBytes(), "$1$lll");
//        System.out.println(s1);

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // $2a$10$wtx35Z.lje3XwI8tTBZTUuV6V6TZqgRMyllEMsWriygP1egNnX.a2
        String encode = passwordEncoder.encode("123456");

        boolean matches = passwordEncoder.matches("123456",
                "$2a$10$wtx35Z.lje3XwI8tTBZTUuV6V6TZqgRMyllEMsWriygP1egNnX.a2");

        System.out.println(encode + "---->" + matches);
    }

}
