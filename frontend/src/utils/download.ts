/**
 * 文件流下载工具：统一处理 axios + responseType:'blob' 的导出下载。
 *
 * 三个必须注意的坑：
 * 1) api/request.ts 的响应拦截器会返回 response.data，所以 await instance.get(...)
 *    拿到的直接就是 Blob，不能再取 .data（否则是 undefined，会导致「假成功」）。
 * 2) 后端业务异常（如后端未重启返回 404、或 500）在 responseType:'blob' 下会被包成 Blob，
 *    其 type 为 application/json / text/plain，必须读取内容并报错，绝不能提示成功。
 * 3) 仅凭 contentType 判断不够可靠，改用「文件头魔数」正向校验：
 *    xlsx 本质是 ZIP，前 4 字节为 50 4B 03 04（"PK"）。
 */
import type { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import instance from '@/api/request'

/** 生成 yyyyMMdd_HHmmss 时间戳，用于拼接下载文件名 */
function timestamp(): string {
  const d = new Date()
  const pad = (n: number): string => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}` +
    `_${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
  )
}

/** 用 FileReader 读取 Blob 文本（兼容性好于 blob.text()） */
function readBlobText(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '')
    reader.onerror = () => reject(reader.error ?? new Error('读取响应内容失败'))
    reader.readAsText(blob, 'utf-8')
  })
}

/**
 * 正向校验是否为真正的 xlsx 文件流。
 * xlsx = ZIP 容器，前 4 字节固定为 "PK\x03\x04"；
 * 兼容极老的 .xls（OLE 头 D0 CF 11 E0），但本项目 EasyExcel 输出 xlsx。
 */
async function isExcelBlob(blob: Blob): Promise<boolean> {
  if (blob.size < 4) {
    return false
  }
  try {
    const head = new Uint8Array(await blob.slice(0, 4).arrayBuffer())
    const isZip = head[0] === 0x50 && head[1] === 0x4b && head[2] === 0x03 && head[3] === 0x04
    const isOle = head[0] === 0xd0 && head[1] === 0xcf && head[2] === 0x11 && head[3] === 0xe0
    return isZip || isOle
  } catch {
    return false
  }
}

/** 从错误 Blob 中提取后端返回的提示文案 */
async function extractErrorMessage(blob: Blob): Promise<string> {
  let text = ''
  try {
    text = await readBlobText(blob)
  } catch {
    return '导出失败，请稍后重试'
  }
  if (!text.trim()) {
    return '导出失败，请稍后重试'
  }
  try {
    const parsed = JSON.parse(text) as { msg?: string; message?: string }
    return parsed.msg || parsed.message || '导出失败，请稍后重试'
  } catch {
    // 非 JSON（如 HTML 错误页 / 纯文本）：截断后作为提示
    return text.replace(/<[^>]+>/g, ' ').trim().slice(0, 200) || '导出失败，请稍后重试'
  }
}

/** 解析 HTTP 异常（响应体被 responseType:'blob' 包成 Blob）并提示 */
async function notifyHttpError(error: unknown): Promise<void> {
  const axiosError = error as AxiosError
  const status = axiosError?.response?.status
  // 401 由拦截器统一处理并跳转登录，这里不重复提示
  if (status === 401) {
    return
  }
  const data = axiosError?.response?.data
  if (data instanceof Blob) {
    ElMessage.error(await extractErrorMessage(data))
    return
  }
  const body = data as { msg?: string } | undefined
  ElMessage.error(body?.msg || axiosError?.message || '导出失败，请检查后端服务是否已启动')
}

/**
 * 发起导出下载请求。只有拿到真正的 Excel 文件流才会触发下载；
 * 任何失败都会 ElMessage.error 并 throw，调用方不得再提示成功。
 * @param url      导出接口路径
 * @param params   查询参数（筛选条件）
 * @param filename 下载文件名（不含扩展名与时间戳，会自动追加 _yyyyMMdd_HHmmss.xlsx）
 */
export async function downloadExcel(
  url: string,
  params?: object,
  filename = '导出文件',
): Promise<void> {
  let blob: Blob
  try {
    // 拦截器已解包，拿到的就是 Blob（不是 AxiosResponse），不能再取 .data
    blob = (await instance.get(url, {
      params,
      responseType: 'blob',
      // 导出全量数据，耗时可能超过默认 10s，放宽到 60s
      timeout: 60_000,
      // 自定义标记：让拦截器不弹默认错误提示，由本函数统一输出更精确的中文提示
      suppressErrorToast: true,
    })) as unknown as Blob
  } catch (error) {
    await notifyHttpError(error)
    throw error
  }

  // 1) 非 Blob：响应格式异常
  if (!(blob instanceof Blob)) {
    ElMessage.error('导出失败：响应格式异常')
    throw new Error('导出失败：响应格式异常')
  }

  // 2) 空文件
  if (blob.size === 0) {
    ElMessage.error('导出失败：文件内容为空')
    throw new Error('导出失败：文件内容为空')
  }

  // 3) 正向校验文件头：不是 Excel（说明后端返回了 JSON/文本错误体）→ 读取内容并报错
  if (!(await isExcelBlob(blob))) {
    const message = await extractErrorMessage(blob)
    ElMessage.error(message)
    throw new Error(message)
  }

  // 4) 真正的 Excel 文件流：触发浏览器下载
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = `${filename}_${timestamp()}.xlsx`
  a.style.display = 'none'
  // 必须先挂载到 DOM 再 click（部分浏览器对游离节点不触发下载）
  document.body.appendChild(a)
  a.click()
  // 点击后立即移除节点；objectURL 延迟释放，避免 Firefox 下载未开始就被回收
  document.body.removeChild(a)
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
}
