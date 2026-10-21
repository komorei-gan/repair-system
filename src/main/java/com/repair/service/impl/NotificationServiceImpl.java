package com.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.common.result.PageResult;
import com.repair.dto.NotificationVO;
import com.repair.dto.UnreadCountVO;
import com.repair.entity.Notification;
import com.repair.mapper.NotificationMapper;
import com.repair.security.LoginUser;
import com.repair.security.SecurityUtil;
import com.repair.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public PageResult<NotificationVO> myNotifications(Page<?> page, Integer isRead) {
        LoginUser current = SecurityUtil.getCurrentUser();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, current.getUserId())
                .eq(isRead != null, Notification::getIsRead, isRead)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId);
        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return PageResult.from(result.convert(NotificationVO::from));
    }

    @Override
    public UnreadCountVO unreadCount() {
        LoginUser current = SecurityUtil.getCurrentUser();
        return new UnreadCountVO(notificationMapper.countUnread(current.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId) {
        LoginUser current = SecurityUtil.getCurrentUser();
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 只能标记自己的通知，防止越权改他人数据
        if (!Objects.equals(notification.getUserId(), current.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        markReadInternal(notificationId, current.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        LoginUser current = SecurityUtil.getCurrentUser();
        notificationMapper.update(null, new UpdateWrapper<Notification>()
                .eq("user_id", current.getUserId())
                .eq("is_read", 0)
                .set("is_read", 1)
                .set("read_time", LocalDateTime.now()));
    }

    /** 条件更新（is_read=0 才更新），重复标记已读时影响 0 行，按幂等处理 */
    private void markReadInternal(Long notificationId, Long userId) {
        notificationMapper.update(null, new UpdateWrapper<Notification>()
                .eq("id", notificationId)
                .eq("user_id", userId)
                .eq("is_read", 0)
                .set("is_read", 1)
                .set("read_time", LocalDateTime.now()));
    }
}
