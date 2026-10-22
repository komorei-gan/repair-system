package com.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.enums.RoleEnum;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.common.result.PageResult;
import com.repair.dto.UserInfoVO;
import com.repair.entity.User;
import com.repair.mapper.UserMapper;
import com.repair.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public User getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    @Override
    public void save(User user) {
        userMapper.insert(user);
    }

    @Override
    public User getByUsernameIgnoreDeleted(String username) {
        return userMapper.selectByUsernameIgnoreDeleted(username);
    }

    @Override
    public void forceResetSeedUser(Long id, String rawPassword, String role) {
        userMapper.forceResetSeedUser(id, passwordEncoder.encode(rawPassword), role);
    }

    @Override
    public List<User> listEnabledByRole(String role) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, role)
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
    }

    @Override
    public PageResult<UserInfoVO> pageUsers(Page<User> page, String keyword, String role) {
        // 关键字：姓名或账号模糊；角色：仅传合法角色时过滤
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getRealName, keyword)
                        .or()
                        .like(User::getUsername, keyword))
                .eq(role != null && RoleEnum.isValid(role), User::getRole, role)
                .orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(page, wrapper);
        return PageResult.from(result.convert(UserInfoVO::from));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值非法，仅支持 0（禁用）或 1（启用）");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                .eq("id", id)
                .set("status", status));
    }
}
