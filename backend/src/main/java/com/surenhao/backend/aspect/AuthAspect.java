package com.surenhao.backend.aspect;

import com.surenhao.backend.annotation.Public;
import com.surenhao.backend.utils.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuthAspect {

    // 1. 定义切入点：拦截 com.surenhao.backend.controller 包下所有类的所有方法
    @Pointcut("execution(* com.surenhao.backend.controller..*.*(..))")
    public void controllerMethods() {}

    // 2. 环绕通知：决定是放行还是拦截
    @Around("controllerMethods()")
    public Object checkLogin(ProceedingJoinPoint point) throws Throwable {

        // 获取当前执行的方法
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // --- 核心逻辑 A：白名单放行 ---
        // 如果方法上加了 @Public 注解，直接放行，不查 ThreadLocal
        if (method.isAnnotationPresent(Public.class)) {
            return point.proceed();
        }

        // --- 核心逻辑 B：拦截 ---
        // 没加注解，就必须检查是否登录
        if (UserContext.get() == null) {
            throw new RuntimeException("401"); // 抛异常，由全局异常处理器捕获
        }

        // 校验通过，放行
        return point.proceed();
    }
}