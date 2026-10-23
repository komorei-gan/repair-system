package com.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.common.result.Result;
import com.repair.dto.AssignRequest;
import com.repair.dto.OrderQuery;
import com.repair.dto.OrderVO;
import com.repair.dto.ReasonRequest;
import com.repair.dto.ReassignRequest;
import com.repair.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端工单接口（design.md §3.4）。
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /** 工单分页查询（状态/分类/时间/关键字） */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<OrderVO>> orders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            OrderQuery query) {
        return Result.success(orderService.adminOrders(new Page<>(pageNum, pageSize), query));
    }

    /** 待受理（PENDING）工单数，供左侧菜单角标展示 */
    @GetMapping("/pending-count")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> pendingCount() {
        return Result.success(orderService.pendingCount());
    }

    /** 派单（PENDING → ASSIGNED，指定师傅）→ 通知师傅 */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        orderService.assign(id, request);
        return Result.success();
    }

    /** 改派（ASSIGNED/PROCESSING → ASSIGNED，换人）→ 通知新旧师傅 */
    @PutMapping("/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reassign(@PathVariable Long id, @Valid @RequestBody ReassignRequest request) {
        orderService.reassign(id, request);
        return Result.success();
    }

    /** 驳回（PENDING → REJECTED，必填原因）→ 通知报修人 */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        orderService.rejectByAdmin(id, request);
        return Result.success();
    }

    /** 管理员取消（PENDING/ASSIGNED → CANCELLED）→ 通知报修人（有师傅则一并） */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        orderService.cancelByAdmin(id, request);
        return Result.success();
    }

    /** 删除工单（仅 PENDING/CANCELLED 可逻辑删除） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.deleteByAdmin(id);
        return Result.success();
    }
}
