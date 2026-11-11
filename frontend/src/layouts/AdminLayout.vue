<script setup lang="ts">
/**
 * 管理端布局骨架：侧边栏 + 顶栏 + 主内容区（router-view）。
 * 进入后台布局即拉取左侧菜单三项角标计数（派单中心 / 师傅审核 / 通知中心），
 * 并启动全局轮询定时器：本组件是后台常驻根，不随路由切换卸载，
 * 因此铃铛未读数等角标可以稳定地每 5 秒刷新一次，无需用户手动 F5。
 */
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import SidebarNav from './components/SidebarNav.vue'
import TopBar from './components/TopBar.vue'

const route = useRoute()
const appStore = useAppStore()

/** 轮询间隔：5 秒（移动端发起申请后 Web 端 5s 内即可看到铃铛角标变化） */
const POLL_INTERVAL_MS = 5_000

/** 轮询定时器句柄；undefined 表示未启动 */
let pollTimer: number | undefined
/** 单次轮询是否仍在进行中（防止上一次未返回时叠加下一次请求） */
let polling = false

/** 拉取全部角标计数：失败静默（拦截器已 suppressErrorToast，store 内也各自 try/catch） */
async function pollBadges(): Promise<void> {
  if (polling) return
  polling = true
  try {
    await appStore.refreshBadgeCounts()
  } finally {
    polling = false
  }
}

// 进入后台即拉取左侧菜单角标计数（红点状态），后续由轮询驱动更新
onMounted(() => {
  void pollBadges()
  // 启动轮询：赋值前先清理，防止极端情况下重复挂载叠加定时器
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = window.setInterval(() => {
    void pollBadges()
  }, POLL_INTERVAL_MS)
})

// 组件卸载时清空定时器，防止内存泄漏与重复触发
onBeforeUnmount(() => {
  if (pollTimer !== undefined) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
})

// 路由切换时关闭移动端抽屉与通知面板
watch(
  () => route.fullPath,
  () => appStore.closeOverlays(),
)
</script>

<template>
  <div class="app">
    <SidebarNav />

    <Transition name="fade">
      <div
        v-if="appStore.sidebarOpen"
        class="scrim"
        aria-hidden="true"
        @click="appStore.toggleSidebar(false)"
      ></div>
    </Transition>

    <div class="shell">
      <TopBar />
      <main class="main">
        <router-view v-slot="{ Component }">
          <!-- 路由切换过渡：旧页面淡出，新页面淡入 + 轻微上移（300ms） -->
          <Transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
}
.shell {
  margin-left: 240px;
  min-height: 100vh;
  min-width: 0;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s var(--ease);
}
.main {
  flex: 1;
  padding: 26px 30px 34px;
  max-width: 1680px;
  width: 100%;
  min-width: 0;
}

/* 移动端遮罩 */
.scrim {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(12, 32, 55, 0.36);
  backdrop-filter: blur(2px);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 路由切换微过渡：旧页淡出 / 新页淡入 + 上移 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.2, 0.7, 0.3, 1);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(14px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ===== 响应式 ===== */
@media (max-width: 1180px) {
  .shell {
    margin-left: 78px;
  }
}
@media (max-width: 780px) {
  .shell {
    margin-left: 0;
  }
  .main {
    padding: 18px 16px 26px;
  }
}
</style>
