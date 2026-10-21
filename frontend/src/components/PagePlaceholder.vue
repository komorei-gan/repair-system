<script setup lang="ts">
/**
 * 占位页通用组件：卡片 + 图标 + 标题 + 说明。
 * 第二阶段逐个替换为真实业务页。
 */
import IconSvg from '@/components/IconSvg.vue'

withDefaults(
  defineProps<{
    title: string
    icon?: string
    description?: string
  }>(),
  {
    icon: 'i-grid',
    description: '该页面将在后续迭代中实现，当前为框架占位。',
  },
)
</script>

<template>
  <div class="placeholder card rise">
    <div class="ph-icon"><IconSvg :name="icon" :size="26" /></div>
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
  </div>
</template>

<style scoped>
.placeholder {
  min-height: calc(100vh - 190px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 24px;
  text-align: center;
}
.ph-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: var(--blue-600);
  background: linear-gradient(150deg, #eaf4ff, #d6e9ff);
  box-shadow: inset 0 0 0 1px rgba(15, 42, 74, 0.03);
}
h1 {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}
p {
  font-size: 13px;
  color: var(--muted);
  max-width: 420px;
}
</style>
