<script setup lang="ts">
/**
 * 系统设置 —— 左右分栏布局（类似微信 PC 端）。
 * 左侧固定 240px 长条形选项列表，右侧动态展示对应内容。
 * 通过 activeTab 响应式变量切换右侧内容，不使用子路由。
 */
import { ref } from 'vue'
import IconSvg from '@/components/IconSvg.vue'
import HelpDialog from '@/components/HelpDialog.vue'
import PasswordView from './PasswordView.vue'
import SystemInfoView from './SystemInfoView.vue'
import ThemeView from './ThemeView.vue'

type SettingTab = 'password' | 'system' | 'theme'

const activeTab = ref<SettingTab>('password')
/** 帮助中心弹窗显隐 */
const helpVisible = ref(false)

interface SettingItem {
  key: SettingTab
  label: string
  icon: string
  tone: 'blue' | 'amber' | 'slate'
}

const items: SettingItem[] = [
  { key: 'password', label: '修改密码', icon: 'i-lock', tone: 'blue' },
  { key: 'system', label: '系统信息', icon: 'i-info', tone: 'amber' },
  { key: 'theme', label: '背景主题', icon: 'i-moon', tone: 'slate' },
]

function switchTab(key: SettingTab): void {
  activeTab.value = key
}
</script>

<template>
  <div class="setting-view">
    <div class="page-head">
      <div>
        <h1>系统设置</h1>
        <p class="page-sub">账号安全、系统信息与界面主题</p>
      </div>
    </div>

    <div class="setting-layout">
      <!-- ============ 左侧：固定 240px 选项栏 ============ -->
      <aside class="card setting-rail" aria-label="设置选项">
        <button
          v-for="item in items"
          :key="item.key"
          type="button"
          class="setting-item"
          :class="{ active: activeTab === item.key }"
          @click="switchTab(item.key)"
        >
          <span class="item-icon" :class="item.tone">
            <IconSvg :name="item.icon" :size="16" />
          </span>
          <span class="item-label">{{ item.label }}</span>
          <span class="item-arrow">
            <IconSvg name="i-chevron-right" :size="15" />
          </span>
        </button>

        <!-- 帮助中心：非 Tab，点击打开弹窗 -->
        <button type="button" class="setting-item no-border" @click="helpVisible = true">
          <span class="item-icon slate">
            <IconSvg name="i-help" :size="16" />
          </span>
          <span class="item-label">帮助中心</span>
          <span class="item-arrow">
            <IconSvg name="i-chevron-right" :size="15" />
          </span>
        </button>
      </aside>

      <!-- ============ 右侧：动态内容（包裹在白色卡片中） ============ -->
      <section class="card setting-content" aria-label="设置内容">
        <PasswordView v-if="activeTab === 'password'" />
        <SystemInfoView v-else-if="activeTab === 'system'" />
        <ThemeView v-else />
      </section>
    </div>

    <!-- 帮助中心弹窗 -->
    <HelpDialog v-model="helpVisible" />
  </div>
</template>

<style scoped>
.setting-view {
  min-width: 0;
  /* 大屏居中：限制最大宽度，紧凑专业 */
  max-width: 1000px;
  margin: 0 auto;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.page-head h1 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.2;
  color: var(--ink);
}
.page-sub {
  font-size: 12.5px;
  color: var(--muted);
  margin-top: 6px;
}

/* ===== 左右分栏 ===== */
.setting-layout {
  display: flex;
  gap: 18px;
  align-items: stretch;
}

/* ===== 左侧选项栏 ===== */
.setting-rail {
  width: 280px;
  flex: none;
  overflow: hidden;
}
.setting-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 15px 18px;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.16s;
}
.setting-item:hover {
  background: #f5f8fc;
}
html.dark .setting-item:hover {
  background: #23272e;
}
.setting-item.no-border {
  border-bottom: 0;
}
/* 选中项：浅蓝高亮 */
.setting-item.active {
  background: var(--blue-50);
}
html.dark .setting-item.active {
  background: rgba(24, 144, 255, 0.16);
}
.setting-item.active .item-label {
  color: var(--blue-700);
  font-weight: 600;
}
html.dark .setting-item.active .item-label {
  color: #63b5ff;
}

.item-icon {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
}
.item-icon.blue {
  background: var(--blue-50);
  color: var(--blue-600);
}
.item-icon.amber {
  background: var(--amber-50);
  color: var(--amber-600);
}
.item-icon.slate {
  background: #f0f5fb;
  color: #5e7a96;
}
html.dark .item-icon.slate {
  background: #2a323d;
  color: #8ba0b5;
}

.item-label {
  flex: 1;
  min-width: 0;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ink-2);
}
.item-arrow {
  flex: none;
  color: #b6c6d7;
}
html.dark .item-arrow {
  color: #55626f;
}

/* ===== 右侧内容 ===== */
.setting-content {
  flex: 1;
  min-width: 0;
  padding: 24px;
}

/* ===== 响应式 ===== */
@media (max-width: 780px) {
  .setting-layout {
    flex-direction: column;
  }
  .setting-rail {
    width: 100%;
  }
}
</style>
