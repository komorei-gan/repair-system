/**
 * 状态/类型展示映射 —— 颜色取值与 Web 端 design-web.html 完全一致。
 */
import type { OrderStatus } from '@/types/api'

/** 工单状态：中文名 + 标签配色 */
export const ORDER_STATUS_MAP: Record<OrderStatus, { label: string; type: string }> = {
  PENDING: { label: '待受理', type: 'primary' },
  ASSIGNED: { label: '已派单', type: 'lightblue' },
  PROCESSING: { label: '维修中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CONFIRMED: { label: '已确认', type: 'success' },
  CANCELLED: { label: '已取消', type: 'default' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

export function orderStatusLabel(status: OrderStatus): string {
  return ORDER_STATUS_MAP[status]?.label ?? status
}

export function orderStatusTagType(status: OrderStatus): string {
  return ORDER_STATUS_MAP[status]?.type ?? 'default'
}

/** 状态浅色胶囊样式类（浅底 + 深字，与 Web 端视觉一致） */
const ORDER_STATUS_CLS: Record<OrderStatus, string> = {
  PENDING: 'tag-pending',
  ASSIGNED: 'tag-assigned',
  PROCESSING: 'tag-processing',
  COMPLETED: 'tag-completed',
  CONFIRMED: 'tag-confirmed',
  CANCELLED: 'tag-cancelled',
  REJECTED: 'tag-rejected',
}

export function orderStatusCls(status: OrderStatus): string {
  return ORDER_STATUS_CLS[status] ?? 'tag-cancelled'
}

/** 列表筛选 tab 配置：空串表示「全部」 */
export const STATUS_FILTERS: { label: string; value: OrderStatus | '' }[] = [
  { label: '全部', value: '' },
  { label: '待受理', value: 'PENDING' },
  { label: '已派单', value: 'ASSIGNED' },
  { label: '维修中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已驳回', value: 'REJECTED' },
]
