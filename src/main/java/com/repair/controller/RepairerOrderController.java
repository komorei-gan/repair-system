package com.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.common.result.Result;
import com.repair.dto.ApplyRequest;
import com.repair.dto.CompleteRequest;
import com.repair.dto.OrderVO;
import com.repair.dto.RepairerStatsVO;
import com.repair.service.OrderService;
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
 * 维修端接口（design.md §3.3）。
 */
@RestController
@RequestMapping("/api/repairer")
@RequiredArgsConstructor
public class RepairerOrderController {

    private final OrderService orderService;

    /** 我的维修任务（分页，按状态筛选） */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('REPAIRER')")
    public Result<PageResult<OrderVO>> myOrders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(orderService.repairerOrders(new Page<>(pageNum, pageSize), status));
    }

    /** 接单（ASSIGNED → PROCESSING）→ 通知报修人 */
    @PutMapping("/orders/{id}/accept")
    @PreAuthorize("hasRole('REPAIRER')")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.accept(id);
        return Result.success();
    }

    /** 申请改派（ASSIGNED → REASSIGNING，必填原因，证据图片选填）→ 通知管理员 */
    @PutMapping("/orders/{id}/reassign-request")
    @PreAuthorize("hasRole('REPAIRER')")
    public Result<Void> reassignRequest(@PathVariable Long id, @Valid @RequestBody ApplyRequest request) {
        orderService.reassignRequest(id, request);
        return Result.success();
    }

    /** 申请仲裁（PROCESSING → ARBITRATING，必填原因，证据图片选填）→ 通知管理员 */
    @PutMapping("/orders/{id}/arbitration-request")
    @PreAuthorize("hasRole('REPAIRER')")
    public Result<Void> arbitrationRequest(@PathVariable Long id, @Valid @RequestBody ApplyRequest request) {
        orderService.arbitrationRequest(id, request);
        return Result.success();
    }

    /** 提交完成（PROCESSING → COMPLETED）→ 通知报修人 */
    @PutMapping("/orders/{id}/complete")
    @PreAuthorize("hasRole('REPAIRER')")
    public Result<Void> complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest request) {
        orderService.complete(id, request);
        return Result.success();
    }

    /** 我的统计（接单数、完成数、平均评分等） */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('REPAIRER')")
    public Result<RepairerStatsVO> stats() {
        return Result.success(orderService.repairerStats());
    }
}
