<script setup lang="ts">
/**
 * 指标卡（含数字滚动动效）。
 * - 页面挂载：数字从 0 滚动到目标值（useCountUp，延迟与卡片入场动画错峰）
 * - 刷新信号（refreshKey 变化）：数字重新从 0 滚动
 * - 入场：slideUp 0.4s 由下往上位移 + 淡入
 */
import { watch } from 'vue'
import { useCountUp } from '@/composables/useCountUp'
import IconSvg from '@/components/IconSvg.vue'

/** 指标卡数据项（由父组件 DashboardView 按真实接口构造） */
export interface StatItem {
  label: string
  value: string
  unit?: string
  pct?: string
  /** 环比文案；后端不提供环比时省略，整行 stat-foot 不渲染 */
  deltaText?: string
  /** 辅助说明；后端不提供时省略 */
  note?: string
  icon: string
  iconCls: string
  featured?: boolean
  /** 环比类型：up 橙 / down 绿 / flat 蓝（有 deltaText 时使用） */
  deltaCls?: 'up' | 'down' | 'flat'
  progress?: number
}

const props = withDefaults(
  defineProps<{
    item: StatItem
    /** 每次自增触发一次重滚（刷新信号） */
    refreshKey: number
    /** 入场动画延迟（s） */
    delay?: number
  }>(),
  { delay: 0 },
)

/** 百分比类指标保留 1 位小数（92.6%），其余为整数 */
const decimals = props.item.pct ? 1 : 0

const { formatted, start } = useCountUp(() => parseFloat(props.item.value), {
  duration: 1100,
  decimals,
  // 数字滚动略晚于卡片入场动画，形成层次
  delay: 260 + props.delay * 1000,
})

/**
 * 数字滚动触发时机（修正首屏全为 0 的问题）：
 * - 首屏不在 onMounted 里滚，而是等接口返回、item.value 由 '0' 变为真实值后再滚；
 * - refreshKey 变化（手动刷新）时无条件重滚，即使数值未变。
 */
watch(
  () => props.item.value,
  (value, prev) => {
    // 首次拿到非 0 真实数据时滚动（避免在接口返回前滚到 0）
    if (value !== prev && parseFloat(value) > 0) {
      start()
    }
  },
)

watch(
  () => props.refreshKey,
  () => start(),
)
</script>

<template>
  <article
    class="stat-card slide-up"
    :class="{ 'is-featured': item.featured }"
    :style="{ animationDelay: delay + 's' }"
  >
    <div class="stat-top">
      <span class="stat-label">{{ item.label }}</span>
      <span class="icon-tile" :class="item.iconCls" aria-hidden="true">
        <IconSvg :name="item.icon" :size="19" />
      </span>
    </div>

    <div class="stat-value num">
      {{ formatted }}<span v-if="item.pct" class="pct">{{ item.pct }}</span
      ><span v-else class="unit">{{ item.unit }}</span>
    </div>

    <div v-if="item.progress" class="progress-line" aria-hidden="true">
      <span :style="{ width: item.progress + '%' }"></span>
    </div>

    <div v-if="item.deltaText || item.note" class="stat-foot" :style="item.progress ? 'margin-top:8px' : ''">
      <span v-if="item.deltaText" class="delta" :class="item.deltaCls">
        <span class="delta-ico" :class="{ rot: item.deltaCls === 'down' }">
          <IconSvg name="i-trending-up" :size="11" />
        </span>
        {{ item.deltaText }}
      </span>
      <span v-if="item.note" class="foot-note">{{ item.note }}</span>
    </div>
  </article>
</template>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
  padding: 19px 20px 17px;
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  transition: transform 0.25s var(--ease), box-shadow 0.25s var(--ease), border-color 0.25s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px -26px rgba(10, 42, 80, 0.4);
  border-color: #dce9f6;
}
.stat-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.stat-label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted);
  padding-top: 3px;
  letter-spacing: 0.01em;
}
.icon-tile {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  box-shadow: inset 0 0 0 1px rgba(15, 42, 74, 0.03);
}
.icon-tile.blue {
  background: linear-gradient(150deg, #eaf4ff, #daebff);
  color: var(--blue-600);
}
.icon-tile.amber {
  background: linear-gradient(150deg, #fff7e6, #ffedcc);
  color: var(--amber-600);
}
.icon-tile.green {
  background: linear-gradient(150deg, #f0fbea, #e2f8d8);
  color: var(--green-600);
}
.icon-tile.slate {
  background: linear-gradient(150deg, #f1f6fc, #e7eff9);
  color: #4c6c8a;
}
.stat-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  font-size: 33px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--ink);
  margin: 11px 0 0;
}
.stat-value .unit {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted-2);
  letter-spacing: 0;
}
.stat-value .pct {
  font-size: 18px;
  font-weight: 600;
  color: var(--muted);
  letter-spacing: -0.02em;
  margin-left: -3px;
}
.stat-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 9px;
  font-size: 11.5px;
  color: #7b8fa5;
  white-space: nowrap;
}
.delta {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-weight: 600;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 5px;
  font-variant-numeric: tabular-nums;
}
.delta-ico {
  display: inline-flex;
  transition: transform 0.3s var(--ease);
}
.delta-ico.rot {
  transform: rotate(90deg);
}
.delta.up {
  color: #a85b00;
  background: #fff6e5;
}
.delta.down {
  color: var(--green-700);
  background: var(--green-50);
}
.delta.flat {
  color: var(--blue-700);
  background: var(--blue-50);
}
.foot-note {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 焦点卡 ===== */
.stat-card.is-featured {
  border-color: #cfe4ff;
  background: radial-gradient(240px 160px at 108% -30%, rgba(24, 144, 255, 0.22), transparent 70%),
    linear-gradient(145deg, #edf6ff 0%, #f7fbff 52%, #ffffff 100%);
}
.stat-card.is-featured::before {
  content: '';
  position: absolute;
  right: -38px;
  top: -46px;
  width: 128px;
  height: 128px;
  border-radius: 50%;
  border: 1px solid rgba(24, 144, 255, 0.16);
}
.stat-card.is-featured::after {
  content: '';
  position: absolute;
  right: -16px;
  top: -24px;
  width: 86px;
  height: 86px;
  border-radius: 50%;
  border: 1px solid rgba(24, 144, 255, 0.12);
}
.stat-card.is-featured .stat-label {
  color: #3b6e9c;
  font-weight: 600;
}
.stat-card.is-featured .icon-tile {
  background: linear-gradient(150deg, #4fadff, #168dfb);
  color: #fff;
  box-shadow: 0 8px 18px -10px rgba(24, 144, 255, 0.9), inset 0 1px 0 rgba(255, 255, 255, 0.35);
}
.stat-card.is-featured .stat-value {
  color: #0b4e8c;
}
.stat-card.is-featured .delta {
  background: #fff;
  color: #0a63b8;
  box-shadow: 0 1px 2px rgba(24, 144, 255, 0.15);
}
.stat-card.is-featured .stat-foot {
  color: #5a7e9e;
}

.progress-line {
  height: 4px;
  border-radius: 99px;
  background: #e9f5df;
  margin-top: 11px;
  overflow: hidden;
}
.progress-line span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7bd94b, #52c41a);
  position: relative;
}
.progress-line span::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 16px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.65));
}

/* ===== 响应式（原 DashboardView 内的卡片适配迁移至此） ===== */
@media (max-width: 1280px) {
  .stat-card {
    padding: 17px 17px 15px;
  }
  .stat-value {
    font-size: 30px;
  }
}
@media (max-width: 780px) {
  .stat-value {
    font-size: 27px;
  }
  .stat-label {
    font-size: 11.5px;
  }
}
@media (max-width: 520px) {
  .stat-card {
    padding: 16px;
  }
}
</style>
