package com.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.common.result.Result;
import com.repair.dto.RepairerAuditVO;
import com.repair.dto.UpdateAuditStatusRequest;
import com.repair.entity.User;
import com.repair.service.RepairerAuditService;
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

/**
 * 管理端师傅审核接口。
 * - GET /api/admin/repairers/audit?auditStatus=&keyword=&pageNum=&pageSize= ：师傅审核分页查询
 * - PUT /api/admin/repairers/audit/{id}                         ：通过/拒绝师傅
 */
@RestController
@RequestMapping("/api/admin/repairers/audit")
@RequiredArgsConstructor
public class AdminRepairerAuditController {

    private final RepairerAuditService repairerAuditService;

    /** 师傅审核分页查询（auditStatus 可选：0 待审核 / 1 已通过 / 2 已拒绝） */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<RepairerAuditVO>> list(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {

        long pn = Math.max(pageNum == null ? 1 : pageNum, 1);
        long ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        PageResult<RepairerAuditVO> page = repairerAuditService.pageAudit(
                new Page<>(pn, ps),
                auditStatus,
                keyword == null || keyword.isBlank() ? null : keyword.trim());
        return Result.success(page);
    }

    /** 待审核（auditStatus=0）师傅申请数，供左侧菜单角标展示 */
    @GetMapping("/pending-count")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> pendingCount() {
        return Result.success(repairerAuditService.pendingCount());
    }

    /** 通过/拒绝师傅（auditStatus: 1 通过 / 2 拒绝） */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody UpdateAuditStatusRequest request) {
        repairerAuditService.audit(id, request.getAuditStatus());
        return Result.success();
    }
}
