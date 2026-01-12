package com.surenhao.backend.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Result<T> implements Serializable {

    private Integer code; // 200 成功, 401 未登录, 500 错误
    private String msg;
    private T data;

    // 1. 成功（带数据）
    public static <T> Result<T> data(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    // 2. 成功（不带数据）
    // 修改点：加上 <T>，允许返回任意类型的 Result (data为null)
    public static <T> Result<T> success() {
        return data(null);
    }

    // 3. 失败（带消息）
    // 🔥🔥 修改点：以前返回 Result<String>，现在改成 <T> Result<T>
    // 这样 Controller 想要什么类型，这里就能自动匹配什么类型
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMsg(msg);
        r.setData(null); // 失败时 data 为 null
        return r;
    }

    // 4. 自定义状态码（比如 401）
    // 修改点：支持泛型数据
    public static <T> Result<T> get(Integer code, String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}