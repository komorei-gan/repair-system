<script setup lang="ts">
/**
 * 数据看板 —— 结构与样式 1:1 复刻 design-web.html。
 * 指标卡/趋势图/表格数据为 mock（TODO：接 /api/admin/stats/dashboard 与 /api/admin/orders）。
 * 环形图使用 ECharts（StatusDonut），支持 showLoading 刷新动效。
 */
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MOCK_ORDERS, STAT_ITEMS, STATUS_TABS, type MockOrderRow } from './mock'
import { orderStatusCls, orderStatusLabel } from '@/utils/status'
import IconSvg from '@/components/IconSvg.vue'
import TrendChart from './components/TrendChart.vue'
import StatusDonut from './components/StatusDonut.vue'

/* ---------- 分段控件 ---------- */
const rangeTabs = ['今日', '本周', '本月']
const activeRange = ref('本周')
const trendRange = ref('近 7 天')

/* ---------- 环形图刷新动效 ---------- */
const donutLoading = ref(true)

/** 拉取状态分布（TODO: 替换为 GET /api/admin/stats/dashboard） */
async function fetchDonut(): Promise<void> {
  donutLoading.value = true
  await new Promise((r) => setTimeout(r, 900))
  donutLoading.value = false
}

/** 首次挂载即展示加载动效 */
onMounted(() => void fetchDonut())

/** 点击刷新：先 showLoading，模拟拉取后更新 */
async function onRefresh(): Promise<void> {
  await fetchDonut()
  ElMessage.success('数据已刷新')
}

/* ---------- 工单表格 ---------- */
const orders = ref<MockOrderRow[]>(MOCK_ORDERS)
const activeTab = ref('全部')

function onTab(label: string): void {
  activeTab.value = label
  // TODO: 按状态筛选调用 GET /api/admin/orders?status=xxx
}

/** 行操作（视觉稿仅交互提示） */
function onAction(row: MockOrderRow, action: string): void {
  const statusLabel = orderStatusLabel(row.status)
  if (action === 'detail') {
    ElMessage.info(`查看工单 ${row.orderNo}（${statusLabel}）详情 —— 第二阶段接入`)
  } else {
    ElMessage.info(`「${action}」操作将在第二阶段接入真实接口（当前状态：${statusLabel}）`)
  }
}
</script>

<template>
  <div class="dashboard">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>
          报修工单看板
          <span class="live-chip"><i></i>实时</span>
        </h1>
        <p class="page-sub">
          <span>2026 年 10 月 21 日 · 周三</span>
          <span class="sep">/</span>
          <span>校园后勤保障中心</span>
          <span class="sep">/</span>
          <span>今日已处理 39 单</span>
        </p>
      </div>
      <div class="page-actions">
        <div class="seg" role="tablist" aria-label="统计时间范围">
          <button
            v-for="t in rangeTabs"
            :key="t"
            type="button"
            role="tab"
            :aria-selected="activeRange === t"
            @click="activeRange = t"
          >
            {{ t }}
          </button>
        </div>
        <button type="button" class="btn btn-ghost btn-icon" aria-label="刷新数据" @click="onRefresh">
          <IconSvg name="i-refresh" :size="15" />
        </button>
        <button type="button" class="btn btn-primary">
          <IconSvg name="i-download" :size="15" />
          导出报表
        </button>
      </div>
    </div>

    <!-- ============ 指标卡 ============ -->
    <section class="stats" aria-label="核心指标">
      <article
        v-for="(s, i) in STAT_ITEMS"
        :key="s.label"
        class="stat-card rise"
        :class="{ 'is-featured': s.featured }"
        :style="{ animationDelay: i * 0.05 + 's' }"
      >
        <div class="stat-top">
          <span class="stat-label">{{ s.label }}</span>
          <span class="icon-tile" :class="s.iconCls" aria-hidden="true">
            <IconSvg :name="s.icon" :size="19" />
          </span>
        </div>
        <div class="stat-value num">
          {{ s.value }}<span v-if="s.pct" class="pct">{{ s.pct }}</span
          ><span v-else class="unit">{{ s.unit }}</span>
        </div>
        <div v-if="s.progress" class="progress-line" aria-hidden="true">
          <span :style="{ width: s.progress + '%' }"></span>
        </div>
        <div class="stat-foot" :style="s.progress ? 'margin-top:8px' : ''">
          <span class="delta" :class="s.deltaCls">
            <IconSvg
              name="i-trending-up"
              :size="11"
              :style="s.deltaCls === 'down' ? 'transform: rotate(90deg)' : ''"
            />
            {{ s.deltaText }}
          </span>
          <span class="foot-note">{{ s.note }}</span>
        </div>
      </article>
    </section>

    <!-- ============ 趋势 + 状态分布 ============ -->
    <section class="insight-grid" aria-label="趋势与分布">
      <article class="card chart-card rise" style="animation-delay: 0.12s">
        <div class="card-head">
          <div>
            <h2>工单趋势</h2>
            <p>近 7 天新增与完成工单对比</p>
          </div>
          <div class="seg mini" role="tablist" aria-label="趋势周期">
            <button
              v-for="t in ['近 7 天', '近 30 天']"
              :key="t"
              type="button"
              role="tab"
              :aria-selected="trendRange === t"
              @click="trendRange = t"
            >
              {{ t }}
            </button>
          </div>
        </div>

        <div class="legend-row">
          <span class="legend"><i class="dot blue"></i>新增工单</span>
          <span class="legend"><i class="dot green"></i>完成工单</span>
          <span class="legend-note">单位：单</span>
        </div>

        <div class="chart-wrap">
          <TrendChart />
        </div>
      </article>

      <article class="card status-card rise" style="animation-delay: 0.18s">
        <div class="card-head">
          <div>
            <h2>工单状态分布</h2>
            <p>本周全部工单构成</p>
          </div>
          <span class="chip">共 128 单</span>
        </div>

        <StatusDonut :loading="donutLoading" />
      </article>
    </section>

    <!-- ============ 工单列表 ============ -->
    <section class="card table-card rise" style="animation-delay: 0.2s" aria-labelledby="orderListTitle">
      <div class="table-head">
        <div class="title-wrap">
          <h2 id="orderListTitle">工单列表</h2>
          <p>共 <b>128</b> 条工单 · <b>18</b> 条待受理，建议优先派单</p>
        </div>
        <div class="toolbar">
          <div class="search-field">
            <IconSvg name="i-search" :size="15" />
            <input type="search" placeholder="搜索工单号 / 标题 / 报修人" aria-label="搜索工单" />
          </div>
          <button type="button" class="select-btn">
            <IconSvg name="i-filter" :size="14" />
            故障分类
            <IconSvg name="i-chevron-down" :size="12" />
          </button>
          <button type="button" class="select-btn">
            <IconSvg name="i-calendar" :size="14" />
            近 7 天
            <IconSvg name="i-chevron-down" :size="12" />
          </button>
          <button type="button" class="btn btn-primary btn-sm">
            <IconSvg name="i-send" :size="14" />
            批量派单
          </button>
        </div>
      </div>

      <div class="tabs" role="tablist" aria-label="按状态筛选工单">
        <button
          v-for="t in STATUS_TABS"
          :key="t.label"
          type="button"
          class="tab"
          role="tab"
          :aria-selected="activeTab === t.label"
          @click="onTab(t.label)"
        >
          {{ t.label }}<span class="count">{{ t.count }}</span>
        </button>
      </div>

      <div class="table-wrap">
        <table>
          <caption class="sr-only">
            报修工单列表，包含工单号、故障描述、分类、报修人、维修师傅、状态、创建时间与操作
          </caption>
          <thead>
            <tr>
              <th scope="col">
                <label class="check"><input type="checkbox" aria-label="全选当前页工单" /></label>
              </th>
              <th scope="col">工单 / 故障描述</th>
              <th scope="col">分类</th>
              <th scope="col">报修人</th>
              <th scope="col">维修师傅</th>
              <th scope="col">状态</th>
              <th scope="col">创建时间</th>
              <th scope="col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in orders" :key="row.orderNo">
              <td>
                <label class="check"
                  ><input type="checkbox" :aria-label="`选择工单 ${row.orderNo}`"
                /></label>
              </td>
              <td class="order-cell">
                <div class="order-no">
                  {{ row.orderNo }}
                  <span v-if="row.urgent" class="tag-urgent">紧急</span>
                </div>
                <span class="order-title">{{ row.title }}</span>
                <span class="order-addr">
                  <IconSvg name="i-pin" :size="11" />
                  {{ row.address }}
                </span>
              </td>
              <td><span class="cat" :class="row.catCls"><i></i>{{ row.category }}</span></td>
              <td>
                <div class="person">
                  <span class="avatar" :class="row.reporter.avatarCls">{{ row.reporter.avatar }}</span>
                  <span>{{ row.reporter.name }}</span>
                </div>
              </td>
              <td>
                <div v-if="row.repairer" class="person">
                  <span class="avatar" :class="row.repairer.avatarCls">{{
                    row.repairer.name.charAt(0)
                  }}</span>
                  <span>{{ row.repairer.name }}</span>
                </div>
                <span v-else class="unassigned">
                  <IconSvg name="i-plus" :size="11" />
                  待分配
                </span>
              </td>
              <td>
                <span class="pill" :class="orderStatusCls(row.status)">
                  <i class="dot"></i>{{ orderStatusLabel(row.status) }}
                </span>
              </td>
              <td class="time-cell">
                {{ row.date }}<small>{{ row.time }}</small>
              </td>
              <td>
                <div class="row-actions">
                  <template v-if="row.status === 'PENDING'">
                    <button type="button" class="act act-primary" @click="onAction(row, '派单')">
                      派单
                    </button>
                  </template>
                  <template v-else-if="row.status === 'ASSIGNED'">
                    <button type="button" class="act act-primary" @click="onAction(row, '改派')">
                      改派
                    </button>
                  </template>
                  <template v-else-if="row.status === 'PROCESSING'">
                    <button type="button" class="act act-primary" @click="onAction(row, '跟进')">
                      跟进
                    </button>
                  </template>
                  <button type="button" class="act" @click="onAction(row, 'detail')">详情</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-foot">
        <span class="foot-info">共 <b>128</b> 条工单，本页显示 <b>6</b> 条</span>
        <nav class="pager" aria-label="分页">
          <button type="button" aria-label="上一页" disabled style="opacity: 0.45; cursor: not-allowed">
            <IconSvg name="i-chevron-left" :size="14" />
          </button>
          <button type="button" aria-current="page">1</button>
          <button type="button">2</button>
          <button type="button">3</button>
          <span class="gap">···</span>
          <button type="button">22</button>
          <button type="button" aria-label="下一页">
            <IconSvg name="i-chevron-right" :size="14" />
          </button>
        </nav>
      </div>
    </section>

    <footer class="page-foot">
      <span class="dot" aria-hidden="true"></span>
      数据每 30 秒自动刷新 · 最后同步 14:36:20
    </footer>
  </div>
</template>

<style scoped>
.dashboard {
  min-width: 0;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}
.page-head h1 {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.2;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 10px;
}
.live-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--green-700);
  background: var(--green-50);
  border: 1px solid #ddf4d2;
}
.live-chip i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--green-500);
  box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.14);
}
.page-sub {
  font-size: 13px;
  color: var(--muted);
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.page-sub .sep {
  color: #c6d4e2;
}
.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ===== 按钮 ===== */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 38px;
  padding: 0 15px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.18s var(--ease), box-shadow 0.18s, background 0.18s, border-color 0.18s,
    color 0.18s;
}
.btn-primary {
  color: #fff;
  background: linear-gradient(180deg, #35a0ff 0%, #1890ff 55%, #0f7ee4 100%);
  box-shadow: 0 8px 18px -10px rgba(24, 144, 255, 0.95), inset 0 1px 0 rgba(255, 255, 255, 0.28);
}
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px -10px rgba(24, 144, 255, 0.9), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.btn-ghost {
  background: #fff;
  border-color: var(--line-strong);
  color: #3e5872;
  box-shadow: var(--shadow-xs);
}
.btn-ghost:hover {
  border-color: var(--blue-200);
  color: var(--blue-700);
  background: #f8fcff;
}
.btn-icon {
  width: 38px;
  padding: 0;
}
.btn-sm {
  height: 34px;
  padding: 0 12px;
  font-size: 12.5px;
  border-radius: 9px;
}

/* ===== 分段控件 ===== */
.seg {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  background: #eaf1f9;
  border-radius: 10px;
}
.seg button {
  height: 30px;
  padding: 0 13px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #57708a;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s, color 0.18s, box-shadow 0.18s;
}
.seg button:hover {
  color: var(--ink-2);
}
.seg button[aria-selected='true'] {
  background: #fff;
  color: var(--ink);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(15, 42, 74, 0.14);
}
.seg.mini {
  padding: 2px;
  border-radius: 9px;
  background: #f0f5fb;
}
.seg.mini button {
  height: 26px;
  padding: 0 10px;
  font-size: 11.5px;
  border-radius: 7px;
}

/* ===== 指标卡 ===== */
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}
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

/* 焦点卡 */
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

/* ===== 图表行 ===== */
.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(0, 1fr);
  gap: 18px;
  margin-bottom: 18px;
  align-items: stretch;
}
.chart-card,
.status-card {
  padding: 20px 22px 18px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}
.card-head h2 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.015em;
  line-height: 1.35;
}
.card-head p {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 2px;
}
.status-card .chip {
  font-size: 10.5px;
  font-weight: 600;
  color: #5e7b96;
  background: #f3f7fc;
  border: 1px solid #e8f0f9;
  padding: 3px 8px;
  border-radius: 6px;
  white-space: nowrap;
}
.legend-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 13px 0 4px;
  font-size: 11.5px;
  color: #5c7288;
}
.legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.legend .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: none;
}
.legend .dot.blue {
  background: var(--blue-500);
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.13);
}
.legend .dot.green {
  background: var(--green-500);
  box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.14);
}
.legend-note {
  margin-left: auto;
  color: #6b8095;
  font-size: 10.5px;
}
.chart-wrap {
  margin-top: auto;
  padding-top: 4px;
}

/* ===== 表格 ===== */
.table-card {
  overflow: hidden;
}
.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 22px 16px;
}
.table-head .title-wrap {
  min-width: 0;
}
.table-head h2 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.015em;
}
.table-head p {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 2px;
}
.table-head p b {
  color: var(--blue-700);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}
.search-field {
  position: relative;
}
.search-field :deep(.ico) {
  position: absolute;
  left: 11px;
  top: 50%;
  transform: translateY(-50%);
  color: #8fa3b8;
  pointer-events: none;
}
.search-field input {
  width: 224px;
  height: 38px;
  padding: 0 13px 0 34px;
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  background: #fafdff;
  font-size: 12.5px;
  color: var(--ink);
  transition: border-color 0.18s, background 0.18s, box-shadow 0.18s;
}
.search-field input::placeholder {
  color: #9aadc1;
}
.search-field input:focus {
  outline: none;
  border-color: var(--blue-500);
  background: #fff;
  box-shadow: var(--ring);
}
.select-btn {
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: #fff;
  font-size: 12.5px;
  font-weight: 500;
  color: #4c647c;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.18s, color 0.18s, background 0.18s;
}
.select-btn:hover {
  border-color: var(--blue-200);
  color: var(--blue-700);
  background: #fafdff;
}
.select-btn :deep(.ico:nth-child(2)) {
  color: #93a7bb;
}

.tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 16px;
  border-top: 1px solid #f2f7fc;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
  scrollbar-width: none;
}
.tabs::-webkit-scrollbar {
  display: none;
}
.tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 46px;
  padding: 0 11px;
  border: 0;
  background: none;
  cursor: pointer;
  font-size: 12.5px;
  font-weight: 500;
  color: #5e7489;
  white-space: nowrap;
  transition: color 0.18s;
}
.tab:hover {
  color: var(--blue-700);
}
.tab .count {
  font-size: 10.5px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  padding: 1px 6px;
  border-radius: 5px;
  background: #f1f5fa;
  color: #73879c;
  transition: background 0.18s, color 0.18s;
}
.tab[aria-selected='true'] {
  color: var(--blue-700);
  font-weight: 600;
}
.tab[aria-selected='true'] .count {
  background: var(--blue-50);
  color: var(--blue-700);
}
.tab[aria-selected='true']::after {
  content: '';
  position: absolute;
  left: 9px;
  right: 9px;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: var(--blue-500);
}

.table-wrap {
  overflow-x: auto;
}
table {
  width: 100%;
  min-width: 1060px;
  border-collapse: collapse;
}
thead th {
  padding: 10px 14px;
  text-align: left;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.07em;
  color: #8298ae;
  background: #f8fbff;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}
thead th:first-child {
  padding-left: 22px;
  width: 46px;
}
thead th:last-child {
  padding-right: 22px;
  text-align: right;
}
tbody td {
  padding: 14px;
  border-bottom: 1px solid #f0f6fc;
  vertical-align: middle;
  font-size: 12.5px;
  color: #3e5872;
}
tbody td:first-child {
  padding-left: 22px;
}
tbody td:last-child {
  padding-right: 22px;
  text-align: right;
}
tbody tr {
  transition: background 0.16s;
}
tbody tr:hover {
  background: #f8fbff;
}
tbody tr:last-child td {
  border-bottom: 0;
}

.check {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}
.check input {
  appearance: none;
  -webkit-appearance: none;
  width: 16px;
  height: 16px;
  border: 1.5px solid #c9d9e8;
  border-radius: 5px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  position: relative;
  display: block;
}
.check input:hover {
  border-color: #8fbef0;
}
.check input:checked {
  background: var(--blue-500);
  border-color: var(--blue-500);
}
.check input:checked::after {
  content: '';
  position: absolute;
  left: 4.5px;
  top: 1.5px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(42deg);
}

.order-cell {
  min-width: 250px;
}
.order-no {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 10.5px;
  color: #6b8095;
  letter-spacing: 0.01em;
  line-height: 1.4;
}
.order-no .tag-urgent {
  font-family: inherit;
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #b25e00;
  background: #fff4e3;
  border: 1px solid #ffe2b8;
  padding: 0 4px;
  border-radius: 3px;
  line-height: 14px;
}
.order-title {
  display: block;
  margin-top: 3px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
  line-height: 1.45;
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.16s;
}
tr:hover .order-title {
  color: var(--blue-700);
}
.order-addr {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 3px;
  font-size: 10.5px;
  color: #6b8095;
  line-height: 1.4;
}
.order-addr :deep(.ico) {
  flex: none;
  color: #8fa3b8;
}

.cat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 9px;
  border-radius: 6px;
  background: #f6f9fd;
  border: 1px solid #edf3fa;
  font-size: 11.5px;
  color: #4f6a84;
  white-space: nowrap;
}
.cat i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex: none;
}
.cat.c-blue i {
  background: #1890ff;
}
.cat.c-cyan i {
  background: #13c2c2;
}
.cat.c-green i {
  background: #52c41a;
}
.cat.c-amber i {
  background: #faad14;
}
.cat.c-steel i {
  background: #6e8ba8;
}

.person {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.avatar {
  width: 28px;
  height: 28px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}
.av-1 {
  background: linear-gradient(140deg, #66b6ff, #0e7be0);
}
.av-2 {
  background: linear-gradient(140deg, #59d8c8, #0fa396);
}
.av-3 {
  background: linear-gradient(140deg, #95dd73, #4caf17);
}
.av-4 {
  background: linear-gradient(140deg, #ffc96b, #e79400);
}
.av-5 {
  background: linear-gradient(140deg, #7fa9cc, #4c7391);
}
.unassigned {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 8px;
  border: 1px dashed #cfddeb;
  border-radius: 7px;
  font-size: 11px;
  color: #93a7bb;
  background: #fbfdff;
  white-space: nowrap;
}

/* 状态胶囊 */
.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 25px;
  padding: 0 9px;
  border-radius: var(--r-pill);
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}
.pill .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex: none;
}
.pill.pending {
  background: #e9f4ff;
  color: #0a63b8;
}
.pill.pending .dot {
  background: #1890ff;
  box-shadow: 0 0 0 2.5px rgba(24, 144, 255, 0.14);
}
.pill.assigned {
  background: #f0f5fb;
  color: #4a6b8a;
}
.pill.assigned .dot {
  background: #8aa9c4;
}
.pill.processing {
  background: #fff6e5;
  color: #a85b00;
}
.pill.processing .dot {
  background: #faad14;
  box-shadow: 0 0 0 2.5px rgba(250, 173, 20, 0.16);
}
.pill.completed {
  background: #f2fbea;
  color: #427811;
}
.pill.completed .dot {
  background: #73c22e;
}
.pill.confirmed {
  background: #e9f9e2;
  color: #2e7d0b;
}
.pill.confirmed .dot {
  background: #52c41a;
}
.pill.cancelled {
  background: #f3f6fa;
  color: #6a7f94;
}
.pill.cancelled .dot {
  background: #b9c8d6;
}
.pill.rejected {
  background: #fff1f0;
  color: #cf1322;
}
.pill.rejected .dot {
  background: #ff4d4f;
}

.time-cell {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  color: #4e6880;
  font-size: 12px;
}
.time-cell small {
  display: block;
  font-size: 10.5px;
  color: #6b8095;
  margin-top: 1px;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  justify-content: flex-end;
}
.act {
  height: 30px;
  padding: 0 9px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: #5e7489;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.16s, color 0.16s, box-shadow 0.16s;
}
.act:hover {
  background: #f0f7ff;
  color: var(--blue-700);
}
.act-primary {
  background: var(--blue-50);
  color: var(--blue-700);
  font-weight: 600;
}
.act-primary:hover {
  background: var(--blue-500);
  color: #fff;
  box-shadow: 0 7px 14px -8px rgba(24, 144, 255, 0.9);
}

.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 13px 22px;
  border-top: 1px solid #f2f7fc;
  background: #fcfefe;
}
.foot-info {
  font-size: 11.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}
.foot-info b {
  color: #415c76;
  font-weight: 600;
}
.pager {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pager button {
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  font-size: 12px;
  color: #5e7489;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
  transition: background 0.16s, border-color 0.16s, color 0.16s;
  display: inline-grid;
  place-items: center;
}
.pager button:hover {
  background: #f2f8ff;
  color: var(--blue-700);
}
.pager button[aria-current='page'] {
  background: var(--blue-500);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 5px 12px -7px rgba(24, 144, 255, 0.9);
}
.pager .gap {
  color: #a5b6c8;
  font-size: 11px;
  padding: 0 2px;
}

.page-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 20px 0 2px;
  font-size: 10.5px;
  color: #6b8095;
  letter-spacing: 0.01em;
}
.page-foot .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--green-500);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 1280px) {
  .stats {
    gap: 14px;
  }
  .stat-card {
    padding: 17px 17px 15px;
  }
  .stat-value {
    font-size: 30px;
  }
}
@media (max-width: 1180px) {
  .insight-grid {
    grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  }
}
@media (max-width: 1024px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 780px) {
  .page-head {
    align-items: flex-start;
    gap: 14px;
    margin-bottom: 18px;
  }
  .page-head h1 {
    font-size: 23px;
  }
  .page-sub {
    font-size: 12px;
  }
  .page-actions {
    width: 100%;
  }
  .page-actions .seg {
    flex: 1;
  }
  .page-actions .seg button {
    flex: 1;
    padding: 0 8px;
  }
  .stats {
    gap: 12px;
  }
  .stat-value {
    font-size: 27px;
  }
  .stat-label {
    font-size: 11.5px;
  }
  .table-head {
    padding: 16px;
  }
  .table-head .toolbar {
    width: 100%;
  }
  .search-field {
    flex: 1;
    min-width: 170px;
  }
  .search-field input {
    width: 100%;
  }
  .table-foot {
    padding: 12px 16px;
    justify-content: center;
  }
  .foot-info {
    width: 100%;
    text-align: center;
  }
  .insight-grid {
    gap: 12px;
  }
  .chart-card,
  .status-card {
    padding: 16px;
  }
  .order-title {
    max-width: 210px;
  }
}
@media (max-width: 520px) {
  .stats {
    grid-template-columns: 1fr;
  }
  .stat-card {
    padding: 16px;
  }
  .page-head h1 {
    font-size: 21px;
  }
  .live-chip {
    display: none;
  }
  .table-head h2 {
    font-size: 15px;
  }
}
</style>
