package com.repair.service;

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

    /** 按角色查询启用中的用户（如 REPAIRER），供派单选择师傅 */
    List<User> listEnabledByRole(String role);
}
