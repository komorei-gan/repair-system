/**
 * 后端接口类型定义（与 design.md / 后端 DTO 一一对应）。
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
  /** 1 启用 / 0 禁用（管理端用户列表展示） */
  status?: number
  /** 密码重置流转标记：0 未申请 / 1 已申请待批准 / 2 已批准（管理端用户列表展示） */
  resetApproved?: number
  /** 创建时间（管理端用户列表展示） */
  createdAt?: string
  /** 信誉分（报修人，管理端「评价/信誉」列展示） */
  creditScore?: number
  /** 师傅平均评分（REPAIRER，管理端「评价/信誉」列展示） */
  rating?: number | null
}

/** 审核状态：0 待审核 / 1 已通过 / 2 已拒绝 */
export type AuditStatus = 0 | 1 | 2

/** 师傅审核列表视图 */
export interface RepairerAuditVO {
  id: number
  username: string
  realName: string | null
  phone: string | null
  /** 申请理由（师傅注册时填写） */
  reason: string | null
  auditStatus: AuditStatus
  createdAt: string | null
}

/** 登录响应：token + 用户信息 */
export interface LoginResponse {
  token: string
  user: UserInfo
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
  /** 师傅申请理由（申请改派 / 申请仲裁） */
  applyReason?: string | null
  /** 师傅申请证据图片 URL 的 JSON 数组 */
  applyImages?: string | null
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
  | 'REASSIGNING'
  | 'ARBITRATING'

/** 通知视图 */
export interface NotificationVO {
  id: number
  /** ORDER 工单通知 / SYSTEM 系统通知 */
  type: 'ORDER' | 'SYSTEM'
  title: string
  content: string
  orderId: number | null
  isRead: 0 | 1
  readTime: string | null
  createdAt: string
}

/** 未读数响应 */
export interface UnreadCountVO {
  unreadCount: number
}

/** 故障分类 */
export interface Category {
  id: number
  name: string
  sort: number
  status: number
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
  /** 师傅申请理由（申请改派 / 申请仲裁） */
  applyReason?: string | null
  /** 师傅申请证据图片 URL 的 JSON 数组 */
  applyImages?: string | null
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
