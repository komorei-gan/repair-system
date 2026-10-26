/**
 * 师傅端工单接口（design.md §3.3）。
 */
import { http } from './request'
import type { OrderVO, PageResult, RepairerStats } from '@/types/api'

/** 师傅任务列表（分页，按状态筛选） */
export function repairerOrders(
  pageNum = 1,
  pageSize = 10,
  status?: string,
): Promise<PageResult<OrderVO>> {
  return http.get<PageResult<OrderVO>>('/api/repairer/orders', {
    pageNum,
    pageSize,
    ...(status ? { status } : {}),
  })
}

/** 接单（ASSIGNED → PROCESSING） */
export function acceptOrder(id: number): Promise<void> {
  return http.put<void>(`/api/repairer/orders/${id}/accept`)
}

/** 提交完工（PROCESSING → COMPLETED），必填维修结果 */
export function completeOrder(id: number, repairResult: string): Promise<void> {
  return http.put<void>(`/api/repairer/orders/${id}/complete`, { repairResult })
}

/** 申请改派（ASSIGNED → REASSIGNING），必填理由，证据图片选填 */
export function reassignRequest(id: number, reason: string, applyImages?: string): Promise<void> {
  return http.put<void>(`/api/repairer/orders/${id}/reassign-request`, {
    reason,
    ...(applyImages ? { applyImages } : {}),
  })
}

/** 申请仲裁（PROCESSING → ARBITRATING），必填理由，证据图片选填 */
export function arbitrationRequest(id: number, reason: string, applyImages?: string): Promise<void> {
  return http.put<void>(`/api/repairer/orders/${id}/arbitration-request`, {
    reason,
    ...(applyImages ? { applyImages } : {}),
  })
}

/** 师傅统计：累计完成单量 / 平均评分 / 待接单 / 维修中 */
export function repairerStats(): Promise<RepairerStats> {
  return http.get<RepairerStats>('/api/repairer/stats')
}
