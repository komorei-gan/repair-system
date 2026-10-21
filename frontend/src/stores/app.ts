/**
 * 应用级 UI 状态：移动端侧栏开合、通知面板开合、待受理工单数角标。
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { pendingCount as fetchPendingCount } from '@/api/order'

export const useAppStore = defineStore('app', () => {
  /** 移动端抽屉式侧栏（<780px） */
  const sidebarOpen = ref(false)
  /** 顶栏通知面板 */
  const notifOpen = ref(false)
  /** 待受理（PENDING）工单数，供左侧菜单角标展示 */
  const pendingCount = ref(0)

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

  /** 拉取待受理工单数（失败静默保留上次值） */
  async function refreshPendingCount(): Promise<void> {
    try {
      pendingCount.value = Number(await fetchPendingCount())
    } catch {
      /* 后端未启动时静默 */
    }
  }

  return {
    sidebarOpen,
    notifOpen,
    pendingCount,
    toggleSidebar,
    toggleNotif,
    closeOverlays,
    refreshPendingCount,
  }
})
