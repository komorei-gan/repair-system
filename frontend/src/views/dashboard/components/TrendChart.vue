<script setup lang="ts">
/**
 * 工单趋势折线图（ECharts 重写版）。
 * - 两条线：新增工单（#1890FF + 渐变面积）、完成工单（#52C41A）
 * - 动效：数据从左向右生长（ECharts 折线 clipPath 入场动画）+ 面积自底部升起
 * - 首屏挂载（含浏览器 F5 刷新）与刷新按钮共用 BaseChart 的统一时序：
 *   先渲染「保留坐标系、仅 series.data 为空」的空态，
 *   再 clear() 销毁旧视图后 setOption 真实数据，
 *   使折线视图重新走「首次创建」分支，从而播放生长动画。
 *
 * ★ 空态必须保留 grid / xAxis / yAxis：
 *   坐标系一旦缺失，ECharts 会连坐标轴一起重算，折线直接从“无中生有”地出现，
 *   而不是在既有坐标系内从左向右生长。因此空态只置空 series.data。
 * ★ y 轴 min=0 / max 按数据动态向上取整：空态与真实数据共用同一轴范围，
 *   生长过程不伴随纵向跳变，也不会因写死上限截断或留白。
 */
import { computed, ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, MarkPointComponent } from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'
import BaseChart from '@/components/charts/BaseChart.vue'

echarts.use([
  LineChart,
  GridComponent,
  TooltipComponent,
  MarkPointComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
])

const props = withDefaults(
  defineProps<{
    /** 近 7 天日期标签（MM-dd） */
    days: string[]
    /** 每日新增工单数 */
    newOrders: number[]
    /** 每日完成工单数 */
    doneOrders: number[]
  }>(),
  { days: () => [], newOrders: () => [], doneOrders: () => [] },
)

/** 生长动画时长（ms），从左向右生长 + 面积升起 */
const GROW_DURATION = 1400
const GROW_EASING = 'cubicOut'

/** y 轴上限：按数据动态向上取整，避免写死导致截断或过空 */
const yMax = computed(() => {
  const peak = Math.max(...props.newOrders, ...props.doneOrders, 10)
  const step = 15
  return Math.max(step, Math.ceil(peak / step) * step)
})

/** y 轴刻度间隔（按上限等分 4 档左右） */
const yInterval = computed(() => Math.ceil(yMax.value / 4))

/**
 * 构造 option。
 * @param empty true 时仅把两条线的 data 置空，grid / xAxis / yAxis 全部保留，
 *              作为生长动画的起点（有坐标系才有“在框内生长”的参照）。
 */
function buildOption(empty = false): EChartsCoreOption {
  return {
    animation: true,
    animationDuration: GROW_DURATION,
    animationEasing: GROW_EASING,
    // 生长依赖「首次创建」的入场动画；更新阶段不再做二次补间
    animationDurationUpdate: 0,
    grid: { left: 40, right: 14, top: 44, bottom: 30 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#102B47',
      borderWidth: 0,
      padding: [10, 14],
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: { type: 'line', lineStyle: { color: '#BBD6F0', type: 'dashed' } },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.days,
      axisLine: { lineStyle: { color: '#DDE9F5' } },
      axisTick: { show: false },
      axisLabel: { color: '#8FA3B8', fontSize: 11, margin: 14 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: yMax.value,
      interval: yInterval.value,
      splitLine: { lineStyle: { color: '#ECF3FA' } },
      axisLabel: { color: '#8FA3B8', fontSize: 10.5 },
    },
    series: [
      {
        id: 'done',
        name: '完成工单',
        type: 'line',
        smooth: 0.4,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        data: empty ? [] : props.doneOrders,
        lineStyle: { width: 2.2, color: '#52C41A', opacity: 0.95 },
        itemStyle: { color: '#52C41A', borderColor: '#fff', borderWidth: 1.5 },
        emphasis: { scale: 1.6 },
        z: 2,
      },
      {
        id: 'new',
        name: '新增工单',
        type: 'line',
        smooth: 0.4,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        data: empty ? [] : props.newOrders,
        lineStyle: { width: 2.6, color: '#1890FF' },
        itemStyle: { color: '#1890FF', borderColor: '#fff', borderWidth: 2 },
        emphasis: { scale: 1.6 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24,144,255,.20)' },
              { offset: 0.7, color: 'rgba(24,144,255,.04)' },
              { offset: 1, color: 'rgba(24,144,255,0)' },
            ],
          },
        },
        // 末端圆点：最后一天标记（仅在非空态且已有数据时展示）
        markPoint: empty || props.newOrders.length === 0
          ? undefined
          : {
              symbol: 'circle',
              symbolSize: 9,
              label: { show: false },
              data: [
                {
                  name: '今日',
                  coord: [props.days.length - 1, props.newOrders[props.newOrders.length - 1]],
                  itemStyle: { color: '#1890FF', borderColor: '#fff', borderWidth: 2.5 },
                },
              ],
            },
        z: 3,
      },
    ],
  }
}

const option = computed<EChartsCoreOption>(() => buildOption(false))
const emptyOption = computed<EChartsCoreOption>(() => buildOption(true))

/** 刷新时由父组件调用，重播生长动画（与 F5 走同一条 BaseChart 时序） */
const chartRef = ref<InstanceType<typeof BaseChart>>()

function refresh(): void {
  chartRef.value?.playGrow()
}

defineExpose({ refresh })
</script>

<template>
  <BaseChart
    ref="chartRef"
    :option="option"
    :empty-option="emptyOption"
    :grow="true"
    :grow-delay="340"
    height="252px"
  />
</template>
