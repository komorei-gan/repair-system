package com.repair.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构（AGENTS.md：包含 code / msg / data）。
 * code 约定与 HTTP 状态一致：200 成功、400 参数错误、401 未登录、403 无权限、404、500。
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;

    private String msg;

    private T data;

    public static <T> Result<T> success() {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    public static <T> Result<T> success(T data) {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String msg) {
        return build(errorCode.getCode(), msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return build(code, msg, null);
    }

    private static <T> Result<T> build(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}
