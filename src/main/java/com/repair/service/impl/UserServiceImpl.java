package com.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.entity.User;
import com.repair.mapper.UserMapper;
import com.repair.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}
