/**
 * 应用级 UI 状态：移动端侧栏开合、通知面板开合、左侧菜单三个角标计数、通知未读数。
 * 三个角标计数集中在此，供左侧菜单（SidebarNav）与顶栏铃铛（TopBar）共用：
 * - dispatchPendingCount：派单中心角标（状态为 PENDING 的待派单工单数）
 * - repairerAuditPendingCount：师傅审核角标（auditStatus=0 待审核师傅申请数）
 * - unreadCount：通知未读数（左侧通知中心菜单 + 右上角铃铛共用同一份）
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { pendingCount as fetchOrderPendingCount, auditingCount as fetchAuditingCount } from '@/api/order'
import { pendingRepairerAuditCount as fetchRepairerAuditPendingCount } from '@/api/repairerAudit'
import { unreadCount as fetchUnreadCount } from '@/api/notification'

export const useAppStore = defineStore('app', () => {
  /** 移动端抽屉式侧栏（<780px） */
  const sidebarOpen = ref(false)
  /** 顶栏通知面板 */
  const notifOpen = ref(false)
  /** 待派单（PENDING）工单数，供「派单中心」菜单角标展示 */
  const dispatchPendingCount = ref(0)
  /** 待审核（auditStatus=0）师傅申请数，供「师傅审核」菜单角标展示 */
  const repairerAuditPendingCount = ref(0)
  /** 待审批（REASSIGNING/ARBITRATING）工单数，供「仲裁中心」菜单角标展示 */
  const arbitrationPendingCount = ref(0)
  /** 通知未读数，供左侧「通知中心」菜单与顶栏铃铛角标共同展示 */
  const unreadCount = ref(0)
  /**
   * 工单详情刷新令牌：递增即通知当前详情页重新拉取数据。
   * 用于「已在目标详情页时再次点击通知」的重复路由场景（router.push 会被判为重复导航而中止）。
   */
  const orderDetailRefreshToken = ref(0)

  function toggleSidebar(open?: boolean): void {
    sidebarOpen.value = open ?? !sidebarOpen.value
  }

  function toggleNotif(open?: boolean): void {
    notifOpen.value = open ?? !notifOpen.value
  }

  function closeOverlays(): void {
    sidebarOpen.value = false
    notifOpen.value = false
  }

  /** 触发当前工单详情页刷新（重复点击同一工单通知时调用） */
  function triggerOrderDetailRefresh(): void {
    orderDetailRefreshToken.value += 1
  }

  /**
   * 重置应用级 UI 状态（退出登录时调用）。
   * 注意：setup 风格的 store 不提供内置 $reset()，必须显式实现。
   */
  function reset(): void {
    sidebarOpen.value = false
    notifOpen.value = false
    dispatchPendingCount.value = 0
    repairerAuditPendingCount.value = 0
    arbitrationPendingCount.value = 0
    unreadCount.value = 0
    orderDetailRefreshToken.value = 0
  }

  /** 拉取待派单工单数（失败静默保留上次值） */
  async function refreshDispatchPendingCount(): Promise<void> {
    try {
      dispatchPendingCount.value = Number(await fetchOrderPendingCount())
    } catch {
      /* 后端未启动时静默 */
    }
  }

  /** 拉取待审核师傅申请数（失败静默保留上次值） */
  async function refreshRepairerAuditPendingCount(): Promise<void> {
    try {
      repairerAuditPendingCount.value = Number(await fetchRepairerAuditPendingCount())
    } catch {
      /* 后端未启动时静默 */
    }
  }

  /** 拉取待审批工单数（失败静默保留上次值） */
  async function refreshArbitrationPendingCount(): Promise<void> {
    try {
      arbitrationPendingCount.value = Number(await fetchAuditingCount())
    } catch {
      /* 后端未启动时静默 */
    }
  }

  /** 拉取通知未读数（失败静默保留上次值） */
  async function refreshUnreadCount(): Promise<void> {
    try {
      const res = await fetchUnreadCount()
      unreadCount.value = Number(res.unreadCount ?? 0)
    } catch {
      /* 后端未启动时静默 */
    }
  }

  /**
   * 一次性拉取左侧菜单全部角标计数（进入后台布局时调用）。
   * 三个请求并行发出，互不阻塞，失败各自静默保留上次值。
   */
  async function refreshBadgeCounts(): Promise<void> {
    await Promise.all([
      refreshDispatchPendingCount(),
      refreshRepairerAuditPendingCount(),
      refreshArbitrationPendingCount(),
      refreshUnreadCount(),
    ])
  }

  /**
   * 直接设置未读数（标记已读/未读后本地同步角标用，无需再请求）。
   * 传 delta 时为相对增减；传绝对值为直接覆盖。均钳制在 [0, ∞)。
   */
  function setUnreadCount(value: number): void {
    unreadCount.value = Math.max(0, value)
  }

  /** 未读数增减（+1 / -1），供标记读/未读后微调角标 */
  function adjustUnreadCount(delta: number): void {
    unreadCount.value = Math.max(0, unreadCount.value + delta)
  }

  /** 待派单工单数增减（派单成功 -1），钳制在 [0, ∞) */
  function adjustDispatchPendingCount(delta: number): void {
    dispatchPendingCount.value = Math.max(0, dispatchPendingCount.value + delta)
  }

  /** 待审核师傅申请数增减（审核通过/拒绝 -1），钳制在 [0, ∞) */
  function adjustRepairerAuditPendingCount(delta: number): void {
    repairerAuditPendingCount.value = Math.max(0, repairerAuditPendingCount.value + delta)
  }

  /** 待审批工单数增减（审批完成 -1），钳制在 [0, ∞) */
  function adjustArbitrationPendingCount(delta: number): void {
    arbitrationPendingCount.value = Math.max(0, arbitrationPendingCount.value + delta)
  }

  return {
    sidebarOpen,
    notifOpen,
    dispatchPendingCount,
    repairerAuditPendingCount,
    arbitrationPendingCount,
    unreadCount,
    orderDetailRefreshToken,
    toggleSidebar,
    toggleNotif,
    closeOverlays,
    triggerOrderDetailRefresh,
    reset,
    refreshDispatchPendingCount,
    refreshRepairerAuditPendingCount,
    refreshArbitrationPendingCount,
    refreshUnreadCount,
    refreshBadgeCounts,
    setUnreadCount,
    adjustUnreadCount,
    adjustDispatchPendingCount,
    adjustRepairerAuditPendingCount,
    adjustArbitrationPendingCount,
  }
})
