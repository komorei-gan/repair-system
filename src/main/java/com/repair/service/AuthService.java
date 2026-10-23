package com.repair.service;

import com.repair.dto.ChangePasswordRequest;
import com.repair.dto.ForgotPasswordRequest;
import com.repair.dto.LoginRequest;
import com.repair.dto.LoginResponse;
import com.repair.dto.RegisterRequest;
import com.repair.dto.ResetPasswordRequest;
import com.repair.dto.UserInfoVO;

/**
 * 认证业务（登录 / 注册 / 当前用户信息 / 修改密码 / 忘记密码审批流）。
 */
public interface AuthService {

    /** 登录：校验账号密码与状态，签发 JWT */
    LoginResponse login(LoginRequest request);

    /** 双角色注册（报修人直接通过；师傅进入待审核） */
    void register(RegisterRequest request);

    /** 当前登录用户信息（不含密码） */
    UserInfoVO me();

    /** 修改当前登录用户密码（校验原密码，加密后更新） */
    void changePassword(ChangePasswordRequest request);

    /** 忘记密码申请：校验账号存在后，通知所有启用管理员 */
    void forgotPassword(ForgotPasswordRequest request);

    /** 忘记密码自助重置：校验已批准后 BCrypt 改密，成功后回写未批准 */
    void resetPassword(ResetPasswordRequest request);
}
