package com.repair.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应：token + 用户信息（含角色，三端据此路由）。
 */
@Data
@Builder
public class LoginResponse {

    private String token;

    private UserInfoVO user;
}
