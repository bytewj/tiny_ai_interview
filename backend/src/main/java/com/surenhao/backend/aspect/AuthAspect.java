package com.surenhao.backend.aspect;

import com.surenhao.backend.annotation.RequireRole;
import com.surenhao.backend.common.RoleEnum;
import com.surenhao.backend.entity.LoginUser;
import com.surenhao.backend.exception.ServiceException;
import com.surenhao.backend.utils.UserContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

@Aspect
@Component
public class AuthAspect {

    /**
     * 拦截所有加了 @RequireRole 注解的方法
     */
    @Before("@annotation(com.surenhao.backend.annotation.RequireRole)")
    public void checkRole(JoinPoint point) {
        // 1. 获取方法上的注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);

        // 2. 获取当前登录用户 (从 ThreadLocal 拿，前面拦截器已经放进去了)
        LoginUser currentUser = UserContext.get();

        // 3. 校验用户是否登录
        if (currentUser == null) {
            throw new RuntimeException("401"); // 未登录
        }

        // 4. 校验权限
        // 获取注解要求的权限 (例如 ADMIN)
        RoleEnum requiredRole = requireRole.value();

        // 获取用户实际的权限 (例如 USER)
        Integer userRoleCode = currentUser.getRole();

        // 如果需要的权限是 ADMIN (1)，但用户的权限是 USER (0)，则报错
        if (Objects.equals(requiredRole.getCode(), RoleEnum.ADMIN.getCode()) &&
                !Objects.equals(userRoleCode, RoleEnum.ADMIN.getCode())) {

            throw new ServiceException(403, "无权操作");
        }
    }
}