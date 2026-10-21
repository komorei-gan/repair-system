package com.repair.dto;

import com.repair.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知列表视图（design.md §3.5 GET /api/notifications）。
 */
@Data
@Builder
public class NotificationVO {

    private Long id;

    /** 通知类型：ORDER 工单通知 / SYSTEM 系统通知 */
    private String type;

    private String title;

    private String content;

    /** 关联工单 id，点击可跳转 */
    private Long orderId;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    private LocalDateTime readTime;

    private LocalDateTime createdAt;

    public static NotificationVO from(Notification notification) {
        return NotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .orderId(notification.getOrderId())
                .isRead(notification.getIsRead())
                .readTime(notification.getReadTime())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
