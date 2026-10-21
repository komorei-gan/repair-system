<script setup lang="ts">
/**
 * ECharts 通用封装（统一生长动画时序）：
 * - 按需注册（echarts/core + 各图表 + CanvasRenderer）
 * - ResizeObserver 自适应容器
 * - 组件卸载时 dispose
 *
 * ★ 生长动画为什么必须“清空重建”：
 *   ECharts 的 setOption(option, true) 只保证「模型」不复用，**视图仍会复用**：
 *   prepareView() 用 `_ec_${series.id}_${series.type}` 作为 viewId，
 *   只要 series 的 id 没变（本项目的 'new' / 'done' / 'status-donut' 都是固定 id），
 *   哪怕是 notMerge: true，折线图的 polyline 与饼图的扇区也会沿用旧实例，
 *   于是走的不是「首次创建 → 入场生长动画」分支，而是「更新 → 形变动画」分支。
 *   空数组更新到满数据时，点位会被直接补成终值（lineAnimationDiff 对新增点不做位移），
 *   肉眼看就是“数据直接变出来”，没有任何生长过程。
 *
 *   因此这里在塞入真实数据前调用 chart.clear()：clear() 会以 `series: []` 做一次
 *   notMerge 更新，所有 series 视图因未存活而被 dispose 并移出 viewMap；
 *   紧接着 setOption(真实数据) 才会重新创建视图，重新触发：
 *     - 折线：clipPath 宽度 0 → 全宽的“从左向右生长”，面积同步升起
 *     - 饼图：扇区从起始角度展开
 *
 * ★ 容器尺寸与时机：
 *   setOption 前显式 chart.resize()；若容器此刻宽高仍为 0
 *   （隐藏 tab、路由过渡、父级尚未完成布局），先等 rAF 重试，避免把动画画进 0×0 画布。
 *
 * 可访问性：URL 带 ?noanim 或系统开启「减弱动态效果」时跳过生长时序；
 *          URL 带 ?anim 可强制播放（便于排查系统减弱动态的影响）。
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'

echarts.use([CanvasRenderer])

const props = withDefaults(
  defineProps<{
    /** 真实数据 option */
    option: EChartsCoreOption
    /** 空数据 option（生长动画起点），需保留坐标系，仅 series.data 为空 */
    emptyOption?: EChartsCoreOption
    /** 是否播放生长动画 */
    grow?: boolean
    /** 空态 → 真实数据的延迟（ms） */
    growDelay?: number
    /** 容器高度 */
    height?: string
  }>(),
  {
    grow: true,
    growDelay: 320,
    height: '100%',
  },
)

const emit = defineEmits<{ 'chart-ready': [instance: echarts.ECharts] }>()

const el = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let observer: ResizeObserver | null = null
let growTimer = 0
let rafId = 0

/** 是否跳过动画（截图辅助 + 尊重系统减弱动态设置；?anim 可强制播放） */
function shouldSkipAnimation(): boolean {
  if (typeof window === 'undefined') return false
  const params = new URLSearchParams(window.location.search)
  if (params.has('anim')) return false
  return params.has('noanim') || window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 等容器有真实宽高后再执行回调（最多重试 30 帧，约 0.5s） */
function whenSized(fn: () => void, tries = 0): void {
  const node = el.value
  if (!node || !chart) return
  if (node.clientWidth > 0 && node.clientHeight > 0) {
    chart.resize()
    fn()
    return
  }
  if (tries >= 30) {
    fn()
    return
  }
  rafId = requestAnimationFrame(() => whenSized(fn, tries + 1))
}

/** 普通渲染：只更新数据，不重播入场动画 */
function render(opt: EChartsCoreOption): void {
  chart?.setOption(opt, { notMerge: true })
}

/**
 * 从头渲染真实数据：clear() 销毁旧视图 → setOption 必然走「首次创建」，
 * 从而重新触发 ECharts 原生入场生长动画。
 */
function renderFromScratch(): void {
  if (!chart) return
  chart.resize()
  chart.clear()
  chart.setOption(props.option, { notMerge: true })
}

/** 取消尚未执行的生长时序（定时器 + 尺寸重试帧） */
function cancelGrow(): void {
  window.clearTimeout(growTimer)
  cancelAnimationFrame(rafId)
}

/**
 * 播放生长动画：先显示保留坐标系的空态，延迟后清空重建真实数据。
 * 挂载（含 F5）与刷新按钮共用这一条路径。
 */
function playGrow(): void {
  if (!chart) return
  cancelGrow()
  whenSized(() => {
    if (!chart) return
    // 空态只保留坐标系与轴，series.data 为 []，作为生长起点
    chart.setOption(props.emptyOption ?? props.option, { notMerge: true })
    growTimer = window.setTimeout(() => {
      if (!chart) return
      renderFromScratch()
    }, props.growDelay)
  })
}

onMounted(() => {
  if (!el.value) return
  chart = echarts.init(el.value, undefined, { renderer: 'canvas' })
  emit('chart-ready', chart)

  if (props.grow && !shouldSkipAnimation()) {
    playGrow()
  } else {
    whenSized(() => render(props.option))
  }

  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(el.value)
})

onBeforeUnmount(() => {
  cancelGrow()
  observer?.disconnect()
  observer = null
  chart?.dispose()
  chart = null
})

watch(
  () => props.option,
  (opt) => {
    if (!chart) return
    // 数据变化时同样重播生长动画，保证“换数据”也有生长过程
    if (props.grow && !shouldSkipAnimation()) {
      playGrow()
    } else {
      cancelGrow()
      render(opt)
    }
  },
  { deep: true },
)

/** 供父组件直接操作实例（高级用法） */
function getInstance(): echarts.ECharts | null {
  return chart
}

/** 手动同步尺寸（父级布局突变后调用） */
function resize(): void {
  chart?.resize()
}

defineExpose({ playGrow, getInstance, resize })
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
