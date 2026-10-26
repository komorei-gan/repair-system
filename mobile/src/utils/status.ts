/**
 * 状态/类型展示映射 —— 颜色取值与 Web 端 design-web.html 完全一致。
 *
 * 状态隔离说明：
 * - 师傅端（repairer）使用真实状态文案：REASSIGNING=「申请改派中」、ARBITRATING=「仲裁中」。
 * - 用户端（reporter）使用「状态欺骗」文案：REASSIGNING=「已派单」、ARBITRATING=「维修中」，
 *   避免向报修人暴露师傅申请改派 / 申请仲裁的内部流程，降低焦虑。
 * - 两套映射各自独立，避免一端改动污染另一端。
 */
import type { OrderStatus } from '@/types/api'

/** 状态视角：reporter 报修人（用户端）/ repairer 师傅端 */
export type StatusScope = 'reporter' | 'repairer'

/** 工单状态：中文名 + 标签配色（师傅端真实文案，作为默认值） */
export const REPAIRER_STATUS_MAP: Record<OrderStatus, { label: string; type: string }> = {
  PENDING: { label: '待受理', type: 'primary' },
  ASSIGNED: { label: '已派单', type: 'lightblue' },
  PROCESSING: { label: '维修中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CONFIRMED: { label: '已确认', type: 'success' },
  CANCELLED: { label: '已取消', type: 'default' },
  REJECTED: { label: '已驳回', type: 'danger' },
  REASSIGNING: { label: '申请改派中', type: 'lightblue' },
  ARBITRATING: { label: '仲裁中', type: 'warning' },
}

/** 用户端状态文案映射：对 REASSIGNING / ARBITRATING 做视觉欺骗，其余复用师傅端文案 */
export const REPORTER_STATUS_MAP: Record<OrderStatus, { label: string; type: string }> = {
  ...REPAIRER_STATUS_MAP,
  REASSIGNING: { label: '已派单', type: 'lightblue' },
  ARBITRATING: { label: '维修中', type: 'warning' },
}

/** 状态浅色胶囊样式类（浅底 + 深字，与 Web 端视觉一致）——师傅端真实配色 */
const REPAIRER_STATUS_CLS: Record<OrderStatus, string> = {
  PENDING: 'tag-pending',
  ASSIGNED: 'tag-assigned',
  PROCESSING: 'tag-processing',
  COMPLETED: 'tag-completed',
  CONFIRMED: 'tag-confirmed',
  CANCELLED: 'tag-cancelled',
  REJECTED: 'tag-rejected',
  REASSIGNING: 'tag-reassigning',
  ARBITRATING: 'tag-arbitrating',
}

/** 用户端胶囊配色：跟随欺骗文案，REASSIGNING 用「已派单」蓝、ARBITRATING 用「维修中」橙 */
const REPORTER_STATUS_CLS: Record<OrderStatus, string> = {
  ...REPAIRER_STATUS_CLS,
  REASSIGNING: 'tag-assigned',
  ARBITRATING: 'tag-processing',
}

/** 兼容旧导出：默认视角为师傅端 */
export const ORDER_STATUS_MAP = REPAIRER_STATUS_MAP
export const ORDER_STATUS_CLS = REPAIRER_STATUS_CLS

/** 按视角取状态中文名 */
export function orderStatusLabel(status: OrderStatus, scope: StatusScope = 'repairer'): string {
  const map = scope === 'reporter' ? REPORTER_STATUS_MAP : REPAIRER_STATUS_MAP
  return map[status]?.label ?? status
}

/** 按视角取标签配色类型 */
export function orderStatusTagType(status: OrderStatus, scope: StatusScope = 'repairer'): string {
  const map = scope === 'reporter' ? REPORTER_STATUS_MAP : REPAIRER_STATUS_MAP
  return map[status]?.type ?? 'default'
}

/** 按视角取胶囊样式类 */
export function orderStatusCls(status: OrderStatus, scope: StatusScope = 'repairer'): string {
  const cls = scope === 'reporter' ? REPORTER_STATUS_CLS : REPAIRER_STATUS_CLS
  return cls[status] ?? 'tag-cancelled'
}

/** 用户端列表筛选 Tab 配置：value 为逗号分隔状态串或 ''（空串表示「全部」、不传 status） */
export const REPORTER_STATUS_FILTERS: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '处理中', value: 'PENDING,ASSIGNED,PROCESSING,REASSIGNING,ARBITRATING' },
  { label: '待评价', value: 'COMPLETED' },
  { label: '已结束', value: 'CONFIRMED,CANCELLED,REJECTED' },
]

/** 师傅端列表筛选 Tab 配置：value 为逗号分隔状态串或 ''（空串表示「全部」） */
export const REPAIRER_STATUS_FILTERS: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '待接单', value: 'ASSIGNED' },
  { label: '维修中', value: 'PROCESSING' },
  { label: '申请中', value: 'REASSIGNING,ARBITRATING' },
  { label: '已完成', value: 'COMPLETED' },
]
