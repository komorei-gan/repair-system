/**
 * 报修端工单接口（design.md §3.2）。
 */
import { http } from './request'
import type { CreateOrderPayload, OrderDetailVO, OrderVO, PageResult, UpdateOrderPayload } from '@/types/api'

/** 我的报修列表（分页，按状态筛选） */
export function myOrders(
  pageNum = 1,
  pageSize = 10,
  status?: string,
): Promise<PageResult<OrderVO>> {
  return http.get<PageResult<OrderVO>>('/api/orders/my', {
    pageNum,
    pageSize,
    ...(status ? { status } : {}),
  })
}

/** 提交报修工单 → 通知管理员 */
export function createOrder(payload: CreateOrderPayload): Promise<number> {
  return http.post<number>('/api/orders', payload)
}

/** 工单详情（含操作日志；三端均可访问，归属校验在服务端） */
export function orderDetail(id: number): Promise<OrderDetailVO> {
  return http.get<OrderDetailVO>(`/api/orders/${id}`)
}

/** 修改工单（仅 PENDING，状态不变）→ 记日志并通知管理员 */
export function updateOrder(id: number, payload: UpdateOrderPayload): Promise<void> {
  return http.put<void>(`/api/orders/${id}`, payload)
}

/** 撤回工单（PENDING → CANCELLED）→ 通知管理员 */
export function cancelOrder(id: number, reason: string): Promise<void> {
  return http.put<void>(`/api/orders/${id}/cancel`, { reason })
}

/** 确认完成并评价（COMPLETED → CONFIRMED）→ 通知师傅 */
export function confirmOrder(id: number, payload: { rating: number; ratingComment?: string }): Promise<void> {
  return http.post<void>(`/api/orders/${id}/confirm`, payload)
}
