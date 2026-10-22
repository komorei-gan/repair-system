<script setup lang="ts">
/**
 * 工单状态分布环形图（design-web.html 图二 → ECharts 实现）。
 * 动效方案（无遮罩）：
 * - 首屏挂载（含浏览器 F5 刷新）：BaseChart 统一时序 —— 先空数据，
 *   延迟 320ms 后切入真实数据，pie 从 0 度顺时针生长到目标比例
 * - 手动刷新：父组件调用 playGrow()，瞬时清空 → 下一帧塞回真实数据，
 *   复用同一生长动画
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

/** 状态分布切片（由父组件按真实接口 statusDistribution 构造） */
export interface StatusSlice {
  status: OrderStatus
  name: string
  value: number
  /** 百分比数值（0~100），展示时补 % */
  pct: number
}

const props = withDefaults(
  defineProps<{
    /** 状态分布切片（固定 7 态） */
    items: StatusSlice[]
    /** 圆心文案：工单总数 */
    totalText?: string
    /** 圆心文案：完成率 */
    completionText?: string
  }>(),
  { totalText: '0', completionText: '完成率 0.0%' },
)

const slices = computed(() => props.items)

/** 环形生长动画时长（ms） */
const GROW_DURATION = 1300
const GROW_EASING = 'cubicOut'

/**
 * 构造 option。
 * @param data 为空数组时表示「空态」，作为生长动画的起点。
 */
function buildOption(data: StatusSlice[]): EChartsCoreOption {
  return {
    animationDuration: GROW_DURATION,
    animationEasing: GROW_EASING,
    // 更新阶段不再二次补间，生长动画由「空 → 满」的入场动画完成
    animationDurationUpdate: 0,
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
          text: props.totalText,
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
          text: props.completionText,
          fill: '#2E7D0B',
          fontSize: 11,
          fontWeight: 'bold',
        },
      },
    ],
    series: [
      {
        id: 'status-donut',
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
        data: data.map((s) => ({
          name: s.name,
          value: s.value,
          itemStyle: { color: STATUS_COLORS[s.status] },
        })),
      },
    ],
  }
}

const option = computed<EChartsCoreOption>(() => buildOption(slices.value))
const emptyOption = computed<EChartsCoreOption>(() => buildOption([]))

/** 刷新时由父组件调用，重播生长动画 */
const chartRef = ref<InstanceType<typeof BaseChart>>()

function refresh(): void {
  chartRef.value?.playGrow()
}

defineExpose({ refresh })
</script>

<template>
  <div class="status-donut">
    <BaseChart
      ref="chartRef"
      :option="option"
      :empty-option="emptyOption"
      :grow="true"
      :grow-delay="320"
      height="196px"
    />

    <!-- 图例（与视觉稿一致的 2 列布局） -->
    <ul class="status-list">
      <li v-for="s in slices" :key="s.status">
        <i class="dot" :style="{ background: STATUS_COLORS[s.status] }"></i>
        <span class="name">{{ s.name }}</span>
        <b class="num">{{ s.value }}</b>
        <span class="pct num">{{ s.pct }}%</span>
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
