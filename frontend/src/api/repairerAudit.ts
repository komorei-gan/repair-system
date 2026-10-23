/**
 * 管理端师傅审核接口。
 * - GET /api/admin/repairers/audit?auditStatus=&keyword=&pageNum=&pageSize= ：师傅审核分页查询
 * - PUT /api/admin/repairers/audit/{id}                         ：通过/拒绝师傅
 */
import { http } from './request'
import type { AuditStatus, PageResult, RepairerAuditVO } from '@/types/api'

/** 师傅审核分页查询参数 */
export interface RepairerAuditQueryParams {
  pageNum?: number
  pageSize?: number
  /** 0 待审核 / 1 已通过 / 2 已拒绝 */
  auditStatus?: AuditStatus
  keyword?: string
}

/** 师傅审核分页查询 */
export function pageRepairerAudit(params: RepairerAuditQueryParams): Promise<PageResult<RepairerAuditVO>> {
  return http.get<PageResult<RepairerAuditVO>>('/api/admin/repairers/audit', {
    ...params,
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 10,
  })
}

/** 待审核（auditStatus=0）师傅申请数，供左侧菜单角标展示 */
export function pendingRepairerAuditCount(): Promise<number> {
  return http.get<number>('/api/admin/repairers/audit/pending-count')
}

/** 通过/拒绝师傅（auditStatus: 1 通过 / 2 拒绝） */
export function updateRepairerAudit(id: number, auditStatus: 1 | 2): Promise<void> {
  return http.put<void>(`/api/admin/repairers/audit/${id}`, { auditStatus })
}
