package com.repair.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举：code 与 HTTP 状态码保持一致，前端按 code 分支处理。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    USERNAME_EXISTS(400, "用户名已存在"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    LOGIN_FAILED(401, "用户名或密码错误"),
    FORBIDDEN(403, "无权限访问"),
    ACCOUNT_DISABLED(403, "账号已被禁用"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    SERVER_ERROR(500, "服务器内部错误");

    private final Integer code;

    private final String msg;
}
