package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码自助重置请求（POST /api/auth/reset-password）。
 * 仅当目标账号 reset_approved == 1 时允许修改密码。
 */
@Data
public class ResetPasswordRequest {

    /** 登录账号 */
    @NotBlank(message = "请输入账号")
    @Size(max = 50, message = "账号长度不能超过 50 个字符")
    private String username;

    /** 新密码 */
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在 6~32 位之间")
    private String newPassword;
}
