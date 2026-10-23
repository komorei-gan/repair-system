package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码申请请求（POST /api/auth/forgot-password）。
 * 用户提交账号，系统校验存在后通知所有启用管理员。
 */
@Data
public class ForgotPasswordRequest {

    /** 登录账号 */
    @NotBlank(message = "请输入账号")
    @Size(max = 50, message = "账号长度不能超过 50 个字符")
    private String username;
}
