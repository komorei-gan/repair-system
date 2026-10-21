<script setup lang="ts">
/**
 * 内联 SVG 图标组件：引用 IconSprite.vue 中定义的 symbol。
 * 用法：<IconSvg name="i-bell" :size="19" />
 */
withDefaults(
  defineProps<{
    name: string
    /** 图标尺寸（px） */
    size?: number
  }>(),
  { size: 18 },
)
</script>

<template>
  <svg class="ico" :style="{ width: size + 'px', height: size + 'px' }" aria-hidden="true">
    <use :href="'#' + name"></use>
  </svg>
</template>

<style scoped>
.ico {
  flex: none;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
