package com.repair.dto;

import com.repair.entity.Notification;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 通知视图与未读数 DTO 的基础断言。
 */
class NotificationVOTest {

    @Test
    void fromEntityCopiesFields() {
        Notification notification = new Notification();
        notification.setId(7L);
        notification.setUserId(3L);
        notification.setType("ORDER");
        notification.setTitle("新工单待接单");
        notification.setContent("您被指派维修工单");
        notification.setOrderId(42L);
        notification.setIsRead(0);
        LocalDateTime created = LocalDateTime.of(2026, 10, 21, 9, 30);
        notification.setCreatedAt(created);

        NotificationVO vo = NotificationVO.from(notification);
        assertEquals(7L, vo.getId());
        assertEquals("ORDER", vo.getType());
        assertEquals("新工单待接单", vo.getTitle());
        assertEquals(42L, vo.getOrderId());
        assertEquals(0, vo.getIsRead());
        assertEquals(created, vo.getCreatedAt());
        // 不对外暴露接收人
        assertNull(readField(vo, "userId"));
    }

    @Test
    void unreadCountWrapper() {
        UnreadCountVO vo = new UnreadCountVO(5L);
        assertEquals(5L, vo.getUnreadCount());
    }

    private Object readField(Object bean, String field) {
        try {
            return bean.getClass().getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
