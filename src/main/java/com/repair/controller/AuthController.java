package com.repair.controller;

import com.repair.common.result.Result;
import com.repair.dto.ChangePasswordRequest;
import com.repair.dto.ForgotPasswordRequest;
import com.repair.dto.LoginRequest;
import com.repair.dto.LoginResponse;
import com.repair.dto.RegisterRequest;
import com.repair.dto.ResetPasswordRequest;
import com.repair.dto.UserInfoVO;
import com.repair.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（design.md §3.1，三端共用）。
 * Controller 只做参数接收与结果返回，业务逻辑全部在 Service 层（AGENTS.md 代码规范）。
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（三端同接口，返回 token + 角色） */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /** 双角色注册（报修人直接通过；师傅进入待审核） */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.me());
    }

    /** 修改密码（校验原密码，更新后需重新登录） */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success();
    }

    /** 忘记密码申请（匿名）：校验账号存在后通知所有启用管理员 */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return Result.success();
    }

    /** 忘记密码自助重置（匿名）：校验已批准后 BCrypt 改密 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }
}
