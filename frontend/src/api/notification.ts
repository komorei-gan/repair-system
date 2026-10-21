/**
 * 通知接口（design.md §3.5，三端通用）。
 */
import { http } from './request'
import type { NotificationVO, PageResult, UnreadCountVO } from '@/types/api'

/** 我的通知列表（分页，按时间倒序） */
export function listNotifications(
  pageNum = 1,
  pageSize = 10,
  isRead?: 0 | 1,
): Promise<PageResult<NotificationVO>> {
  return http.get<PageResult<NotificationVO>>('/api/notifications', {
    pageNum,
    pageSize,
    ...(isRead !== undefined ? { isRead } : {}),
  })
}

/** 未读数（30s 轮询专用，轻量） */
export function unreadCount(): Promise<UnreadCountVO> {
  return http.get<UnreadCountVO>('/api/notifications/unread-count')
}

/** 标记单条已读（幂等） */
export function markRead(id: number): Promise<void> {
  return http.put<void>(`/api/notifications/${id}/read`)
}

/** 全部标记已读 */
export function markAllRead(): Promise<void> {
  return http.put<void>('/api/notifications/read-all')
}
