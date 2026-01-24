package com.surenhao.backend.handler;

import com.surenhao.backend.common.Result;
import com.surenhao.backend.exception.ServiceException; // 引入刚才新建的类
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 🔥 核心：拦截我们自定义的业务异常
     * 这样 AOP 抛出的 403 和 拦截器抛出的 401 都能准确透传给前端
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        // 调用 Result 中我们刚才新增的 error(code, msg) 方法
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 兜底：拦截所有未知的系统异常 (比如空指针、数据库连不上)
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统内部异常", e);
        return Result.error(500, "系统内部异常，请联系管理员");
    }
}