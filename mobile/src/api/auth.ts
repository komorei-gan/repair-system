/**
 * 认证接口（design.md §3.1，三端共用）。
 */
import { http } from './request'
import type { LoginResponse, RegisterPayload } from '@/types/api'

/** 登录：返回 token + 用户信息 */
export function login(username: string, password: string): Promise<LoginResponse> {
  return http.post<LoginResponse>('/api/auth/login', { username, password })
}

/** 报修人注册（role 固定 REPORTER） */
export function register(payload: RegisterPayload): Promise<void> {
  return http.post<void>('/api/auth/register', payload)
}
