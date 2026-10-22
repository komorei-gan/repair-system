package com.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.dto.UserInfoVO;
import com.repair.entity.User;

import java.util.List;

/**
 * 用户基础读写服务（供认证模块及后续工单/用户管理模块复用）。
 */
public interface UserService {

    /** 按用户名查询（逻辑删除的自动过滤） */
    User getByUsername(String username);

    /** 按主键查询（逻辑删除的自动过滤） */
    User getById(Long id);

    /** 用户名是否已存在 */
    boolean existsByUsername(String username);

    /** 新增用户 */
    void save(User user);

    /** 按用户名查询（包含已被逻辑删除的记录），用于种子账号兜底修复 */
    User getByUsernameIgnoreDeleted(String username);

    /** 强制重置种子账号（密码 / 角色 / 启用状态 / 逻辑删除标记一并修正） */
    void forceResetSeedUser(Long id, String rawPassword, String role);

    /** 按角色查询启用中的用户（如 REPAIRER），供派单选择师傅 */
    List<User> listEnabledByRole(String role);

    /** 用户管理分页查询：支持关键字（姓名/账号模糊）与角色筛选 */
    PageResult<UserInfoVO> pageUsers(Page<User> page, String keyword, String role);

    /** 启用/禁用用户（status: 1 启用 / 0 禁用） */
    void updateStatus(Long id, Integer status);
}
