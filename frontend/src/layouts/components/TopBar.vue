<script setup lang="ts">
/**
 * 顶栏 —— 面包屑、搜索、帮助、铃铛、用户信息、登出。
 * 说明：铃铛未读数的轮询已上提到 AdminLayout（后台常驻根，不随路由切换卸载），
 * 此处不再自行起轮询，避免重复请求。初次进入时由 AdminLayout 立即拉取一次。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import type { NotificationVO } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'
import NotificationPanel from './NotificationPanel.vue'
import NotificationDetailDialog from '@/components/NotificationDetailDialog.vue'
import HelpDialog from '@/components/HelpDialog.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

/** 帮助中心弹窗显隐 */
const helpVisible = ref(false)
/** 通知详情弹窗：显隐 + 当前展示的通知 */
const detailVisible = ref(false)
const activeNotif = ref<NotificationVO | null>(null)

/** 打开通知详情弹窗（铃铛面板点击后调用） */
function onOpenDetail(n: NotificationVO): void {
  activeNotif.value = n
  detailVisible.value = true
}

/** 弹窗关闭后刷新一次未读角标 */
watch(detailVisible, (v) => {
  if (!v) void appStore.refreshUnreadCount()
})

const pageTitle = computed(() => String(route.meta.title ?? ''))

/** 当前登录用户是否为管理员（管理员无「个人中心」页面，下拉菜单需隐藏该项） */
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

/**
 * 用户下拉菜单统一入口：
 * - settings → 跳转系统设置页
 * - logout   → 二次确认后登出
 */
function onDropdownCommand(cmd: string): void {
  if (cmd === 'settings') {
    if (route.name !== 'settings') {
      void router.push('/settings')
    }
    return
  }
  if (cmd === 'logout') {
    void onLogout()
  }
}

onMounted(() => {
  // 进入顶栏即主动拉取一次未读数，避免等待下一轮轮询（其余角标由 AdminLayout 兜底）
  void appStore.refreshUnreadCount()
  document.addEventListener('click', onDocClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})

function toggleNotif(): void {
  appStore.toggleNotif()
}

/**
 * 面板底部「查看全部通知」：
 * 1) 跳转通知中心（router.push('/notifications')）；
 * 2) 关闭弹出面板；
 * 3) 左侧菜单高亮由 SidebarNav 依据 route.name 自动同步，无需额外处理。
 */
function onViewAllNotices(): void {
  appStore.toggleNotif(false)
  if (route.name !== 'notifications') {
    void router.push('/notifications')
  }
}

/** 点击面板外部关闭 */
function onDocClick(e: MouseEvent): void {
  const wrap = document.getElementById('notif-wrap')
  const panel = document.getElementById('notif-panel')
  if (wrap && panel && !wrap.contains(e.target as Node) && !panel.contains(e.target as Node)) {
    appStore.toggleNotif(false)
  }
}

/**
 * 退出登录：
 * 1) 使用标准 Element Plus 确认框（禁止 window.confirm）；
 * 2) 确认后清空 Pinia 状态（auth + app）与 localStorage 凭证；
 * 3) 用 router.replace 跳登录页——不保留历史记录，避免「后退」回到受保护页面。
 */
async function onLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
      // 关闭点击遮罩关闭，避免误触导致意料之外的退出
      closeOnClickModal: false,
    })
  } catch {
    return // 用户取消：不做任何清理
  }

  // 先关闭可能展开的浮层，防止残留 DOM 状态带入登录页
  appStore.closeOverlays()
  // 清空 Pinia（auth 内部会同步清掉 localStorage 的 token 与用户缓存）
  authStore.logout()
  appStore.reset()
  // 兜底清空 localStorage 中本应用的键（不用 clear()，避免误删第三方数据）
  localStorage.removeItem('repair_token')
  localStorage.removeItem('repair_user')

  ElMessage.success('已安全退出')
  // replace：不产生历史记录，退出后无法「后退」回到控制台
  void router.replace('/login')
}
</script>

<template>
  <header class="topbar">
    <button
      type="button"
      class="icon-btn menu-btn"
      aria-label="打开导航"
      :aria-expanded="appStore.sidebarOpen"
      @click="appStore.toggleSidebar()"
    >
      <IconSvg name="i-menu" :size="19" />
    </button>

    <nav class="crumb" aria-label="面包屑">
      <span>工作台</span>
      <IconSvg name="i-chevron-right" :size="13" />
      <strong>{{ pageTitle }}</strong>
    </nav>

    <div class="topbar-right">
      <button type="button" class="icon-btn" aria-label="帮助中心" @click="helpVisible = true">
        <IconSvg name="i-help" :size="19" />
      </button>

      <div class="notif-wrap" id="notif-wrap">
        <button
          type="button"
          class="icon-btn"
          :aria-label="`消息通知，${appStore.unreadCount} 条未读`"
          :aria-expanded="appStore.notifOpen"
          @click="toggleNotif"
        >
          <IconSvg name="i-bell" :size="19" />
          <span v-if="appStore.unreadCount > 0" class="bell-badge">
            {{ appStore.unreadCount > 99 ? '99+' : appStore.unreadCount }}
          </span>
        </button>

        <NotificationPanel
          v-if="appStore.notifOpen"
          id="notif-panel"
          @read-all="appStore.refreshUnreadCount"
          @view-all="onViewAllNotices"
          @open-detail="onOpenDetail"
        />
      </div>

      <div class="topbar-divider" aria-hidden="true"></div>

      <el-dropdown trigger="click" @command="onDropdownCommand">
        <button type="button" class="profile">
          <span class="avatar-lg" aria-hidden="true">{{ authStore.avatarChar }}</span>
          <span class="profile-meta">
            <b>{{ authStore.displayName }}</b>
            <span>后勤保障中心 · 管理员</span>
          </span>
          <IconSvg name="i-chevron-down" :size="14" />
        </button>
        <template #dropdown>
          <el-dropdown-menu class="profile-menu">
            <el-dropdown-item v-if="!isAdmin" disabled>
              <IconSvg name="i-user" :size="15" />
              个人中心
            </el-dropdown-item>
            <el-dropdown-item command="settings" :divided="!isAdmin">
              <IconSvg name="i-sliders" :size="15" />
              系统设置
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <IconSvg name="i-logout" :size="15" />
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 全局帮助中心弹窗 -->
    <HelpDialog v-model="helpVisible" />

    <!-- 通知详情弹窗 -->
    <NotificationDetailDialog v-model="detailVisible" :notification="activeNotif" />
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 66px;
  flex: none;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 30px;
  background: var(--topbar-bg);
  backdrop-filter: blur(16px) saturate(1.25);
  -webkit-backdrop-filter: blur(16px) saturate(1.25);
  border-bottom: 1px solid var(--line);
}
.menu-btn {
  display: none;
}
.icon-btn {
  position: relative;
  width: 38px;
  height: 38px;
  flex: none;
  display: inline-grid;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 11px;
  background: transparent;
  color: #5a728a;
  cursor: pointer;
  transition: background 0.18s, color 0.18s, border-color 0.18s;
}
html.dark .icon-btn {
  color: #9aadc1;
}
.icon-btn:hover {
  background: #f2f7fd;
  border-color: #e6eff9;
  color: var(--blue-700);
}
html.dark .icon-btn:hover {
  background: #23272e;
  border-color: #36424f;
  color: #63b5ff;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--muted-2);
  white-space: nowrap;
}
.crumb :deep(.ico) {
  color: #b3c3d3;
}
.crumb strong {
  color: var(--ink-2);
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}
.topbar-divider {
  width: 1px;
  height: 24px;
  background: var(--line);
  margin: 0 8px;
}

.notif-wrap {
  position: relative;
}
.bell-badge {
  position: absolute;
  top: 5px;
  right: 4px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #fff;
  background: linear-gradient(180deg, #ff6b6b, #f5222d);
  border: 2px solid #fff;
  border-radius: 99px;
  box-shadow: 0 3px 8px -3px rgba(245, 34, 45, 0.8);
}

.profile {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 6px 4px 4px;
  margin-left: 4px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s, border-color 0.18s;
}
.profile:hover {
  background: #f7fafe;
  border-color: #eaf2fa;
}
html.dark .profile:hover {
  background: #23272e;
  border-color: #36424f;
}
.avatar-lg {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #5fb3ff, #0e7be0);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35), 0 4px 10px -6px rgba(14, 123, 224, 0.8);
}
.profile-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}
.profile-meta b {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}
.profile-meta span {
  font-size: 10.5px;
  color: var(--muted-2);
  white-space: nowrap;
}
.profile :deep(.ico) {
  color: #a5b7c9;
}

/* ===== 用户下拉菜单 ===== */
/* 下拉菜单通过 teleport 渲染到 body，需用 :global 穿透 scoped 作用域 */
:global(.profile-menu) {
  min-width: 168px;
  padding: 6px;
}
:global(.profile-menu .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1;
}
:global(.profile-menu .el-dropdown-menu__item:not(.is-disabled):not(.is-divided)) {
  color: #3e5872;
}
:global(.profile-menu .el-dropdown-menu__item:not(.is-disabled):hover) {
  background: var(--blue-50);
  color: var(--blue-700);
}
:global(.profile-menu .el-dropdown-menu__item .ico) {
  flex: none;
  color: #8fa3b8;
}
:global(.profile-menu .el-dropdown-menu__item:not(.is-disabled):hover .ico) {
  color: var(--blue-600);
}
/* 分割线项：上方留出间距，避免菜单项挤在一起 */
:global(.profile-menu .el-dropdown-menu__item--divided) {
  margin-top: 6px;
}
:global(.profile-menu .el-dropdown-menu__item--divided::before) {
  margin: 0 -6px 6px;
}

/* ===== 响应式 ===== */
@media (max-width: 860px) {
  .crumb {
    display: none;
  }
  .menu-btn {
    display: inline-grid;
  }
}
@media (max-width: 780px) {
  .topbar {
    padding: 0 16px;
    height: 60px;
    gap: 10px;
  }
  .topbar-divider {
    display: none;
  }
  .profile-meta {
    display: none;
  }
  .profile {
    padding: 3px;
  }
}
@media (max-width: 520px) {
  .topbar-right .icon-btn[aria-label='帮助中心'] {
    display: none;
  }
}
</style>
