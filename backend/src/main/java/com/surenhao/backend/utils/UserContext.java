package com.surenhao.backend.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.surenhao.backend.entity.LoginUser; // 引入新类

public class UserContext {
    // 泛型从 SysUser 改为 LoginUser
    private static final ThreadLocal<LoginUser> userHolder = new TransmittableThreadLocal<>();

    public static void set(LoginUser user) {
        userHolder.set(user);
    }

    public static LoginUser get() {
        return userHolder.get();
    }

    public static void remove() {
        userHolder.remove();
    }
}