package com.repair.service.impl;

import com.repair.common.enums.AuditStatus;
import com.repair.common.enums.NotificationType;
import com.repair.common.enums.RoleEnum;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.dto.ChangePasswordRequest;
import com.repair.dto.ForgotPasswordRequest;
import com.repair.dto.LoginRequest;
import com.repair.dto.LoginResponse;
import com.repair.dto.RegisterRequest;
import com.repair.dto.ResetPasswordRequest;
import com.repair.dto.UserInfoVO;
import com.repair.entity.User;
import com.repair.mapper.UserMapper;
import com.repair.security.JwtUtil;
import com.repair.security.LoginUser;
import com.repair.security.SecurityUtil;
import com.repair.service.AuthService;
import com.repair.service.NotifyService;
import com.repair.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final UserMapper userMapper;

    private final NotifyService notifyService;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 统一提示，不暴露账号是否存在
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        // 师傅账号登录前校验审核状态：待审核 / 已拒绝一律拦截；报修人、管理员不受影响
        if (RoleEnum.REPAIRER.equals(user.getRole())) {
            Integer audit = user.getAuditStatus();
            if (audit == null || audit == AuditStatus.PENDING) {
                throw new BusinessException(ErrorCode.AUDIT_PENDING);
            }
            if (audit == AuditStatus.REJECTED) {
                throw new BusinessException(ErrorCode.AUDIT_REJECTED);
            }
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .user(UserInfoVO.from(user))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        String role = request.getRole();
        boolean isRepairer = RoleEnum.REPAIRER.equals(role);

        // 角色白名单：仅允许报修人 / 维修师傅，杜绝前端伪造 ADMIN
        if (!RoleEnum.REPORTER.equals(role) && !isRepairer) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "注册角色非法");
        }
        // 师傅注册必须填写申请理由
        if (isRepairer) {
            String reason = request.getReason();
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "申请理由不能为空");
            }
        }
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setStatus(1);
        user.setFinishCount(0);
        // 审核状态：报修人直接通过；师傅进入待审核
        user.setAuditStatus(isRepairer ? AuditStatus.PENDING : AuditStatus.APPROVED);
        user.setReason(isRepairer ? request.getReason().trim() : null);
        userService.save(user);

        // 师傅注册成功后，通知所有启用中的管理员前往审核（与注册同事务，保证「注册即通知」）
        if (isRepairer) {
            List<Long> adminIds = userService.listEnabledByRole(RoleEnum.ADMIN).stream()
                    .map(User::getId)
                    .toList();
            notifyService.notifyUsers(adminIds, NotificationType.SYSTEM,
                    "新的师傅注册申请",
                    "收到新的维修师傅注册申请，请前往【师傅审核】菜单处理。",
                    null);
        }
        // 并发重复注册由数据库唯一索引兜底，DuplicateKeyException 统一转为「用户名已存在」
    }

    @Override
    public UserInfoVO me() {
        LoginUser loginUser = SecurityUtil.getCurrentUser();
        User user = userService.getById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return UserInfoVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        LoginUser loginUser = SecurityUtil.getCurrentUser();
        User user = userService.getById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 校验原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码不正确");
        }
        // 新密码不能与原密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与原密码相同");
        }
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                .eq("id", user.getId())
                .set("password", passwordEncoder.encode(request.getNewPassword())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        // 标记为「已申请待批准」（reset_approved=1），供管理端按钮态识别
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                .eq("id", user.getId())
                .set("reset_approved", 1));
        // 通知所有启用中的管理员前往用户管理处理
        List<Long> adminIds = userService.listEnabledByRole(RoleEnum.ADMIN).stream()
                .map(User::getId)
                .toList();
        notifyService.notifyUsers(adminIds, NotificationType.SYSTEM,
                "密码重置申请",
                "用户【" + user.getUsername() + "】申请重置密码，请前往用户管理处理。",
                null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        // 仅「已批准」（reset_approved==2）才允许重置；未申请(0)/待批准(1)一律拦截
        if (user.getResetApproved() == null || user.getResetApproved() != 2) {
            throw new BusinessException(ErrorCode.RESET_NOT_APPROVED);
        }
        // 已批准：BCrypt 改密，并回写为「未申请」0（单次有效）
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                .eq("id", user.getId())
                .set("password", passwordEncoder.encode(request.getNewPassword()))
                .set("reset_approved", 0));
        // 状态变更写通知（AGENTS.md）
        notifyService.notify(user.getId(), NotificationType.SYSTEM,
                "密码已重置",
                "您的密码已重置成功，请使用新密码登录。",
                null);
    }
}
