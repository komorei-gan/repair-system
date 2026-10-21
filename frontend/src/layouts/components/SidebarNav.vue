<script setup lang="ts">
/**
 * 左侧导航 —— 样式与分组 100% 复刻 design-web.html。
 * 点击菜单：切换路由（右侧主内容区随之切换） + 移动端自动收起抽屉。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import IconSvg from '@/components/IconSvg.vue'

interface NavItem {
  name: string
  icon: string
  badge?: number
  dot?: boolean
  /** 是否展示「待受理工单数」动态角标（orders 菜单） */
  pending?: boolean
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const groups: NavGroup[] = [
  {
    label: '概览',
    items: [{ name: 'dashboard', icon: 'i-grid' }],
  },
  {
    label: '工单管理',
    items: [
      { name: 'orders', icon: 'i-clipboard', pending: true },
      { name: 'dispatch', icon: 'i-send' },
      { name: 'repair-progress', icon: 'i-wrench' },
    ],
  },
  {
    label: '资源管理',
    items: [
      { name: 'repairers', icon: 'i-users' },
      { name: 'users', icon: 'i-user' },
      { name: 'categories', icon: 'i-tags' },
    ],
  },
  {
    label: '系统',
    items: [
      { name: 'notifications', icon: 'i-bell', dot: true },
      { name: 'settings', icon: 'i-sliders' },
    ],
  },
]

/** 各菜单标题（取自路由 meta，避免两处维护） */
const routeMeta = computed(() => {
  const map: Record<string, string> = {}
  for (const group of groups) {
    for (const item of group.items) {
      const matched = router.resolve({ name: item.name })
      map[item.name] = String(matched.meta.title ?? item.name)
    }
  }
  return map
})

function isActive(name: string): boolean {
  return route.name === name
}

function onNavigate(name: string): void {
  void router.push({ name })
  if (window.matchMedia('(max-width: 780px)').matches) {
    appStore.toggleSidebar(false)
  }
}
</script>

<template>
  <aside
    class="sidebar"
    :class="{ 'is-open': appStore.sidebarOpen }"
    aria-label="主导航"
  >
    <div class="brand">
      <div class="brand-mark" aria-hidden="true">
        <IconSvg name="i-wrench" :size="21" />
      </div>
      <div class="brand-text">
        <b>校园易修</b>
        <span>CAMPUS FIX</span>
      </div>
    </div>

    <nav class="nav">
      <template v-for="group in groups" :key="group.label">
        <div class="nav-label">{{ group.label }}</div>
        <button
          v-for="item in group.items"
          :key="item.name"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item.name) }"
          :aria-current="isActive(item.name) ? 'page' : undefined"
          :title="routeMeta[item.name]"
          @click="onNavigate(item.name)"
        >
          <IconSvg :name="item.icon" />
          <span class="label">{{ routeMeta[item.name] }}</span>
          <span v-if="item.pending && appStore.pendingCount > 0" class="nav-badge">
            {{ appStore.pendingCount > 99 ? '99+' : appStore.pendingCount }}
          </span>
          <span v-else-if="item.badge" class="nav-badge">{{ item.badge }}</span>
          <span v-else-if="item.dot" class="nav-dot" aria-hidden="true"></span>
        </button>
      </template>
    </nav>

    <div class="side-foot">
      <div class="sys-card">
        <div class="sys-row">
          <span class="status-dot" aria-hidden="true"></span>
          <span>系统运行正常</span>
          <span class="sys-ping">99.98%</span>
        </div>
        <p>通知轮询 · 30s 自动同步</p>
        <div class="sys-bar" aria-hidden="true"><span style="width: 99%"></span></div>
      </div>
      <div class="side-version"><span>CampusFix Admin</span><span>v1.4.2</span></div>
    </div>
  </aside>
</template>

<style scoped>
/* ===== 结构（与 design-web.html 完全一致） ===== */
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 100;
  width: 240px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #fcfeff 58%, #f8fbff 100%);
  border-right: 1px solid var(--line);
  transition: transform 0.3s var(--ease), width 0.3s var(--ease);
}
.sidebar::after {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 180px;
  z-index: -1;
  background: radial-gradient(220px 120px at 20% 0%, rgba(24, 144, 255, 0.09), transparent 70%);
  pointer-events: none;
}

/* ===== 品牌 ===== */
.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 22px 20px 18px;
  flex: none;
}
.brand-mark {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(145deg, #4aabff 0%, #1890ff 48%, #0b6fd0 100%);
  box-shadow: 0 8px 18px -8px rgba(24, 144, 255, 0.85), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  position: relative;
}
.brand-mark::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  pointer-events: none;
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  min-width: 0;
}
.brand-text b {
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: 0.01em;
}
.brand-text span {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--muted-2);
  margin-top: 4px;
  white-space: nowrap;
}

/* ===== 菜单 ===== */
.nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 12px 8px;
  scrollbar-width: thin;
  scrollbar-color: #dce7f3 transparent;
}
.nav::-webkit-scrollbar {
  width: 5px;
}
.nav::-webkit-scrollbar-thumb {
  background: #e0eaf5;
  border-radius: 99px;
}
.nav-label {
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #6b8095;
  padding: 16px 12px 7px;
}
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 9px 12px;
  margin: 2px 0;
  border: 0;
  background: transparent;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 500;
  color: #4b637b;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s, color 0.18s, box-shadow 0.18s;
}
.nav-item :deep(.ico) {
  color: #7a90a8;
  transition: color 0.18s;
}
.nav-item:hover {
  background: #f2f8ff;
  color: var(--blue-700);
}
.nav-item:hover :deep(.ico) {
  color: var(--blue-500);
}
.nav-item.active {
  color: var(--blue-700);
  font-weight: 600;
  background: linear-gradient(90deg, #e9f4ff 0%, #f6faff 100%);
  box-shadow: inset 0 0 0 1px #d8eaff;
}
.nav-item.active :deep(.ico) {
  color: var(--blue-500);
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 17px;
  border-radius: 0 3px 3px 0;
  background: var(--blue-500);
}
.nav-item .label {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 19px;
  height: 18px;
  padding: 0 6px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  background: var(--red-50);
  color: #e5484d;
}
.nav-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--blue-500);
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.13);
  flex: none;
}

/* ===== 底部系统卡 ===== */
.side-foot {
  flex: none;
  padding: 12px 14px 16px;
}
.sys-card {
  padding: 12px 13px;
  border: 1px solid #e1edfa;
  border-radius: 12px;
  background: linear-gradient(160deg, #f7fbff 0%, #f1f8ff 100%);
}
.sys-row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  font-weight: 600;
  color: #3e6284;
}
.sys-ping {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--green-700);
  background: var(--green-50);
  padding: 1px 6px;
  border-radius: 5px;
  font-weight: 600;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--green-500);
  flex: none;
  box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.16);
  animation: breathe 2.8s ease-in-out infinite;
}
@keyframes breathe {
  0%,
  100% {
    box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.16);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(82, 196, 26, 0.06);
  }
}
.sys-card p {
  font-size: 10.5px;
  color: #7c93a9;
  margin-top: 6px;
}
.sys-bar {
  height: 3px;
  border-radius: 99px;
  background: #e3eef9;
  margin-top: 9px;
  overflow: hidden;
}
.sys-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--green-500), #86d95f);
}
.side-version {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  color: #6b8095;
  padding: 10px 4px 0;
}
.side-version span {
  font-variant-numeric: tabular-nums;
}

/* ===== 响应式：图标轨（1180px） ===== */
@media (max-width: 1180px) {
  .sidebar {
    width: 78px;
  }
  .brand {
    padding: 20px 0;
    justify-content: center;
  }
  .brand-text,
  .nav-label,
  .nav-badge,
  .nav-dot,
  .nav-item .label,
  .side-foot {
    display: none;
  }
  .nav {
    padding: 6px 10px;
  }
  .nav-item {
    justify-content: center;
    width: 46px;
    height: 44px;
    margin: 4px auto;
    padding: 0;
  }
  .nav-item.active::before {
    left: -8px;
  }
}

/* ===== 响应式：抽屉（780px） ===== */
@media (max-width: 780px) {
  .sidebar {
    width: 258px;
    transform: translateX(-100%);
    box-shadow: 26px 0 60px -30px rgba(8, 30, 55, 0.45);
  }
  .sidebar.is-open {
    transform: none;
  }
  .brand {
    justify-content: flex-start;
    padding: 20px 18px 16px;
  }
  .brand-text {
    display: flex;
  }
  .nav-label {
    display: block;
  }
  .nav-item {
    justify-content: flex-start;
    width: 100%;
    height: auto;
    margin: 2px 0;
    padding: 9px 12px;
  }
  .nav-item .label {
    display: block;
  }
  .nav-badge {
    display: inline-flex;
  }
  .nav-dot {
    display: inline-block;
  }
  .nav-item.active::before {
    left: 0;
  }
  .side-foot {
    display: block;
  }
  .nav {
    padding: 4px 12px 8px;
  }
}
</style>
