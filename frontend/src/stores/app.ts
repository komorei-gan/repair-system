/**
 * 应用级 UI 状态：移动端侧栏开合、通知面板开合。
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  /** 移动端抽屉式侧栏（<780px） */
  const sidebarOpen = ref(false)
  /** 顶栏通知面板 */
  const notifOpen = ref(false)

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

  return { sidebarOpen, notifOpen, toggleSidebar, toggleNotif, closeOverlays }
})
