/**
 * 管理端用户接口（design.md §3.4）。
 * - GET /api/admin/users?role=REPAIRER        ：按角色查询启用用户（扁平列表，供派单弹窗）
 * - GET /api/admin/users?pageNum=&pageSize=&keyword=&role= ：用户管理分页查询
 * - PUT /api/admin/users/{id}/status          ：启用/禁用用户
 */
import { http } from './request'
import type { PageResult, UserInfo } from '@/types/api'

/** 用户分页查询参数 */
export interface UserQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  role?: string
}

/** 按角色查询启用中的用户（默认 REPAIRER 维修师傅，供派单弹窗） */
export function listUsersByRole(role: string = 'REPAIRER'): Promise<UserInfo[]> {
  return http.get<UserInfo[]>('/api/admin/users', { role })
}

/** 用户管理分页查询（姓名/账号关键字 + 角色筛选） */
export function pageUsers(params: UserQueryParams): Promise<PageResult<UserInfo>> {
  return http.get<PageResult<UserInfo>>('/api/admin/users', {
    ...params,
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 10,
  })
}

/** 启用/禁用用户（status: 1 启用 / 0 禁用） */
export function updateUserStatus(id: number, status: 0 | 1): Promise<void> {
  return http.put<void>(`/api/admin/users/${id}/status`, { status })
}

/** 批准密码重置（reset_approved 置 1，并通知该用户） */
export function approveReset(id: number): Promise<void> {
  return http.put<void>(`/api/admin/users/${id}/approve-reset`)
}
