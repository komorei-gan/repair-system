/**
 * Axios 请求实例封装。
 * - baseURL 指向后端 http://localhost:8080（可用 .env 覆盖）
 * - 请求拦截器：自动从 localStorage 取 JWT，挂 Authorization: Bearer <token>
 * - 响应拦截器：code===200 直接返回业务数据；401 清 token 回登录页；其余统一 ElMessage 报错
 */
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'
import { clearToken, getToken } from '@/utils/token'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

/* ---------- 请求拦截器：自动带 JWT ---------- */
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/* ---------- 响应拦截器：解包 Result ---------- */
instance.interceptors.response.use(
  (response) => {
    const result = response.data as Result
    // 非统一结构（如文件流）直接透传
    if (result === null || typeof result !== 'object' || !('code' in result)) {
      return response.data
    }
    if (result.code === 200) {
      return result.data
    }
    if (result.code === 401) {
      clearToken()
      ElMessage.error(result.msg || '登录已过期，请重新登录')
      // 避免在登录页重复跳转
      if (!window.location.pathname.endsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
      return Promise.reject(new Error(result.msg || '未登录'))
    }
    // 调用方自行处理错误提示（如导出下载需要解析 Blob 后输出精确文案）
    if (!response.config.suppressErrorToast) {
      ElMessage.error(result.msg || '请求失败')
    }
    return Promise.reject(new Error(result.msg || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearToken()
      if (!window.location.pathname.endsWith('/login')) {
        ElMessage.error('登录已过期，请重新登录')
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    } else if (error?.config?.suppressErrorToast) {
      // 交给调用方统一提示，避免与下载工具重复弹窗
    } else if (status) {
      const msg = error?.response?.data?.msg ?? error?.message ?? '网络请求失败'
      ElMessage.error(msg)
    } else {
      ElMessage.error('网络连接失败，请确认后端服务已启动')
    }
    return Promise.reject(error)
  },
)

/**
 * 泛型请求方法：返回值已解包（直接拿 data）。
 * 分页接口返回 PageResult<T>；其余返回业务对象。
 */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  // 响应拦截器已解包 Result.data，此处断言返回类型
  return instance.request<T>(config) as Promise<T>
}

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ method: 'GET', url, params })
  },
  post<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'POST', url, data })
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'PUT', url, data })
  },
  delete<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ method: 'DELETE', url, params })
  },
}

export default instance
