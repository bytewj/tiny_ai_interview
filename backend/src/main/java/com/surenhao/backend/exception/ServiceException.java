package com.surenhao.backend.exception;

import lombok.Getter;

/**
 * 业务异常 (支持自定义错误码)
 */
@Getter
public class ServiceException extends RuntimeException {
    private final Integer code;

    // 默认 500
    public ServiceException(String msg) {
        super(msg);
        this.code = 500;
    }

    // 自定义错误码 (比如 401, 403)
    public ServiceException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}