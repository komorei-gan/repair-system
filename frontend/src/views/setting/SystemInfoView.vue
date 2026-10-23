<script setup lang="ts">
/**
 * 系统信息 —— 系统设置右侧内容块（纯静态展示）。
 */
const systemInfo = [
  { label: '系统名称', value: '校园易修 · 报修工单系统' },
  { label: '当前版本', value: 'v1.0.0' },
  { label: '后端技术栈', value: 'Spring Boot 3 + MySQL 8 + MyBatis-Plus' },
  { label: '前端技术栈', value: 'Vue 3 + Element Plus（Web）+ uni-app（移动端）' },
]
</script>

<template>
  <div class="content-block">
    <div class="block-title">
      <h2>当前运行环境</h2>
      <p>系统概览信息</p>
    </div>

    <ul class="info-list">
      <li v-for="item in systemInfo" :key="item.label" class="info-row">
        <span class="info-label">{{ item.label }}</span>
        <span class="info-value">{{ item.value }}</span>
      </li>
    </ul>
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

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px dashed var(--line);
}
.info-row:last-child {
  border-bottom: 0;
}
.info-label {
  flex: none;
  font-size: 12.5px;
  color: var(--muted);
}
.info-value {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-2);
  text-align: right;
  word-break: break-all;
}
</style>
