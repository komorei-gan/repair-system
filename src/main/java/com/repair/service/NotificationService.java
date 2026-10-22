package com.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.dto.NotificationVO;
import com.repair.dto.UnreadCountVO;

/**
 * 通知读取服务（design.md §3.5）。
 * 所有方法只作用于当前登录用户（从 SecurityContext 取），不接受外部传入 userId，
 * 从接口层面杜绝越权查询他人通知。
 */
public interface NotificationService {

    /** 我的通知列表（分页，按创建时间倒序） */
    PageResult<NotificationVO> myNotifications(Page<?> page, Integer isRead);

    /** 未读数（轮询专用，走 idx_user_read 索引） */
    UnreadCountVO unreadCount();

    /** 标记单条已读（幂等：已读的重复标记直接成功） */
    void markRead(Long notificationId);

    /** 标记单条未读（幂等：未读的重复标记直接成功） */
    void markUnread(Long notificationId);

    /** 全部标记已读 */
    void markAllRead();
}
