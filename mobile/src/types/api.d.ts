/**
 * 后端接口类型定义（与 design.md / 后端 DTO 一一对应，与 Web 端保持一致）。
 */

/** 统一返回结构：code 与 HTTP 状态一致，200 成功 */
export interface Result<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 统一分页返回结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 用户信息视图（不含 password） */
export interface UserInfo {
  id: number
  username: string
  realName: string | null
  phone: string | null
  /** 角色：ADMIN 管理员 / REPAIRER 师傅 / REPORTER 报修人 */
  role: 'ADMIN' | 'REPAIRER' | 'REPORTER'
  avatar: string | null
}

/** 登录响应：token + 用户信息 */
export interface LoginResponse {
  token: string
  user: UserInfo
}

/** 注册请求（对应后端 RegisterRequest，四项必填） */
export interface RegisterPayload {
  username: string
  password: string
  realName: string
  phone: string
}

/** 工单列表视图（列表页不含 description/images 大字段） */
export interface OrderVO {
  id: number
  orderNo: string
  reporterId: number
  reporterName: string
  categoryId: number
  categoryName: string
  title: string
  address: string
  contactPhone: string
  status: OrderStatus
  repairerId: number | null
  repairerName: string | null
  assignTime: string | null
  acceptTime: string | null
  finishTime: string | null
  confirmTime: string | null
  rating: number | null
  remindCount: number
  createdAt: string
}

/** 工单状态（design.md §1.1） */
export type OrderStatus =
  | 'PENDING'
  | 'ASSIGNED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'REJECTED'

/** 故障分类 */
export interface Category {
  id: number
  name: string
  sort: number
  status: number
  createdAt: string
}

/** 提交报修请求（对应后端 CreateOrderRequest） */
export interface CreateOrderPayload {
  categoryId: number
  title: string
  description?: string
  images?: string
  address?: string
  contactPhone?: string
}

/** 修改报修请求（对应后端 UpdateOrderRequest，不含 images） */
export interface UpdateOrderPayload {
  categoryId: number
  title: string
  description?: string
  address?: string
  contactPhone?: string
}

/** 工单操作日志 */
export interface OrderLogVO {
  id: number
  operatorId: number | null
  operatorName: string | null
  operatorRole: string | null
  action: string
  fromStatus: OrderStatus | null
  toStatus: OrderStatus | null
  remark: string | null
  createdAt: string
}

/** 工单详情视图（含 description/images 大字段与操作日志） */
export interface OrderDetailVO {
  id: number
  orderNo: string
  reporterId: number
  reporterName: string | null
  categoryId: number | null
  categoryName: string | null
  title: string
  description: string | null
  images: string | null
  address: string | null
  contactPhone: string | null
  status: OrderStatus
  repairerId: number | null
  repairerName: string | null
  repairerPhone: string | null
  assignTime: string | null
  acceptTime: string | null
  finishTime: string | null
  confirmTime: string | null
  rejectReason: string | null
  cancelReason: string | null
  repairResult: string | null
  rating: number | null
  ratingComment: string | null
  remindCount: number
  createdAt: string
  logs: OrderLogVO[]
}

