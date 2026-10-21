<script setup lang="ts">
/**
 * ECharts 通用封装：
 * - 按需注册（echarts/core + 对应图表 + CanvasRenderer）
 * - ResizeObserver 自适应容器
 * - showLoading / hideLoading 透传（数据刷新动效）
 * - 组件卸载时 dispose
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'

// 按需注册渲染器与图表类型（各使用方注册自己的图表）
echarts.use([CanvasRenderer])

const props = withDefaults(
  defineProps<{
    option: EChartsCoreOption
    /** 初始加载动画 */
    loading?: boolean
    loadingText?: string
    /** 容器高度 */
    height?: string
  }>(),
  {
    loading: false,
    loadingText: '正在加载…',
    height: '100%',
  },
)

const emit = defineEmits<{ 'chart-ready': [instance: echarts.ECharts] }>()

const el = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let observer: ResizeObserver | null = null

onMounted(() => {
  if (!el.value) return
  chart = echarts.init(el.value, undefined, { renderer: 'canvas' })
  chart.setOption(props.option)
  if (props.loading) {
    chart.showLoading({
      text: props.loadingText,
      color: '#1890FF',
      maskColor: 'rgba(255,255,255,.75)',
      textColor: '#5E7489',
      fontSize: 13,
      spinnerRadius: 9,
      lineWidth: 3,
    })
  }
  emit('chart-ready', chart)

  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(el.value)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  chart?.dispose()
  chart = null
})

watch(
  () => props.option,
  (opt) => {
    chart?.setOption(opt, { notMerge: true })
  },
  { deep: true },
)

watch(
  () => props.loading,
  (val) => {
    if (!chart) return
    if (val) {
      chart.showLoading({
        text: props.loadingText,
        color: '#1890FF',
        maskColor: 'rgba(255,255,255,.75)',
        textColor: '#5E7489',
        fontSize: 13,
        spinnerRadius: 9,
        lineWidth: 3,
      })
    } else {
      chart.hideLoading()
    }
  },
)

/** 供父组件直接调用实例（高级用法） */
function getInstance(): echarts.ECharts | null {
  return chart
}

/** 手动触发 loading（如点击刷新按钮） */
function showLoading(text?: string): void {
  chart?.showLoading({
    text: text ?? props.loadingText,
    color: '#1890FF',
    maskColor: 'rgba(255,255,255,.75)',
    textColor: '#5E7489',
    fontSize: 13,
    spinnerRadius: 9,
    lineWidth: 3,
  })
}

function hideLoading(): void {
  chart?.hideLoading()
}

defineExpose({ getInstance, showLoading, hideLoading })
</script>

<template>
  <div ref="el" class="base-chart" :style="{ height }" role="img"></div>
</template>

<style scoped>
.base-chart {
  width: 100%;
  min-height: 40px;
}
</style>
