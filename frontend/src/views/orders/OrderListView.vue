<script setup lang="ts">
/**
 * 工单列表 —— 管理端核心业务页（design.md §3.4）。
 * 能力：状态 Tab 筛选 / 关键字搜索（工单号·标题·报修人）/ 分类筛选 /
 *       分页表格（loading 遮罩）/ 详情跳转 / 派单 / 改派 / 删除。
 */
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as orderApi from '@/api/order'
import * as categoryApi from '@/api/category'
import { STATUS_COLORS, orderStatusLabel } from '@/utils/status'
import type { Category, OrderStatus, OrderVO } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'
import AssignDialog from '@/components/AssignDialog.vue'

/* ---------- 状态 Tab（「ALL」为哨兵值，避免空字符串歧义） ---------- */
type TabValue = 'ALL' | OrderStatus
interface StatusTab {
  label: string
  status?: OrderStatus
}

const STATUS_TABS: StatusTab[] = [
  { label: '全部' },
  { label: '待受理', status: 'PENDING' },
  { label: '已派单', status: 'ASSIGNED' },
  { label: '维修中', status: 'PROCESSING' },
  { label: '已完成', status: 'COMPLETED' },
  { label: '已确认', status: 'CONFIRMED' },
  { label: '已取消', status: 'CANCELLED' },
  { label: '已驳回', status: 'REJECTED' },
]

/** 合法状态码集合，用于校验 URL 参数 */
const VALID_STATUSES = STATUS_TABS.filter((t) => t.status).map((t) => t.status as OrderStatus)

const router = useRouter()
const route = useRoute()

/* ---------- 筛选状态（activeTab 从 URL 的 status 参数初始化） ---------- */
function resolveTabFromQuery(): TabValue {
  const q = route.query.status
  const v = typeof q === 'string' ? q : undefined
  return v && (VALID_STATUSES as string[]).includes(v) ? (v as OrderStatus) : 'ALL'
}

const activeTab = ref<TabValue>(resolveTabFromQuery())
const keyword = ref('')
const categoryId = ref<number | undefined>(undefined)
const categories = ref<Category[]>([])

/* ---------- 分页状态 ---------- */
const pageNum = ref(1)
const pageSize = ref(10)

/* ---------- 表格数据 ---------- */
const loading = ref(false)
const orders = ref<OrderVO[]>([])
const total = ref(0)

/* ---------- 派单 / 改派弹窗 ---------- */
const assignDialogVisible = ref(false)
const assignMode = ref<'assign' | 'reassign'>('assign')
const assignTarget = ref<OrderVO | null>(null)

/** 加载分类列表（失败不阻塞列表，拦截器已提示） */
async function loadCategories(): Promise<void> {
  try {
    categories.value = await categoryApi.listCategories()
  } catch {
    categories.value = []
  }
}

/** 拉取工单分页数据 */
async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const res = await orderApi.adminOrders({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeTab.value === 'ALL' ? undefined : activeTab.value,
      categoryId: categoryId.value,
      keyword: keyword.value.trim() || undefined,
    })
    orders.value = res.records ?? []
    total.value = Number(res.total ?? 0)
  } catch {
    // 错误提示已由 axios 拦截器统一处理；此处重置空态
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 切换 Tab：同步状态到 URL 并重置页码为 1 后重新加载 */
function onTabChange(name: string | number): void {
  activeTab.value = typeof name === 'string' && name !== 'ALL' ? (name as OrderStatus) : 'ALL'
  pageNum.value = 1
  void syncQueryAndLoad()
}

/** 将当前 Tab 状态同步到 URL 查询参数（不触发整页刷新） */
function syncQueryAndLoad(): Promise<void> {
  const query = activeTab.value === 'ALL' ? {} : { status: activeTab.value }
  return router.replace({ name: 'orders', query }).then(() => loadOrders())
}

/** 搜索 / 清空关键字 / 查询按钮：重置页码为 1 */
function onSearch(): void {
  pageNum.value = 1
  void loadOrders()
}

/** 切换分类：重置页码为 1 */
function onCategoryChange(): void {
  pageNum.value = 1
  void loadOrders()
}

/** 重置筛选条件 */
function onReset(): void {
  keyword.value = ''
  categoryId.value = undefined
  pageNum.value = 1
  void loadOrders()
}

/* ---------- 分页 ---------- */
function onPageChange(p: number): void {
  pageNum.value = p
  void loadOrders()
}

function onSizeChange(s: number): void {
  pageSize.value = s
  pageNum.value = 1
  void loadOrders()
}

/* ---------- 状态胶囊：基于 STATUS_COLORS 生成浅色底 + 主色文字 ---------- */
function statusTagStyle(status: OrderStatus): Record<string, string> {
  const color = STATUS_COLORS[status] ?? '#1890FF'
  return {
    color,
    backgroundColor: `${color}1A`,
    borderColor: `${color}40`,
  }
}

/* ---------- 详情跳转：把当前 status 通过 query 带过去，并带上来源页 ---------- */
function goDetail(row: OrderVO): void {
  void router.push({
    name: 'order-detail',
    params: { id: row.id },
    query: {
      from: route.path,
      ...(activeTab.value === 'ALL' ? {} : { status: activeTab.value }),
    },
  })
}

/* ---------- 派单 / 改派 ---------- */
function openAssign(row: OrderVO): void {
  assignTarget.value = row
  assignMode.value = 'assign'
  assignDialogVisible.value = true
}

function openReassign(row: OrderVO): void {
  assignTarget.value = row
  assignMode.value = 'reassign'
  assignDialogVisible.value = true
}

/** 派单 / 改派成功后刷新列表 */
function onAssignSuccess(): void {
  void loadOrders()
}

/* ---------- 删除 ---------- */
async function onDelete(row: OrderVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '确定要删除该工单吗？删除后不可恢复',
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return // 用户取消
  }

  try {
    await orderApi.deleteOrder(row.id)
    ElMessage.success('工单已删除')
    await loadOrders()
  } catch {
    // 错误提示已由 axios 拦截器统一处理（含「流转中的工单无法删除」）
  }
}

onMounted(() => {
  void loadCategories()
  void loadOrders()
})

// 外部（如详情页返回）改变 URL 的 status 时，同步 Tab 并重新加载
watch(
  () => route.query.status,
  (val) => {
    const v = typeof val === 'string' ? val : undefined
    const next: TabValue = v && (VALID_STATUSES as string[]).includes(v) ? (v as OrderStatus) : 'ALL'
    if (next !== activeTab.value) {
      activeTab.value = next
      pageNum.value = 1
      void loadOrders()
    }
  },
)
</script>

<template>
  <div class="order-list">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>工单列表</h1>
        <p class="page-sub">共 <b>{{ total }}</b> 条工单 · 支持按状态、分类、关键字筛选</p>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <section class="card filter-card rise" aria-label="筛选条件">
      <el-tabs :model-value="activeTab" @tab-change="onTabChange">
        <el-tab-pane
          v-for="t in STATUS_TABS"
          :key="t.status ?? 'ALL'"
          :label="t.label"
          :name="t.status ?? 'ALL'"
        />
      </el-tabs>

      <div class="filter-bar">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="工单号 / 报修人 / 标题"
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #prefix>
            <IconSvg name="i-search" :size="15" />
          </template>
        </el-input>

        <el-select
          v-model="categoryId"
          class="category-select"
          placeholder="全部分类"
          clearable
          @change="onCategoryChange"
        >
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>

        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </section>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" style="animation-delay: 0.06s" aria-label="工单表格">
      <el-table
        v-loading="loading"
        :data="orders"
        row-key="id"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--th-bg)', color: 'var(--muted)', fontWeight: 600 }"
      >
        <el-table-column prop="orderNo" label="工单号" min-width="170" align="center" />
        <el-table-column
          prop="title"
          label="故障标题"
          min-width="230"
          show-overflow-tooltip
          align="center"
        />
        <el-table-column label="分类" width="130" align="center">
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报修人" width="110" align="center" />
        <el-table-column label="维修师傅" width="120" align="center">
          <template #default="{ row }">{{ row.repairerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :style="statusTagStyle(row.status)" size="small" effect="plain">
              {{ orderStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="175" align="center" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row as OrderVO)">详情</el-button>
            <el-button
              v-if="(row as OrderVO).status === 'PENDING'"
              link
              type="primary"
              @click="openAssign(row as OrderVO)"
            >
              派单
            </el-button>
            <el-button
              v-if="
                (row as OrderVO).status === 'ASSIGNED' ||
                (row as OrderVO).status === 'PROCESSING'
              "
              link
              type="primary"
              @click="openReassign(row as OrderVO)"
            >
              改派
            </el-button>
            <el-button
              v-if="
                (row as OrderVO).status === 'PENDING' ||
                (row as OrderVO).status === 'CANCELLED'
              "
              link
              type="danger"
              @click="onDelete(row as OrderVO)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无工单" />
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </section>

    <!-- ============ 派单 / 改派弹窗（复用公共组件） ============ -->
    <AssignDialog
      v-model="assignDialogVisible"
      :mode="assignMode"
      :order="assignTarget"
      @success="onAssignSuccess"
    />
  </div>
</template>

<style scoped>
.order-list {
  min-width: 0;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.page-head h1 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.2;
  color: var(--ink);
}
.page-sub {
  font-size: 12.5px;
  color: var(--muted);
  margin-top: 6px;
}
.page-sub b {
  color: var(--blue-700);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ===== 筛选卡片 ===== */
.filter-card {
  padding: 6px 22px 18px;
  margin-bottom: 18px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 4px;
}
.search-input {
  width: 260px;
  flex: none;
}
.category-select {
  width: 170px;
  flex: none;
}

/* 搜索图标着色与视觉稿一致 */
.search-input :deep(.ico) {
  color: #8fa3b8;
}

/* ===== 表格卡片 ===== */
.table-card {
  overflow: hidden;
}

/* el-table 行 hover 与整体观感微调 */
.table-card :deep(.el-table) {
  --el-table-border-color: var(--table-border);
  --el-table-row-hover-bg-color: var(--row-hover-bg);
  font-size: 12.5px;
}
.table-card :deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid var(--line);
}
.table-card :deep(.el-table .cell) {
  color: var(--cell-text);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid var(--line);
  background: var(--surface-2);
}

/* ===== 派单弹窗 ===== */
.assign-info {
  padding: 12px 14px;
  margin-bottom: 6px;
  background: #f7fbff;
  border: 1px solid #e6f0fb;
  border-radius: 10px;
}
.assign-row {
  font-size: 13px;
  color: #4c647c;
  line-height: 1.9;
}
.assign-row b {
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}

/* ===== 响应式 ===== */
@media (max-width: 780px) {
  .search-input,
  .category-select {
    width: 100%;
  }
  .filter-card {
    padding: 6px 16px 16px;
  }
  .pagination-bar {
    justify-content: center;
    padding: 14px 12px;
    overflow-x: auto;
  }
}
</style>
