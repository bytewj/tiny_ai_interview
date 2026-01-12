package com.surenhao.backend.utils;

import com.surenhao.backend.entity.SysUser;

public class UserContext {
    private static final ThreadLocal<SysUser> userHolder = new ThreadLocal<>();

    public static void set(SysUser user) {
        userHolder.set(user);
    }

    public static SysUser get() {
        return userHolder.get();
    }

    public static void remove() {
        userHolder.remove();
    }
}