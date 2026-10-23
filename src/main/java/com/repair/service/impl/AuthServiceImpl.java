package com.repair.service.impl;

import com.repair.common.enums.RoleEnum;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.dto.ChangePasswordRequest;
import com.repair.dto.LoginRequest;
import com.repair.dto.LoginResponse;
import com.repair.dto.RegisterRequest;
import com.repair.dto.UserInfoVO;
import com.repair.entity.User;
import com.repair.mapper.UserMapper;
import com.repair.security.JwtUtil;
import com.repair.security.LoginUser;
import com.repair.security.SecurityUtil;
import com.repair.service.AuthService;
import com.repair.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final UserMapper userMapper;

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
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .user(UserInfoVO.from(user))
                .build();
    }

    @Override
    public void register(RegisterRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole(RoleEnum.REPORTER);
        user.setStatus(1);
        user.setFinishCount(0);
        userService.save(user);
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
}
