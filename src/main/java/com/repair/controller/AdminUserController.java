package com.repair.controller;

import com.repair.common.result.Result;
import com.repair.dto.UserInfoVO;
import com.repair.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端用户接口（design.md §3.4 GET /api/admin/users）。
 * 当前阶段仅提供「按角色查询启用用户」，供工单列表派单弹窗选择维修师傅。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 按角色查询启用中的用户（如 REPAIRER）。
     * 未传 role 时默认返回维修师傅列表（当前仅派单场景使用）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserInfoVO>> listByRole(@RequestParam(defaultValue = "REPAIRER") String role) {
        return Result.success(userService.listEnabledByRole(role).stream()
                .map(UserInfoVO::from)
                .toList());
    }
}
