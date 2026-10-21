/**
 * 管理端用户接口（design.md §3.4 GET /api/admin/users）。
 * 当前阶段仅提供「按角色查询启用用户」，供派单弹窗选择维修师傅。
 */
import { http } from './request'
import type { UserInfo } from '@/types/api'

/** 按角色查询启用中的用户（默认 REPAIRER 维修师傅） */
export function listUsersByRole(role: string = 'REPAIRER'): Promise<UserInfo[]> {
  return http.get<UserInfo[]>('/api/admin/users', { role })
}
