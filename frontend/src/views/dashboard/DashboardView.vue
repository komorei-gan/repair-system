<script setup lang="ts">
/**
 * 数据看板 —— 结构与样式 1:1 复刻 design-web.html。
 * 数据来源：GET /api/admin/stats/dashboard（指标卡/环形图/折线图）与
 * GET /api/admin/orders?pageSize=5（最新 5 条工单）。
 * 动效：
 * - 指标卡：useCountUp 数字滚动（挂载与刷新时重滚）
 * - 环形图：ECharts 原生过渡，刷新时色块从 0 度顺时针生长（无遮罩）
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDashboardStats } from '@/api/dashboard'
import { adminOrders } from '@/api/order'
import { listCategories } from '@/api/category'
import type { Category, OrderStatus, OrderVO } from '@/types/api'
import { orderStatusCls, orderStatusLabel } from '@/utils/status'
import IconSvg from '@/components/IconSvg.vue'
import AssignDialog from '@/components/AssignDialog.vue'
import TrendChart from './components/TrendChart.vue'
import StatusDonut from './components/StatusDonut.vue'
import StatCard, { type StatItem } from './components/StatCard.vue'

const router = useRouter()

/* ---------- 分段控件（本轮仅「今日/本周/本月」占位，不影响数据口径） ---------- */
const rangeTabs = ['今日', '本周', '本月']
const activeRange = ref('本周')
const trendRange = ref('近 7 天')

/* ---------- 刷新信号：驱动指标卡数字重滚 ---------- */
const refreshKey = ref(0)

/* ---------- 图表实例（刷新时重播生长动画） ---------- */
const donutRef = ref<InstanceType<typeof StatusDonut>>()
const trendRef = ref<InstanceType<typeof TrendChart>>()

/* ---------- 看板统计与最新工单 ---------- */
const totalOrders = ref(0)
const pendingCount = ref(0)
const processingCount = ref(0)
const completionRate = ref(0)
const statusDistribution = ref<{ status: OrderStatus; name: string; count: number; percentage: number }[]>([])
const trendDays = ref<string[]>([])
const trendNew = ref<number[]>([])
const trendDone = ref<number[]>([])
const orders = ref<OrderVO[]>([])
const orderTotal = ref(0)

/** 拉取看板统计 + 最新 5 条工单 */
async function fetchData(): Promise<void> {
  const [stats, orderPage] = await Promise.all([
    getDashboardStats(),
    adminOrders({ pageNum: orderPageNum.value, pageSize: orderPageSize.value }),
  ])

  totalOrders.value = stats.totalOrders
  pendingCount.value = stats.pendingCount
  processingCount.value = stats.processingCount
  completionRate.value = stats.completionRate
  statusDistribution.value = stats.statusDistribution
  trendDays.value = stats.trend.map((t) => t.date)
  trendNew.value = stats.trend.map((t) => t.newCount)
  trendDone.value = stats.trend.map((t) => t.doneCount)

  orders.value = orderPage.records
  orderTotal.value = orderPage.total
}

/** 点击刷新：数字重滚 + 环形图与折线图重新生长 */
async function onRefresh(): Promise<void> {
  await fetchData()
  refreshKey.value += 1
  donutRef.value?.refresh()
  trendRef.value?.refresh()
  ElMessage.success('数据已刷新')
}

/** 切换趋势周期：近 7 天保持当前数据，近 30 天暂未实现（二期迭代） */
function onTrendRange(range: string): void {
  if (range === '近 30 天') {
    ElMessage.info('该功能将在二期迭代中上线')
    return
  }
  trendRange.value = range
}

/** 批量派单：暂未实现（二期迭代） */
function onBatchAssign(): void {
  ElMessage.info('该功能将在二期迭代中上线')
}

/* ---------- 指标卡数据（严格对应后端 totalOrders/pendingCount/processingCount/completionRate） ---------- */
const statItems = computed<StatItem[]>(() => [
  {
    label: '总工单数',
    value: String(totalOrders.value),
    unit: '单',
    icon: 'i-inbox',
    iconCls: 'blue',
    featured: true,
  },
  {
    label: '待处理工单',
    value: String(pendingCount.value),
    unit: '单',
    icon: 'i-clock',
    iconCls: 'amber',
  },
  {
    label: '维修中',
    value: String(processingCount.value),
    unit: '单',
    icon: 'i-wrench',
    iconCls: 'slate',
  },
  {
    label: '本周完成率',
    value: completionRate.value.toFixed(1),
    pct: '%',
    icon: 'i-check-circle',
    iconCls: 'green',
    progress: completionRate.value,
  },
])

/* ---------- 环形图与折线图 props ---------- */
const donutItems = computed(() =>
  statusDistribution.value.map((i) => ({
    status: i.status,
    name: i.name,
    value: i.count,
    pct: i.percentage,
  })),
)

const donutTotalText = computed(() => String(totalOrders.value))
const donutCompletionText = computed(() => `完成率 ${completionRate.value.toFixed(1)}%`)

/* ---------- 页头文案：今日日期与今日新增 ---------- */
const todayLabel = computed(() => {
  const now = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 · ${weekdays[now.getDay()]}`
})

/** 今日新增数（趋势数组最后一项 newCount） */
const todayNew = computed(() => trendNew.value[trendNew.value.length - 1] ?? 0)

/** 待受理条数（供列表副标题） */
const pendingCountLabel = computed(() => pendingCount.value)

/* ---------- 工单表格 ---------- */
const activeTab = ref('全部')

/* ---------- 工单筛选（看板内列表） ---------- */
const orderKeyword = ref('')
const orderCategoryId = ref<number | undefined>(undefined)
const categories = ref<Category[]>([])

/* ---------- 工单分页（看板内最新工单，pageSize 固定 5） ---------- */
const orderPageNum = ref(1)
const orderPageSize = ref(5)

/** 当前激活 Tab 对应的状态码（「全部」→ undefined） */
function currentStatus(): OrderStatus | undefined {
  const tab = statusTabs.value.find((t) => t.label === activeTab.value)
  return tab?.status
}

/** 加载看板工单列表（带筛选条件 + 分页） */
async function loadOrders(): Promise<void> {
  const page = await adminOrders({
    pageNum: orderPageNum.value,
    pageSize: orderPageSize.value,
    status: currentStatus(),
    categoryId: orderCategoryId.value,
    keyword: orderKeyword.value.trim() || undefined,
  })
  orders.value = page.records
  orderTotal.value = page.total
}

/** 切换看板工单列表页码 */
function onOrderPageChange(page: number): void {
  orderPageNum.value = page
  void loadOrders()
}

/** 搜索框回车 / 清空：重置页码为 1 后查询 */
function onOrderSearch(): void {
  orderPageNum.value = 1
  void loadOrders()
}

/** 切换故障分类：重置页码为 1 后查询 */
function onCategoryChange(): void {
  orderPageNum.value = 1
  void loadOrders()
}

/** 切换状态 Tab：重置页码为 1 后按状态查询 */
function onTab(label: string): void {
  activeTab.value = label
  orderPageNum.value = 1
  void loadOrders()
}

/** 状态 Tab：由状态分布真实计数构造（全部 + 7 态） */
const statusTabs = computed(() => {
  const all = statusDistribution.value.reduce((sum, i) => sum + i.count, 0)
  return [
    { label: '全部', count: all, status: undefined as OrderStatus | undefined },
    ...statusDistribution.value.map((i) => ({
      label: i.name,
      count: i.count,
      status: i.status as OrderStatus,
    })),
  ]
})

/** 加载故障分类（失败不阻塞，拦截器已提示） */
async function loadCategories(): Promise<void> {
  try {
    categories.value = await listCategories()
  } catch {
    categories.value = []
  }
}

/* ---------- 派单 / 改派弹窗 ---------- */
const assignDialogVisible = ref(false)
const assignMode = ref<'assign' | 'reassign'>('assign')
const assignTarget = ref<OrderVO | null>(null)

/** 打开派单弹窗（PENDING → ASSIGNED） */
function openAssign(row: OrderVO): void {
  assignTarget.value = row
  assignMode.value = 'assign'
  assignDialogVisible.value = true
}

/** 打开改派弹窗（ASSIGNED/PROCESSING → ASSIGNED） */
function openReassign(row: OrderVO): void {
  assignTarget.value = row
  assignMode.value = 'reassign'
  assignDialogVisible.value = true
}

/** 派单/改派成功后：刷新列表与顶部统计图表 */
function onAssignSuccess(): void {
  void fetchData()
  refreshKey.value += 1
  donutRef.value?.refresh()
  trendRef.value?.refresh()
}

/** 行操作：详情 / 跟进 跳转详情页；派单 / 改派 打开对应弹窗 */
function onAction(row: OrderVO, action: string): void {
  if (action === 'detail' || action === '跟进') {
    void router.push({ name: 'order-detail', params: { id: row.id } })
    return
  }
  if (action === '派单') {
    openAssign(row)
    return
  }
  if (action === '改派') {
    openReassign(row)
  }
}

/* ---------- 工单行展示辅助 ---------- */
/** 分类名 → 固定配色（与视觉稿一致，按名字哈希轮转） */
function catCls(categoryName: string): string {
  const palette = ['c-blue', 'c-cyan', 'c-green', 'c-amber', 'c-steel']
  let hash = 0
  for (let i = 0; i < categoryName.length; i += 1) {
    hash = (hash * 31 + categoryName.charCodeAt(i)) >>> 0
  }
  return palette[hash % palette.length]
}

/** 姓名首字 + 按名字哈希选头像配色 */
function avatarCls(name: string): string {
  const palette = ['av-1', 'av-2', 'av-3', 'av-4', 'av-5']
  let hash = 0
  for (let i = 0; i < name.length; i += 1) {
    hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  }
  return palette[hash % palette.length]
}

function initial(name: string): string {
  return name?.charAt(0) ?? '?'
}

/** createdAt(2026-10-21 14:32:00) → { date: '10-21', time: '14:32' } */
function splitTime(createdAt: string): { date: string; time: string } {
  const m = createdAt?.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/)
  if (!m) return { date: createdAt ?? '', time: '' }
  return { date: `${m[2]}-${m[3]}`, time: `${m[4]}:${m[5]}` }
}

onMounted(() => {
  void loadCategories()
  void fetchData()
})
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
          <span>{{ todayLabel }}</span>
          <span class="sep">/</span>
          <span>校园后勤保障中心</span>
          <span class="sep">/</span>
          <span>今日新增 {{ todayNew }} 单</span>
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

    <!-- ============ 指标卡（数字滚动 + slideUp 渐显） ============ -->
    <section class="stats" aria-label="核心指标">
      <StatCard
        v-for="(s, i) in statItems"
        :key="s.label"
        :item="s"
        :refresh-key="refreshKey"
        :delay="i * 0.06"
      />
    </section>

    <!-- ============ 趋势 + 状态分布 ============ -->
    <section class="insight-grid" aria-label="趋势与分布">
      <article class="card chart-card slide-up" style="animation-delay: 0.14s">
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
              @click="onTrendRange(t)"
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
          <TrendChart ref="trendRef" :days="trendDays" :new-orders="trendNew" :done-orders="trendDone" />
        </div>
      </article>

      <article class="card status-card slide-up" style="animation-delay: 0.2s">
        <div class="card-head">
          <div>
            <h2>工单状态分布</h2>
            <p>本周全部工单构成</p>
          </div>
          <span class="chip">共 {{ totalOrders }} 单</span>
        </div>

        <StatusDonut
          ref="donutRef"
          :items="donutItems"
          :total-text="donutTotalText"
          :completion-text="donutCompletionText"
        />
      </article>
    </section>

    <!-- ============ 工单列表 ============ -->
    <section class="card table-card slide-up" style="animation-delay: 0.26s" aria-labelledby="orderListTitle">
      <div class="table-head">
        <div class="title-wrap">
          <h2 id="orderListTitle">工单列表</h2>
          <p>共 <b>{{ orderTotal }}</b> 条工单 · <b>{{ pendingCountLabel }}</b> 条待受理，建议优先派单</p>
        </div>
        <div class="toolbar">
          <div class="search-field">
            <IconSvg name="i-search" :size="15" />
            <input
              v-model="orderKeyword"
              type="search"
              placeholder="搜索工单号 / 标题 / 报修人"
              aria-label="搜索工单"
              @keyup.enter="onOrderSearch"
              @search="onOrderSearch"
            />
          </div>
          <el-select
            v-model="orderCategoryId"
            class="category-select"
            placeholder="故障分类"
            clearable
            @change="onCategoryChange"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <button type="button" class="select-btn">
            <IconSvg name="i-calendar" :size="14" />
            近 7 天
            <IconSvg name="i-chevron-down" :size="12" />
          </button>
          <button type="button" class="btn btn-primary btn-sm" @click="onBatchAssign">
            <IconSvg name="i-send" :size="14" />
            批量派单
          </button>
        </div>
      </div>

      <div class="tabs" role="tablist" aria-label="按状态筛选工单">
        <button
          v-for="t in statusTabs"
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
                <div class="order-no">{{ row.orderNo }}</div>
                <span class="order-title">{{ row.title }}</span>
                <span class="order-addr">
                  <IconSvg name="i-pin" :size="11" />
                  {{ row.address }}
                </span>
              </td>
              <td>
                <span class="cat" :class="catCls(row.categoryName ?? '')">
                  <i></i>{{ row.categoryName ?? '未分类' }}
                </span>
              </td>
              <td>
                <div class="person">
                  <span class="avatar" :class="avatarCls(row.reporterName ?? '?')">{{
                    initial(row.reporterName ?? '?')
                  }}</span>
                  <span>{{ row.reporterName ?? '未知' }}</span>
                </div>
              </td>
              <td>
                <div v-if="row.repairerName" class="person">
                  <span class="avatar" :class="avatarCls(row.repairerName)">{{
                    initial(row.repairerName)
                  }}</span>
                  <span>{{ row.repairerName }}</span>
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
                {{ splitTime(row.createdAt).date }}<small>{{ splitTime(row.createdAt).time }}</small>
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
                    <button type="button" class="act act-primary" @click="onAction(row, '改派')">
                      改派
                    </button>
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
        <span class="foot-info">共 <b>{{ orderTotal }}</b> 条工单，本页显示 <b>{{ orders.length }}</b> 条</span>
        <el-pagination
          v-model:current-page="orderPageNum"
          :page-size="orderPageSize"
          :total="orderTotal"
          layout="total, prev, pager, next"
          background
          @current-change="onOrderPageChange"
        />
      </div>
    </section>

    <footer class="page-foot">
      <span class="dot" aria-hidden="true"></span>
      数据实时统计 · 点击右上角刷新同步最新数据
    </footer>

    <!-- 派单 / 改派 弹窗（与工单列表页复用同一组件） -->
    <AssignDialog
      v-model="assignDialogVisible"
      :mode="assignMode"
      :order="assignTarget"
      @success="onAssignSuccess"
    />
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
  background: var(--surface);
  border-color: var(--line-strong);
  color: var(--cell-text);
  box-shadow: var(--shadow-xs);
}
.btn-ghost:hover {
  border-color: var(--blue-200);
  color: var(--blue-700);
  background: var(--surface-2);
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
  background: var(--surface);
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

/* ===== 指标卡网格（卡片样式已迁至 StatCard 组件） ===== */
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
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
  background: var(--surface-2);
  font-size: 12.5px;
  color: var(--ink);
  transition: border-color 0.18s, background 0.18s, box-shadow 0.18s;
}
html.dark .search-field input {
  background: #1d1e1f;
  border-color: #4c4d4f;
  color: #e5eaf3;
}
.search-field input::placeholder {
  color: #9aadc1;
}
html.dark .search-field input::placeholder {
  color: #6b7a8d;
}
.search-field input:focus {
  outline: none;
  border-color: var(--blue-500);
  background: var(--surface);
  box-shadow: var(--ring);
}
/* 故障分类下拉（替换原「故障分类」占位按钮，样式与工具栏其余控件对齐） */
.category-select {
  width: 148px;
  flex: none;
}
.category-select :deep(.el-select__wrapper) {
  height: 38px;
  border-radius: 10px;
  box-shadow: inset 0 0 0 1px var(--line-strong);
}
.category-select :deep(.el-select__wrapper:hover) {
  box-shadow: inset 0 0 0 1px var(--blue-200);
}
.category-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: inset 0 0 0 1px var(--blue-500), var(--ring);
}
.select-btn {
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: var(--surface);
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted);
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
  /* 表头统一居中，与各业务列表页（el-table align="center"）保持一致 */
  text-align: center;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.07em;
  color: #8298ae;
  background: var(--th-bg);
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}
html.dark thead th {
  color: #7a90a8;
}
thead th:first-child {
  padding-left: 22px;
  width: 46px;
}
thead th:last-child {
  padding-right: 22px;
}
tbody td {
  padding: 14px;
  border-bottom: 1px solid var(--table-border);
  vertical-align: middle;
  /* 内容统一居中 */
  text-align: center;
  font-size: 12.5px;
  color: var(--cell-text);
}
tbody td:first-child {
  padding-left: 22px;
}
tbody td:last-child {
  padding-right: 22px;
}
tbody tr {
  transition: background 0.16s;
}
tbody tr:hover {
  background: var(--row-hover-bg);
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
  background: var(--surface);
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
.pill.reassigning {
  background: #f0f5ff;
  color: #2f54eb;
}
.pill.reassigning .dot {
  background: #2f54eb;
  box-shadow: 0 0 0 2.5px rgba(47, 84, 235, 0.14);
}
.pill.arbitrating {
  background: #fff7e6;
  color: #b26b00;
}
.pill.arbitrating .dot {
  background: #faad14;
  box-shadow: 0 0 0 2.5px rgba(250, 173, 20, 0.16);
}

.time-cell {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  color: #4e6880;
  font-size: 12px;
}
/* 时间与日期同行显示：原为 display:block 导致「日期在上、时间在下」竖排 */
.time-cell small {
  display: inline;
  font-size: 10.5px;
  color: #6b8095;
  margin-left: 4px;
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
  border-top: 1px solid var(--line);
  background: var(--surface-2);
}
.foot-info {
  font-size: 11.5px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}
.foot-info b {
  color: var(--ink-2);
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
  .category-select {
    flex: 1;
    min-width: 150px;
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
