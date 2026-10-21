<script setup lang="ts">
import { useRouter } from 'vue-router'
import IconSvg from '@/components/IconSvg.vue'

const router = useRouter()
</script>

<template>
  <div class="notfound">
    <div class="nf-code num">404</div>
    <h1>页面不存在</h1>
    <p>您访问的页面可能已被移动或删除。</p>
    <button type="button" class="nf-btn" @click="router.push('/dashboard')">
      <IconSvg name="i-grid" :size="15" />
      返回数据看板
    </button>
  </div>
</template>

<style scoped>
.notfound {
  min-height: calc(100vh - 190px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  padding: 40px 24px;
}
.nf-code {
  font-size: 72px;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: var(--blue-100);
  line-height: 1;
}
h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.02em;
}
p {
  font-size: 13px;
  color: var(--muted);
}
.nf-btn {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 15px;
  border-radius: 10px;
  border: 0;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(180deg, #35a0ff 0%, #1890ff 55%, #0f7ee4 100%);
  box-shadow: 0 8px 18px -10px rgba(24, 144, 255, 0.95), inset 0 1px 0 rgba(255, 255, 255, 0.28);
  transition: transform 0.18s var(--ease), box-shadow 0.18s;
}
.nf-btn:hover {
  transform: translateY(-1px);
}
</style>
