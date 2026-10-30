package com.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.common.result.Result;
import com.repair.dto.NotificationVO;
import com.repair.dto.UnreadCountVO;
import com.repair.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知接口（design.md §3.5，三端通用）。
 * 所有接口只操作当前登录用户的通知（userId 从 SecurityContext 取，不接受客户端传入）。
 */
@Tag(name = "通知模块")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    /** 我的通知列表（分页，按时间倒序；可按 isRead 筛选） */
    @GetMapping
    public Result<PageResult<NotificationVO>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer isRead) {
        return Result.success(notificationService.myNotifications(new Page<>(pageNum, pageSize), isRead));
    }

    /** 未读数（轮询专用，轻量，走 idx_user_read 索引） */
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    /** 标记单条已读（幂等） */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    /** 标记单条未读（幂等） */
    @PutMapping("/{id}/unread")
    public Result<Void> markUnread(@PathVariable Long id) {
        notificationService.markUnread(id);
        return Result.success();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.success();
    }
}
