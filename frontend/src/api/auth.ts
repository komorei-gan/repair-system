/**
 * 认证接口（design.md §3.1，三端共用）。
 */
import { http } from './request'
import type { LoginResponse, UserInfo } from '@/types/api'

/** 登录：返回 token + 用户信息 */
export function login(username: string, password: string): Promise<LoginResponse> {
  return http.post<LoginResponse>('/api/auth/login', { username, password })
}

/** 当前登录用户信息 */
export function me(): Promise<UserInfo> {
  return http.get<UserInfo>('/api/auth/me')
}

/** 修改密码（校验原密码，成功后需重新登录） */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return http.put<void>('/api/auth/password', { oldPassword, newPassword })
}
