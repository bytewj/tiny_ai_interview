package com.surenhao.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 专门用于 Redis 存储和 ThreadLocal 传递的用户信息
 * 不包含密码等敏感字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
}