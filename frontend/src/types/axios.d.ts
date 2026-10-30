/**
 * axios 类型扩展：为本项目自定义的请求配置补充类型。
 */
import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /**
     * 抑制响应拦截器的默认错误提示。
     * 用于导出下载等需要自行解析 Blob 错误体、输出精确文案的请求，避免重复弹窗。
     */
    suppressErrorToast?: boolean
  }
}
