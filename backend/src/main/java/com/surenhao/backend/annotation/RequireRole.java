package com.surenhao.backend.annotation;

import com.surenhao.backend.common.RoleEnum;
import java.lang.annotation.*;

@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface RequireRole {
    // 默认需要管理员权限，也可以传参指定其他角色
    RoleEnum value() default RoleEnum.ADMIN;
}