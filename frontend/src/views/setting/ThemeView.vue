<script setup lang="ts">
/**
 * 背景主题 —— 系统设置右侧内容块。
 * 通过给 <html> 添加/移除 `dark` 类实现，并用 localStorage 持久化。
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import IconSvg from '@/components/IconSvg.vue'
import { getTheme, setTheme, type ThemeMode } from '@/utils/theme'

const current = ref<ThemeMode>(getTheme())

function onChange(mode: ThemeMode): void {
  current.value = setTheme(mode)
  ElMessage.success(mode === 'dark' ? '已切换为深色模式' : '已切换为浅色模式')
}
</script>

<template>
  <div class="content-block">
    <div class="block-title">
      <h2>界面主题</h2>
      <p>选择你偏好的显示模式，设置会自动保存</p>
    </div>

    <!-- 浅色 / 深色 切换开关 -->
    <div class="theme-row">
      <span class="theme-icon light">
        <IconSvg name="i-sun" :size="18" />
      </span>
      <div class="theme-text">
        <b>浅色模式</b>
        <p>明亮清爽，适合白天使用</p>
      </div>
      <el-switch
        :model-value="current === 'light'"
        size="large"
        @change="(val: string | number | boolean) => onChange(val ? 'light' : 'dark')"
      />
    </div>

    <div class="theme-row no-border">
      <span class="theme-icon dark">
        <IconSvg name="i-moon" :size="18" />
      </span>
      <div class="theme-text">
        <b>深色模式</b>
        <p>护眼柔和，适合夜间使用</p>
      </div>
      <el-switch
        :model-value="current === 'dark'"
        size="large"
        @change="(val: string | number | boolean) => onChange(val ? 'dark' : 'light')"
      />
    </div>
  </div>
</template>

<style scoped>
.content-block {
  min-width: 0;
}
.block-title {
  margin-bottom: 6px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}
.block-title h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-2);
}
.block-title p {
  font-size: 11.5px;
  color: var(--muted-2);
  margin-top: 4px;
}

/* ===== 主题选项行 ===== */
.theme-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}
.theme-row.no-border {
  border-bottom: 0;
}
.theme-icon {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
}
.theme-icon.light {
  background: var(--amber-50);
  color: var(--amber-600);
}
.theme-icon.dark {
  background: #e8eef7;
  color: #41536a;
}
html.dark .theme-icon.dark {
  background: #2a323d;
  color: #8ba0b5;
}
.theme-text {
  flex: 1;
  min-width: 0;
}
.theme-text b {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-2);
}
.theme-text p {
  font-size: 11.5px;
  color: var(--muted-2);
  margin-top: 2px;
}
</style>
