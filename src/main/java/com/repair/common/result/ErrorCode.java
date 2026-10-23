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
    ORDER_NOT_FOUND(404, "工单不存在"),
    ORDER_STATUS_INVALID(400, "当前状态不允许该操作"),
    ORDER_NOT_OWNED(403, "无权操作该工单"),
    REPAIRER_INVALID(400, "维修师傅不存在或不可用"),
    CATEGORY_NOT_FOUND(404, "故障分类不存在或已停用"),
    REMIND_TOO_FREQUENT(400, "催单过于频繁，请稍后再试"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    LOGIN_FAILED(401, "用户名或密码错误"),
    FORBIDDEN(403, "无权限访问"),
    ACCOUNT_DISABLED(403, "账号已被禁用"),
    AUDIT_PENDING(403, "您的账号正在审核中，请耐心等待"),
    AUDIT_REJECTED(403, "您的账号已被拒绝，如有疑问请联系管理员"),
    RESET_NOT_APPROVED(403, "您的申请尚未被管理员批准"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    SERVER_ERROR(500, "服务器内部错误");

    private final Integer code;

    private final String msg;
}
