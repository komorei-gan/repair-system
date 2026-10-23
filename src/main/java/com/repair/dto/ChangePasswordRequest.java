package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求（PUT /api/auth/password）。
 */
@Data
public class ChangePasswordRequest {

    /** 原密码（用于校验） */
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在 6~32 位之间")
    private String newPassword;
}
