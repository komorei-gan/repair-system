/**
 * 应用级 UI 状态：移动端侧栏开合、通知面板开合、待受理工单数角标、通知未读数角标。
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { pendingCount as fetchPendingCount } from '@/api/order'
import { unreadCount as fetchUnreadCount } from '@/api/notification'

export const useAppStore = defineStore('app', () => {
  /** 移动端抽屉式侧栏（<780px） */
  const sidebarOpen = ref(false)
  /** 顶栏通知面板 */
  const notifOpen = ref(false)
  /** 待受理（PENDING）工单数，供左侧菜单角标展示 */
  const pendingCount = ref(0)
  /** 通知未读数，供顶栏铃铛角标展示 */
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
    pendingCount.value = 0
    unreadCount.value = 0
    orderDetailRefreshToken.value = 0
  }

  /** 拉取待受理工单数（失败静默保留上次值） */
  async function refreshPendingCount(): Promise<void> {
    try {
      pendingCount.value = Number(await fetchPendingCount())
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

  return {
    sidebarOpen,
    notifOpen,
    pendingCount,
    unreadCount,
    orderDetailRefreshToken,
    toggleSidebar,
    toggleNotif,
    closeOverlays,
    triggerOrderDetailRefresh,
    reset,
    refreshPendingCount,
    refreshUnreadCount,
    setUnreadCount,
    adjustUnreadCount,
  }
})
