package com.surenhao.backend.handler;

import com.surenhao.backend.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 拦截我们自己抛出的 "401" 异常
    @ExceptionHandler(RuntimeException.class)
    public Result handlerRuntimeException(RuntimeException e) {
        // 如果是 401 异常
        if ("401".equals(e.getMessage())) {
            return Result.get(401, "您未登录或登录已过期", null);
        }
        e.printStackTrace();
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handlerException(Exception e) {
        e.printStackTrace();
        return Result.error("系统内部异常");
    }
}