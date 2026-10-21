<script setup lang="ts">
/**
 * 工单状态分布环形图（design-web.html 图二 → ECharts 实现）。
 * - 数据与配色 1:1 复刻视觉稿
 * - 初次挂载与手动刷新时 showLoading 展示加载动效
 */
import { computed, ref } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, GraphicComponent } from 'echarts/components'
import { LabelLayout } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'
import type { OrderStatus } from '@/types/api'
import { STATUS_COLORS } from '@/utils/status'
import BaseChart from '@/components/charts/BaseChart.vue'

echarts.use([PieChart, TooltipComponent, GraphicComponent, LabelLayout, CanvasRenderer])

interface StatusSlice {
  status: OrderStatus
  name: string
  value: number
  pct: string
}

withDefaults(
  defineProps<{
    /** 是否展示加载动效 */
    loading?: boolean
  }>(),
  { loading: false },
)

const emit = defineEmits<{ 'ready': [] }>()

/** 与 design-web.html 图二完全一致的数据（TODO: 接 /api/admin/stats/dashboard） */
const slices = ref<StatusSlice[]>([
  { status: 'PENDING', name: '待受理', value: 18, pct: '14.1%' },
  { status: 'ASSIGNED', name: '已派单', value: 22, pct: '17.2%' },
  { status: 'PROCESSING', name: '维修中', value: 31, pct: '24.2%' },
  { status: 'COMPLETED', name: '已完成', value:19, pct: '14.8%' },
  { status: 'CONFIRMED', name: '已确认', value: 33, pct: '25.8%' },
  { status: 'CANCELLED', name: '已取消', value: 5, pct: '3.9%' },
])

const total = computed(() => slices.value.reduce((s, i) => s + i.value, 0))

const option = computed<EChartsCoreOption>(() => ({
  tooltip: {
    trigger: 'item',
    backgroundColor: '#102B47',
    borderWidth: 0,
    padding: [8, 12],
    textStyle: { color: '#fff', fontSize: 12 },
    formatter: '{b}：{c} 单（{d}%）',
  },
  graphic: [
    {
      type: 'text',
      left: 'center',
      top: '41%',
      style: {
        text: String(total.value),
        fill: '#0F2137',
        fontSize: 30,
        fontWeight: 'bold',
      },
    },
    {
      type: 'text',
      left: 'center',
      top: '55%',
      style: {
        text: '工单总数',
        fill: '#7E92A8',
        fontSize: 11,
      },
    },
    {
      type: 'text',
      left: 'center',
      top: '63%',
      style: {
        text: '完成率 92.6%',
        fill: '#2E7D0B',
        fontSize: 11,
        fontWeight: 'bold',
      },
    },
  ],
  series: [
    {
      type: 'pie',
      radius: ['62%', '84%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: { show: false },
      emphasis: {
        scaleSize: 6,
        label: { show: false },
      },
      data: slices.value.map((s) => ({
        name: s.name,
        value: s.value,
        itemStyle: { color: STATUS_COLORS[s.status] },
      })),
    },
  ],
}))

function onReady(): void {
  emit('ready')
}
</script>

<template>
  <div class="status-donut">
    <BaseChart :option="option" :loading="loading" height="196px" @chart-ready="onReady" />

    <!-- 图例（与视觉稿一致的 2 列布局） -->
    <ul class="status-list">
      <li v-for="s in slices" :key="s.status">
        <i class="dot" :style="{ background: STATUS_COLORS[s.status] }"></i>
        <span class="name">{{ s.name }}</span>
        <b class="num">{{ s.value }}</b>
        <span class="pct num">{{ s.pct }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.status-donut {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.status-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px 16px;
  width: 100%;
  margin-top: 14px;
  padding-top: 15px;
  border-top: 1px dashed #e8f0f8;
}
.status-list li {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: #5d738a;
  min-width: 0;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  flex: none;
}
.name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.status-list b {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-2);
}
.pct {
  font-size: 10.5px;
  color: #6b8095;
  min-width: 32px;
  text-align: right;
}

@media (max-width: 1180px) {
  .status-list {
    grid-template-columns: 1fr;
  }
}
@media (min-width: 781px) and (max-width: 1024px) {
  .status-list {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 780px) {
  .status-list {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 520px) {
  .status-list {
    grid-template-columns: 1fr;
  }
}
</style>
