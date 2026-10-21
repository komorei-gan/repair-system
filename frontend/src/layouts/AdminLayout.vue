<script setup lang="ts">
/**
 * 管理端布局骨架：侧边栏 + 顶栏 + 主内容区（router-view）。
 */
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import SidebarNav from './components/SidebarNav.vue'
import TopBar from './components/TopBar.vue'

const route = useRoute()
const appStore = useAppStore()

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
          <Transition name="page" mode="out-in">
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

/* 路由切换微过渡 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.page-leave-to {
  opacity: 0;
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
