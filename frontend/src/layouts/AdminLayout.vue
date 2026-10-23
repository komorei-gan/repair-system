<script setup lang="ts">
/**
 * 管理端布局骨架：侧边栏 + 顶栏 + 主内容区（router-view）。
 * 进入后台布局即拉取左侧菜单三项角标计数（派单中心 / 师傅审核 / 通知中心）。
 */
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import SidebarNav from './components/SidebarNav.vue'
import TopBar from './components/TopBar.vue'

const route = useRoute()
const appStore = useAppStore()

// 进入后台即拉取左侧菜单角标计数（红点状态），后续由操作与轮询驱动更新
onMounted(() => {
  void appStore.refreshBadgeCounts()
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
