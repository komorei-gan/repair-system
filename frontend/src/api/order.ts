/**
 * 管理端工单接口（design.md §3.4）。
 */
import { http } from './request'
import type { OrderDetailVO, OrderVO, PageResult } from '@/types/api'

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

/** 工单详情（含操作日志；管理员可访问） */
export function orderDetail(id: number): Promise<OrderDetailVO> {
  return http.get<OrderDetailVO>(`/api/orders/${id}`)
}

/** 管理员派单（PENDING → ASSIGNED） */
export function assignOrder(id: number, repairerId: number): Promise<void> {
  return http.put<void>(`/api/admin/orders/${id}/assign`, { repairerId })
}

/** 管理员改派（ASSIGNED/PROCESSING → ASSIGNED，换师傅） */
export function reassignOrder(id: number, repairerId: number): Promise<void> {
  return http.put<void>(`/api/admin/orders/${id}/reassign`, { repairerId })
}

/** 管理员删除工单（仅 PENDING/CANCELLED 可逻辑删除） */
export function deleteOrder(id: number): Promise<void> {
  return http.delete<void>(`/api/admin/orders/${id}`)
}

/** 待受理（PENDING）工单数，供左侧菜单角标展示 */
export function pendingCount(): Promise<number> {
  return http.get<number>('/api/admin/orders/pending-count')
}

/** 待审批工单列表参数（type: REASSIGN / ARBITRATE / 空=全部） */
export interface AuditingQueryParams {
  pageNum?: number
  pageSize?: number
  /** REASSIGN 申请改派 / ARBITRATE 申请仲裁 */
  type?: string
}

/** 待审批工单分页查询（申请改派 / 申请仲裁） */
export function auditingOrders(params: AuditingQueryParams): Promise<PageResult<OrderVO>> {
  return http.get<PageResult<OrderVO>>('/api/admin/orders/auditing', {
    ...params,
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 10,
  })
}

/** 待审批（REASSIGNING / ARBITRATING）工单数，供左侧仲裁中心角标展示 */
export function auditingCount(): Promise<number> {
  return http.get<number>('/api/admin/orders/auditing-count')
}

/** 统一审批参数（approve + type + remark） */
export interface AuditPayload {
  approve: boolean
  type: 'REASSIGN' | 'ARBITRATE'
  remark?: string
}

/** 管理员统一审批（改派 / 仲裁） */
export function auditOrder(id: number, payload: AuditPayload): Promise<void> {
  return http.put<void>(`/api/admin/orders/${id}/audit`, payload)
}
