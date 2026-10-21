/**
 * 管理端工单接口（design.md §3.4，本阶段仅列表联调用，其余后续接入）。
 */
import { http } from './request'
import type { OrderVO, PageResult } from '@/types/api'

/** 工单分页查询参数（对应后端 OrderQuery） */
export interface OrderQueryParams {
  pageNum?: number
  pageSize?: number
  status?: string
  categoryId?: number
  keyword?: string
  startTime?: string
  endTime?: string
}

/** 工单分页查询（状态/分类/时间/关键字） */
export function adminOrders(params: OrderQueryParams): Promise<PageResult<OrderVO>> {
  return http.get<PageResult<OrderVO>>('/api/admin/orders', { ...params })
}
