package com.surenhao.backend.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {
    // 假设数据库里 0是普通用户，1是管理员
    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    private final Integer code;
    private final String msg;
}