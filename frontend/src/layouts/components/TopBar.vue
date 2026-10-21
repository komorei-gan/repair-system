<script setup lang="ts">
/**
 * 顶栏 —— 面包屑、搜索、帮助、铃铛（30s 轮询未读数）、用户信息、登出。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as notifApi from '@/api/notification'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import IconSvg from '@/components/IconSvg.vue'
import NotificationPanel from './NotificationPanel.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const unread = ref(0)
let pollTimer: number | undefined

const pageTitle = computed(() => String(route.meta.title ?? ''))

/** 30s 轮询未读数（design.md §4.2），失败静默保留上次值 */
async function refreshUnread(): Promise<void> {
  try {
    const res = await notifApi.unreadCount()
    unread.value = Number(res.unreadCount ?? 0)
  } catch {
    /* 后端未启动时静默 */
  }
}

onMounted(() => {
  void refreshUnread()
  void appStore.refreshPendingCount()
  pollTimer = window.setInterval(() => {
    void refreshUnread()
    void appStore.refreshPendingCount()
  }, 30_000)
  document.addEventListener('click', onDocClick)
})

onBeforeUnmount(() => {
  if (pollTimer) window.clearInterval(pollTimer)
  document.removeEventListener('click', onDocClick)
})

function toggleNotif(): void {
  appStore.toggleNotif()
}

/** 点击面板外部关闭 */
function onDocClick(e: MouseEvent): void {
  const wrap = document.getElementById('notif-wrap')
  const panel = document.getElementById('notif-panel')
  if (wrap && panel && !wrap.contains(e.target as Node) && !panel.contains(e.target as Node)) {
    appStore.toggleNotif(false)
  }
}

async function onLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }
  authStore.logout()
  ElMessage.success('已安全退出')
  void router.push('/login')
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

    <div class="topbar-search">
      <IconSvg name="i-search" :size="15" />
      <input type="search" placeholder="搜索工单号 / 报修人 / 故障关键词" aria-label="搜索工单" />
      <kbd>/</kbd>
    </div>

    <div class="topbar-right">
      <button type="button" class="icon-btn" aria-label="帮助中心">
        <IconSvg name="i-help" :size="19" />
      </button>

      <div class="notif-wrap" id="notif-wrap">
        <button
          type="button"
          class="icon-btn"
          :aria-label="`消息通知，${unread} 条未读`"
          :aria-expanded="appStore.notifOpen"
          @click="toggleNotif"
        >
          <IconSvg name="i-bell" :size="19" />
          <span v-if="unread > 0" class="bell-badge">{{ unread > 99 ? '99+' : unread }}</span>
        </button>

        <NotificationPanel
          v-if="appStore.notifOpen"
          id="notif-panel"
          @read-all="refreshUnread"
        />
      </div>

      <div class="topbar-divider" aria-hidden="true"></div>

      <el-dropdown trigger="click" @command="(cmd: string) => cmd === 'logout' && onLogout()">
        <button type="button" class="profile">
          <span class="avatar-lg" aria-hidden="true">{{ authStore.avatarChar }}</span>
          <span class="profile-meta">
            <b>{{ authStore.displayName }}</b>
            <span>后勤保障中心 · 管理员</span>
          </span>
          <IconSvg name="i-chevron-down" :size="14" />
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
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
  background: rgba(255, 255, 255, 0.82);
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
.icon-btn:hover {
  background: #f2f7fd;
  border-color: #e6eff9;
  color: var(--blue-700);
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

.topbar-search {
  position: relative;
  flex: 1;
  max-width: 360px;
  margin-left: 8px;
}
.topbar-search :deep(.ico) {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #8fa3b8;
  pointer-events: none;
}
.topbar-search input {
  width: 100%;
  height: 38px;
  padding: 0 44px 0 35px;
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  background: #f8fbff;
  color: var(--ink);
  font-size: 13px;
  transition: border-color 0.18s, background 0.18s, box-shadow 0.18s;
}
.topbar-search input::placeholder {
  color: #9aadc1;
}
.topbar-search input:focus {
  outline: none;
  border-color: var(--blue-500);
  background: #fff;
  box-shadow: var(--ring);
}
.topbar-search kbd {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-family: inherit;
  font-size: 11px;
  color: #93a7bb;
  border: 1px solid #e3ecf6;
  background: #fff;
  border-radius: 5px;
  min-width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  line-height: 1;
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

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .topbar-search {
    max-width: 250px;
  }
}
@media (max-width: 860px) {
  .topbar-search,
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
