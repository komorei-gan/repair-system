/**
 * JWT Token 存取（localStorage）。
 * 后端约定：Authorization: Bearer <token>。
 */
const TOKEN_KEY = 'repair_token'
const USER_KEY = 'repair_user'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getCachedUser<T>(): T | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function setCachedUser<T>(user: T): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}
