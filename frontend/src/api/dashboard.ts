/**
 * 管理端数据看板接口（design.md §3.4 GET /api/admin/stats/dashboard）。
 */
import { http } from './request'
import type { OrderStatus } from '@/types/api'

/** 状态分布条目（环形图） */
export interface DashboardStatusItem {
  status: OrderStatus
  name: string
  count: number
  percentage: number
}

/** 趋势条目（折线图） */
export interface DashboardTrendItem {
  /** MM-dd 展示格式 */
  date: string
  newCount: number
  doneCount: number
}

/** 看板统计视图 */
export interface DashboardStatsVO {
  totalOrders: number
  pendingCount: number
  processingCount: number
  completionRate: number
  statusDistribution: DashboardStatusItem[]
  trend: DashboardTrendItem[]
}

/** 首页看板统计：指标卡 + 状态分布 + 近 7 天趋势 */
export function getDashboardStats(): Promise<DashboardStatsVO> {
  return http.get<DashboardStatsVO>('/api/admin/stats/dashboard')
}
