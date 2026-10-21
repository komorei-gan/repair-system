/**
 * 状态/类型展示映射 —— 颜色取值与 design-web.html 完全一致。
 */
import type { OrderStatus } from '@/types/api'

/** 工单状态：中文名 + 胶囊配色（背景/文字/圆点） */
export const ORDER_STATUS_MAP: Record<OrderStatus, { label: string; cls: string }> = {
  PENDING: { label: '待受理', cls: 'pill pending' },
  ASSIGNED: { label: '已派单', cls: 'pill assigned' },
  PROCESSING: { label: '维修中', cls: 'pill processing' },
  COMPLETED: { label: '已完成', cls: 'pill completed' },
  CONFIRMED: { label: '已确认', cls: 'pill confirmed' },
  CANCELLED: { label: '已取消', cls: 'pill cancelled' },
  REJECTED: { label: '已驳回', cls: 'pill rejected' },
}

export function orderStatusLabel(status: OrderStatus): string {
  return ORDER_STATUS_MAP[status]?.label ?? status
}

export function orderStatusCls(status: OrderStatus): string {
  return ORDER_STATUS_MAP[status]?.cls ?? 'pill assigned'
}

/** 状态分布环形图配色（与视觉稿图二一致） */
export const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: '#1890FF',
  ASSIGNED: '#7FB9F2',
  PROCESSING: '#FAAD14',
  COMPLETED: '#95DE64',
  CONFIRMED: '#52C41A',
  CANCELLED: '#C9D6E5',
  REJECTED: '#8C8C8C',
}

/** 通知类型映射 */
export const NOTIFICATION_TYPE_META: Record<
  string,
  { label: string; cls: string }
> = {
  ORDER: { label: '工单通知', cls: 'notif-ico blue' },
  SYSTEM: { label: '系统通知', cls: 'notif-ico slate' },
}
