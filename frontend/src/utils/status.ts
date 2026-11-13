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
  REASSIGNING: { label: '申请改派中', cls: 'pill reassigning' },
  ARBITRATING: { label: '仲裁中', cls: 'pill arbitrating' },
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
  REASSIGNING: '#2F54EB',
  ARBITRATING: '#FAAD14',
}

/** 通知类型映射 */
export const NOTIFICATION_TYPE_META: Record<
  string,
  { label: string; cls: string }
> = {
  ORDER: { label: '工单通知', cls: 'notif-ico blue' },
  SYSTEM: { label: '系统通知', cls: 'notif-ico slate' },
}

/**
 * 工单操作日志动作映射字典（后端英文动作码 → 中文）。
 * 后端 order_log.action 全集：SUBMIT / ASSIGN / ACCEPT / FINISH / CONFIRM /
 * CANCEL / UPDATE / ARBITRATE_REQUEST / REASSIGN_REQUEST / AUDIT / REJECT / REMIND。
 */
export const ORDER_LOG_ACTION_MAP: Record<string, string> = {
  SUBMIT: '提交报修',
  ASSIGN: '派单',
  ACCEPT: '接单',
  FINISH: '提交完工',
  CONFIRM: '确认评价',
  CANCEL: '取消工单',
  UPDATE: '修改工单',
  ARBITRATE_REQUEST: '申请仲裁',
  REASSIGN_REQUEST: '申请改派',
  AUDIT: '仲裁/改派审批',
  REJECT: '驳回/拒单',
  REMIND: '催单',
  DELETE: '删除工单',
}

/** 动作码 → 中文（未收录的动作码原样返回，避免出现 undefined） */
export function orderLogActionLabel(action: string): string {
  return ORDER_LOG_ACTION_MAP[action] ?? action
}
