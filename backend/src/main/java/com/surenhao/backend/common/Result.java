package com.surenhao.backend.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class Result<T> implements Serializable {

    private Integer code; // 200:成功, 401:未登录, 403:无权限, 500:错误
    private String msg;
    private T data;

    // 1. 成功（带数据） -> Result.data(user)
    public static <T> Result<T> data(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    // 2. 成功（无数据，默认消息）-> Result.success()
    public static <T> Result<T> success() {
        return success("操作成功");
    }

    // 3. 成功（无数据，自定义消息）-> Result.success("封禁成功") 【本次新增】
    public static <T> Result<T> success(String msg) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }

    // 4. 失败（默认 500）-> Result.error("系统炸了")
    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }

    // 5. 失败（自定义错误码）-> Result.error(403, "权限不足")
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }
    // 【新增】为了兼容 Controller 中的 Result.fail(...) 调用
    public static <T> Result<T> fail(String msg) {
        return error(500, msg);
    }

}