package com.surenhao.backend.annotation;

import java.lang.annotation.*;

/**
 * 加上此注解的接口，无需登录即可访问 (公开接口)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Public {
}