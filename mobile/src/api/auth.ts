/**
 * 认证接口（design.md §3.1，三端共用）。
 */
import { http } from './request'
import type { LoginResponse } from '@/types/api'

/** 登录：返回 token + 用户信息 */
export function login(username: string, password: string): Promise<LoginResponse> {
  return http.post<LoginResponse>('/api/auth/login', { username, password })
}
