/**
 * 分页 pageSize 持久化工具。
 * - 各列表页切换 pageSize 后写入 localStorage，再次进入时恢复上次选择。
 * - 仅接受 [10, 20, 50] 中的合法值，其余一律回落默认 10。
 * - pageNum 不持久化：组件每次挂载自然重置为 1。
 */

/** 各分页页面的 localStorage key 集中定义，避免魔法字符串散落各处 */
export const PAGE_SIZE_KEY = {
  /** 工单列表 */
  orders: 'repair_pageSize_orders',
  /** 派单中心 */
  dispatch: 'repair_pageSize_dispatch',
  /** 仲裁中心 */
  arbitration: 'repair_pageSize_arbitration',
  /** 用户管理 */
  users: 'repair_pageSize_users',
  /** 师傅审核 */
  repairerAudit: 'repair_pageSize_repairerAudit',
  /** 通知中心 */
  notice: 'repair_pageSize_notice',
} as const

/** 允许的 pageSize 取值（与各页 el-pagination 的 :page-sizes 保持一致） */
const VALID_SIZES = [10, 20, 50]

/**
 * 从 localStorage 读取 pageSize。
 * 缺失、非法或非数字时回落默认 10。
 */
export function loadPageSize(key: string): number {
  try {
    const raw = localStorage.getItem(key)
    if (raw == null) return 10
    const n = Number(raw)
    return VALID_SIZES.includes(n) ? n : 10
  } catch {
    return 10
  }
}

/** 将 pageSize 写入 localStorage（写入失败静默忽略） */
export function savePageSize(key: string, size: number): void {
  try {
    localStorage.setItem(key, String(size))
  } catch {
    /* 隐私模式 / 存储受限等场景静默忽略 */
  }
}
