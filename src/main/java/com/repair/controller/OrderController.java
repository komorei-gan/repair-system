package com.repair.controller;

import com.repair.common.result.PageResult;
import com.repair.common.result.Result;
import com.repair.dto.ConfirmRequest;
import com.repair.dto.CreateOrderRequest;
import com.repair.dto.OrderDetailVO;
import com.repair.dto.OrderVO;
import com.repair.dto.ReasonRequest;
import com.repair.dto.UpdateOrderRequest;
import com.repair.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报修端工单接口（design.md §3.2）。
 * Controller 只做参数接收与结果返回，业务与状态流转全部在 Service 层。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 提交报修工单 → 通知管理员 */
    @PostMapping
    @PreAuthorize("hasRole('REPORTER')")
    public Result<Long> submit(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.submit(request));
    }

    /** 我的报修列表（分页，按状态筛选） */
    @GetMapping("/my")
    @PreAuthorize("hasRole('REPORTER')")
    public Result<PageResult<OrderVO>> myOrders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(orderService.myOrders(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), status));
    }

    /** 工单详情（含操作日志）；三端均可访问，归属校验在 Service */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /** 修改工单（仅 PENDING，状态不变）→ 记日志并通知管理员 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('REPORTER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        orderService.update(id, request);
        return Result.success();
    }

    /** 取消工单（PENDING）→ 通知管理员 */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('REPORTER')")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        orderService.cancelByReporter(id, request);
        return Result.success();
    }

    /** 确认完成并评价（COMPLETED）→ 通知师傅 */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('REPORTER')")
    public Result<Void> confirm(@PathVariable Long id, @Valid @RequestBody ConfirmRequest request) {
        orderService.confirm(id, request);
        return Result.success();
    }

    /** 催单（PROCESSING，状态不变）→ 通知师傅（限频） */
    @PostMapping("/{id}/remind")
    @PreAuthorize("hasRole('REPORTER')")
    public Result<Void> remind(@PathVariable Long id) {
        orderService.remind(id);
        return Result.success();
    }
}
