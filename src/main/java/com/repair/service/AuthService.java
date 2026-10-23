package com.repair.service;

import com.repair.dto.ChangePasswordRequest;
import com.repair.dto.LoginRequest;
import com.repair.dto.LoginResponse;
import com.repair.dto.RegisterRequest;
import com.repair.dto.UserInfoVO;

/**
 * 认证业务（登录 / 注册 / 当前用户信息 / 修改密码）。
 */
public interface AuthService {

    /** 登录：校验账号密码与状态，签发 JWT */
    LoginResponse login(LoginRequest request);

    /** 报修人注册（role 固定 REPORTER） */
    void register(RegisterRequest request);

    /** 当前登录用户信息（不含密码） */
    UserInfoVO me();

    /** 修改当前登录用户密码（校验原密码，加密后更新） */
    void changePassword(ChangePasswordRequest request);
}
