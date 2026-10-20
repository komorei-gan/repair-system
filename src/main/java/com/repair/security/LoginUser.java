package com.repair.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户（存入 SecurityContext 的 Principal）。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;

    private String username;

    private String role;
}
