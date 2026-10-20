package com.repair.service.impl;

import com.repair.entity.Notification;
import com.repair.mapper.NotificationMapper;
import com.repair.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 通知写入实现：仅做字段裁剪与插入，事务语义由调用方（工单状态流转）决定。
 */
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    /** 与 notification 表列宽一致，防御性截断 */
    private static final int TITLE_MAX = 100;

    private static final int CONTENT_MAX = 500;

    private final NotificationMapper notificationMapper;

    @Override
    public void notify(Long userId, String type, String title, String content, Long orderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(cut(title, TITLE_MAX));
        notification.setContent(cut(content, CONTENT_MAX));
        notification.setOrderId(orderId);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    @Override
    public void notifyUsers(Collection<Long> userIds, String type, String title, String content, Long orderId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            notify(userId, type, title, content, orderId);
        }
    }

    private String cut(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
