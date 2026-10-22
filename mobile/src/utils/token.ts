/**
 * JWT Token 存取（uni storage）。
 * 后端约定：Authorization: Bearer <token>。
 */
const TOKEN_KEY = 'repair_token'
const USER_KEY = 'repair_user'

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export function getCachedUser<T>(): T | null {
  const raw = uni.getStorageSync(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function setCachedUser<T>(user: T): void {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}
