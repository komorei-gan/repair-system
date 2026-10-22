package com.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.common.result.Result;
import com.repair.dto.UpdateStatusRequest;
import com.repair.dto.UserInfoVO;
import com.repair.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端用户接口（design.md §3.4）。
 * - GET /api/admin/users?role=REPAIRER        ：按角色查询启用用户（扁平列表，供派单弹窗）
 * - GET /api/admin/users?pageNum=&pageSize=&keyword=&role= ：用户管理分页查询（含关键字/角色筛选）
 * - PUT /api/admin/users/{id}/status          ：启用/禁用用户
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 兼容双语义的用户查询：
     * 1) 传 role 且不传分页参数 → 返回启用用户的扁平列表（派单弹窗沿用旧行为）；
     * 2) 传分页参数（pageNum 非空）→ 返回分页结构（用户管理页），支持 keyword + role 筛选。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {

        // 分页模式：显式携带 pageNum 才走分页，避免影响派单弹窗
        if (pageNum != null) {
            long pn = Math.max(pageNum, 1);
            long ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
            PageResult<UserInfoVO> page = userService.pageUsers(
                    new Page<>(pn, ps),
                    keyword == null || keyword.isBlank() ? null : keyword.trim(),
                    role == null || role.isBlank() ? null : role.trim());
            return Result.success(page);
        }

        // 扁平列表模式：按角色查询启用用户（默认 REPAIRER，供派单选择师傅）
        String effectiveRole = role == null || role.isBlank() ? "REPAIRER" : role.trim();
        List<UserInfoVO> users = userService.listEnabledByRole(effectiveRole).stream()
                .map(UserInfoVO::from)
                .toList();
        return Result.success(users);
    }

    /** 启用/禁用用户（status: 1 启用 / 0 禁用） */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success();
    }
}
