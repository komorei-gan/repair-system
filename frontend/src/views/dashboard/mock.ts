/**
 * 数据看板 mock 数据 —— 与 design-web.html 完全一致。
 * TODO(第二阶段)：替换为 GET /api/admin/stats/dashboard 与 GET /api/admin/orders 的真实数据。
 */
import type { OrderStatus } from '@/types/api'

/** 近 7 天工单趋势（视觉稿图一） */
export const TREND_DAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
export const TREND_NEW = [32, 41, 28, 53, 46, 61, 47]
export const TREND_DONE = [24, 33, 26, 39, 42, 50, 44]

/** 指标卡 */
export interface StatItem {
  label: string
  value: string
  unit?: string
  pct?: string
  deltaText: string
  note: string
  icon: string
  iconCls: string
  featured?: boolean
  /** delta 类型：up 橙 / down 绿 / flat 蓝 */
  deltaCls: 'up' | 'down' | 'flat'
  progress?: number
}

export const STAT_ITEMS: StatItem[] = [
  {
    label: '待受理工单',
    value: '18',
    unit: '单',
    deltaText: '6',
    note: '较昨日 · 平均响应 12 分钟',
    icon: 'i-inbox',
    iconCls: 'blue',
    featured: true,
    deltaCls: 'up',
  },
  {
    label: '维修中',
    value: '31',
    unit: '单',
    deltaText: '3',
    note: '较昨日 · 8 位师傅在岗',
    icon: 'i-wrench',
    iconCls: 'amber',
    deltaCls: 'down',
  },
  {
    label: '今日新增',
    value: '47',
    unit: '单',
    deltaText: '12',
    note: '较昨日 · 高峰 10:00–11:00',
    icon: 'i-calendar',
    iconCls: 'slate',
    deltaCls: 'flat',
  },
  {
    label: '本周完成率',
    value: '92.6',
    pct: '%',
    deltaText: '2.4%',
    note: '较上周 · 目标 90%',
    icon: 'i-check-circle',
    iconCls: 'green',
    deltaCls: 'down',
    progress: 92.6,
  },
]

/** 工单表格（视觉稿示例行） */
export interface MockOrderRow {
  orderNo: string
  urgent?: boolean
  title: string
  address: string
  category: string
  catCls: string
  reporter: { name: string; avatar: string; avatarCls: string }
  repairer: { name: string; avatarCls: string } | null
  status: OrderStatus
  date: string
  time: string
}

export const MOCK_ORDERS: MockOrderRow[] = [
  {
    orderNo: 'RO20261021-018',
    urgent: true,
    title: '3 号教学楼 402 多媒体教室投影仪无法开机',
    address: '3 号教学楼 · 402 多媒体教室',
    category: '教学设备',
    catCls: 'c-blue',
    reporter: { name: '林晓雨', avatar: '林', avatarCls: 'av-1' },
    repairer: null,
    status: 'PENDING',
    date: '10-21',
    time: '14:32',
  },
  {
    orderNo: 'RO20261021-017',
    title: '学生公寓 7 栋 512 卫生间水压过低',
    address: '学生公寓 7 栋 · 512',
    category: '给排水',
    catCls: 'c-cyan',
    reporter: { name: '周子昂', avatar: '周', avatarCls: 'av-2' },
    repairer: { name: '李伟民', avatarCls: 'av-5' },
    status: 'ASSIGNED',
    date: '10-21',
    time: '13:05',
  },
  {
    orderNo: 'RO20261021-016',
    title: '图书馆 2 层自习区空调不制冷',
    address: '图书馆 · 2 层自习区',
    category: '空调设备',
    catCls: 'c-amber',
    reporter: { name: '苏婷', avatar: '苏', avatarCls: 'av-3' },
    repairer: { name: '赵国强', avatarCls: 'av-4' },
    status: 'PROCESSING',
    date: '10-21',
    time: '11:48',
  },
  {
    orderNo: 'RO20261021-015',
    title: '体育馆器材室西侧门锁损坏无法上锁',
    address: '体育馆 · 器材室西侧',
    category: '门窗五金',
    catCls: 'c-green',
    reporter: { name: '何嘉豪', avatar: '何', avatarCls: 'av-1' },
    repairer: { name: '王志远', avatarCls: 'av-2' },
    status: 'COMPLETED',
    date: '10-21',
    time: '10:20',
  },
  {
    orderNo: 'RO20261021-012',
    title: '实验楼 B 栋 305 网络端口无信号',
    address: '实验楼 B 栋 · 305',
    category: '网络通信',
    catCls: 'c-steel',
    reporter: { name: '王思远', avatar: '王', avatarCls: 'av-4' },
    repairer: { name: '陈立群', avatarCls: 'av-3' },
    status: 'CONFIRMED',
    date: '10-20',
    time: '16:10',
  },
  {
    orderNo: 'RO20261020-041',
    title: '一号食堂后厨排水口堵塞返水',
    address: '一号食堂 · 后厨',
    category: '给排水',
    catCls: 'c-cyan',
    reporter: { name: '郑一诺', avatar: '郑', avatarCls: 'av-5' },
    repairer: null,
    status: 'CANCELLED',
    date: '10-20',
    time: '17:42',
  },
]

/** 状态 Tab（视觉稿计数） */
export const STATUS_TABS: { label: string; count: number; status?: OrderStatus }[] = [
  { label: '全部', count: 128 },
  { label: '待受理', count: 18, status: 'PENDING' },
  { label: '已派单', count: 22, status: 'ASSIGNED' },
  { label: '维修中', count: 31, status: 'PROCESSING' },
  { label: '已完成', count: 19, status: 'COMPLETED' },
  { label: '已确认', count: 33, status: 'CONFIRMED' },
  { label: '已取消', count: 5, status: 'CANCELLED' },
]
