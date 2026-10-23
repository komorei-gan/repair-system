/**
 * uni.request 封装。
 * - baseURL 指向后端 http://localhost:8080
 * - 请求前自动从 uni storage 取 JWT，挂 Authorization: Bearer <token>
 * - 响应统一解包 Result<T>：code===200 返回 data；401 清 token 回登录页；其余 showToast 报错
 */
import type { Result } from '@/types/api'
import { clearToken, getToken } from '@/utils/token'

const BASE_URL = 'http://localhost:8080'

/** 网络层失败：无 statusCode（后端未启动 / 域名不可达） */
function isNetworkError(statusCode: number | undefined): boolean {
  return statusCode === undefined || statusCode === 0
}

/** 跳转登录页（避免在登录页重复跳转） */
function redirectToLogin(): void {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]?.route ?? ''
  if (current !== 'pages/login/login') {
    uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
    uni.reLaunch({ url: '/pages/login/login' })
  }
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
}

/**
 * 发起请求并解包 Result.data。
 * 泛型 T 为业务数据（已解包）。
 */
function request<T>(options: RequestOptions): Promise<T> {
  const { url, method = 'GET', data } = options
  const token = getToken()

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data: data as UniApp.RequestOptions['data'],
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      timeout: 10_000,
      success: (res) => {
        // 网络层失败：后端未启动 / 跨域不可达
        if (isNetworkError(res.statusCode)) {
          uni.showToast({ title: '网络连接失败，请确认后端服务已启动', icon: 'none' })
          reject(new Error('network error'))
          return
        }

        const result = res.data as Result
        // 非统一结构（如文件流）直接透传
        if (result === null || typeof result !== 'object' || !('code' in result)) {
          resolve(res.data as T)
          return
        }

        if (result.code === 200) {
          resolve(result.data as T)
          return
        }

        if (result.code === 401) {
          clearToken()
          redirectToLogin()
          reject(new Error(result.msg || '未登录'))
          return
        }

        uni.showToast({ title: result.msg || '请求失败', icon: 'none' })
        reject(new Error(result.msg || '请求失败'))
      },
      fail: () => {
        uni.showToast({ title: '网络连接失败，请确认后端服务已启动', icon: 'none' })
        reject(new Error('network error'))
      },
    })
  })
}

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    // 拼接 query 参数
    let fullUrl = url
    if (params) {
      const query = Object.entries(params)
        .filter(([, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join('&')
      if (query) fullUrl += `?${query}`
    }
    return request<T>({ url: fullUrl, method: 'GET' })
  },
  post<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ url, method: 'POST', data })
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ url, method: 'PUT', data })
  },
  delete<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    let fullUrl = url
    if (params) {
      const query = Object.entries(params)
        .filter(([, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join('&')
      if (query) fullUrl += `?${query}`
    }
    return request<T>({ url: fullUrl, method: 'DELETE' })
  },
}

/**
 * 图片上传（multipart/form-data）。
 * 上传成功后返回后端生成的完整图片 URL（已解包 Result<string>）。
 * 401 时清 token 回登录页；其余错误 showToast 提示。
 */
export function uploadFile(filePath: string): Promise<string> {
  const token = getToken()

  return new Promise<string>((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/api/files/upload`,
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      timeout: 20_000,
      success: (res) => {
        // 网络层失败：后端未启动 / 跨域不可达
        if (isNetworkError(res.statusCode)) {
          uni.showToast({ title: '网络连接失败，请确认后端服务已启动', icon: 'none' })
          reject(new Error('network error'))
          return
        }

        let result: Result | null = null
        try {
          result = JSON.parse(res.data) as Result
        } catch {
          uni.showToast({ title: '上传失败，请重试', icon: 'none' })
          reject(new Error('invalid response'))
          return
        }

        if (result && typeof result === 'object' && result.code === 200) {
          resolve(result.data as string)
          return
        }

        if (result && result.code === 401) {
          clearToken()
          redirectToLogin()
          reject(new Error(result.msg || '未登录'))
          return
        }

        uni.showToast({ title: result?.msg || '上传失败', icon: 'none' })
        reject(new Error(result?.msg || '上传失败'))
      },
      fail: () => {
        uni.showToast({ title: '网络连接失败，请确认后端服务已启动', icon: 'none' })
        reject(new Error('network error'))
      },
    })
  })
}

export default request
