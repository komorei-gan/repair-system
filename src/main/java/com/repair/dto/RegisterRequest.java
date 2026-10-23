package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数（双角色：报修人 REPORTER / 维修师傅 REPAIRER）。
 * role 由前端显式传入，服务端仅接受 REPORTER / REPAIRER，绝不允许 ADMIN。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度须为 3-50 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为 6-20 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    private String realName;

    /** 手机号必填，须为大陆手机号格式 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 注册角色：仅允许 REPORTER 报修人 / REPAIRER 维修师傅 */
    @NotBlank(message = "请选择注册角色")
    @Pattern(regexp = "REPORTER|REPAIRER", message = "注册角色非法")
    private String role;

    /** 申请理由（仅师傅注册必填，报修人忽略） */
    @Size(max = 500, message = "申请理由不能超过 500 字")
    private String reason;
}
