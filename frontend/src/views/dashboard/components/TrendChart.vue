<script setup lang="ts">
/**
 * 工单趋势手绘 SVG 折线图 —— 路径坐标 1:1 迁自 design-web.html（图一）。
 */
import { TREND_DAYS } from '../mock'

withDefaults(defineProps<{ days?: string[] }>(), {
  days: () => TREND_DAYS,
})
</script>

<template>
  <svg
    class="chart-svg"
    viewBox="0 0 720 248"
    role="img"
    aria-label="近七天工单趋势折线图：新增工单分别为 32、41、28、53、46、61、47 单，完成工单分别为 24、33、26、39、42、50、44 单"
  >
    <defs>
      <linearGradient id="areaBlue" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#1890FF" stop-opacity=".20" />
        <stop offset="70%" stop-color="#1890FF" stop-opacity=".04" />
        <stop offset="100%" stop-color="#1890FF" stop-opacity="0" />
      </linearGradient>
    </defs>

    <!-- 横向网格 -->
    <g>
      <line class="grid-line" x1="34" y1="66" x2="704" y2="66" />
      <line class="grid-line" x1="34" y1="102" x2="704" y2="102" />
      <line class="grid-line" x1="34" y1="138" x2="704" y2="138" />
      <line class="grid-line" x1="34" y1="174" x2="704" y2="174" />
      <line class="axis-line" x1="34" y1="210" x2="704" y2="210" />
    </g>

    <!-- Y 轴刻度 -->
    <g text-anchor="end">
      <text class="tick" x="24" y="69.5">60</text>
      <text class="tick" x="24" y="105.5">45</text>
      <text class="tick" x="24" y="141.5">30</text>
      <text class="tick" x="24" y="177.5">15</text>
      <text class="tick" x="24" y="213.5">0</text>
    </g>

    <!-- 面积 -->
    <path
      d="M34 133.2 C71.2 133.2,108.5 111.6,145.67 111.6 C182.8 111.6,220.2 142.8,257.33 142.8 C294.5 142.8,331.8 82.8,369 82.8 C406.2 82.8,443.5 99.6,480.67 99.6 C517.8 99.6,555.2 63.6,592.33 63.6 C629.5 63.6,666.8 97.2,704 97.2 L704 210 L34 210 Z"
      fill="url(#areaBlue)"
    />

    <!-- 完成工单 -->
    <path
      class="line-done"
      d="M34 152.4 C71.2 152.4,108.5 130.8,145.67 130.8 C182.8 130.8,220.2 147.6,257.33 147.6 C294.5 147.6,331.8 116.4,369 116.4 C406.2 116.4,443.5 109.2,480.67 109.2 C517.8 109.2,555.2 90,592.33 90 C629.5 90,666.8 104.4,704 104.4"
    />

    <!-- 新增工单 -->
    <path
      class="line-new"
      d="M34 133.2 C71.2 133.2,108.5 111.6,145.67 111.6 C182.8 111.6,220.2 142.8,257.33 142.8 C294.5 142.8,331.8 82.8,369 82.8 C406.2 82.8,443.5 99.6,480.67 99.6 C517.8 99.6,555.2 63.6,592.33 63.6 C629.5 63.6,666.8 97.2,704 97.2"
    />

    <!-- 峰值标注 -->
    <g>
      <line class="guide" x1="592.33" y1="76" x2="592.33" y2="210" />
      <circle cx="592.33" cy="63.6" r="4.5" fill="#fff" stroke="#1890FF" stroke-width="2.6" />
      <g class="chart-tip">
        <rect x="552" y="24" width="80" height="26" rx="8" />
        <text x="592" y="41" text-anchor="middle">61 单 · 峰值</text>
      </g>
    </g>

    <!-- 末端圆点 -->
    <circle class="end-dot" cx="704" cy="97.2" r="4" stroke="#1890FF" />
    <circle class="end-dot" cx="704" cy="104.4" r="4" stroke="#52C41A" />

    <!-- X 轴标签 -->
    <g text-anchor="middle">
      <text v-for="(d, i) in days" :key="d" class="tick" :x="i === 0 ? 34 : i * 111.67 + 34" y="236">
        {{ d }}
      </text>
    </g>
  </svg>
</template>

<style scoped>
.chart-svg {
  width: 100%;
  height: auto;
  overflow: visible;
}
.chart-svg text {
  font-family: inherit;
  font-size: 10.5px;
  fill: #93a7bb;
}
.chart-svg .grid-line {
  stroke: #ecf3fa;
  stroke-width: 1;
}
.chart-svg .axis-line {
  stroke: #dde9f5;
  stroke-width: 1;
}
.chart-svg .tick {
  fill: #8fa3b8;
}
.chart-svg .line-new {
  fill: none;
  stroke: #1890ff;
  stroke-width: 2.6;
  stroke-linecap: round;
}
.chart-svg .line-done {
  fill: none;
  stroke: #52c41a;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-opacity: 0.95;
}
.chart-svg .end-dot {
  fill: #fff;
  stroke-width: 2.4;
}
.chart-svg .guide {
  stroke: #bbd6f0;
  stroke-dasharray: 3 4;
  stroke-width: 1;
}
.chart-tip rect {
  fill: #102b47;
}
.chart-tip text {
  fill: #fff;
  font-size: 10.5px;
  font-weight: 600;
}
</style>
