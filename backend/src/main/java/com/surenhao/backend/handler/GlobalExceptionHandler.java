package com.surenhao.backend.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.util.SaResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 拦截：未登录异常
    @ExceptionHandler(NotLoginException.class)
    public SaResult handlerNotLoginException(NotLoginException nle) {
        // 打印一行日志即可，不需要打印堆栈
        System.out.println("捕获未登录异常：" + nle.getMessage());

        // 返回给前端 401 状态码
        return SaResult.get(401, "请先登录", null);
    }

    // 拦截：其他所有异常 (兜底)
    @ExceptionHandler(Exception.class)
    public SaResult handlerException(Exception e) {
        e.printStackTrace(); // 打印堆栈以便调试
        return SaResult.error(e.getMessage());
    }
}