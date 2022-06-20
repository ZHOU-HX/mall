package com.hxzhou.mall.cart.vo;

import lombok.Data;

@Data
public class UserInfoTo {

    private Long userId;        // 已登录用户id
    private String userKey;     // 没登录时分配的临时用户key

    private boolean tempUser = false;       // 判断临时用户是否是第一次被创建
}
