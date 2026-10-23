<script setup lang="ts">
/**
 * 派单中心 —— 只处理「待受理（PENDING）」工单的集中派单。
 * 复用 OrderListView 的表格与分页逻辑，去掉搜索框与状态 Tabs：
 * 查询时固定传 status: 'PENDING'，操作列保留「详情」「派单」与「删除」。
 * 详情跳转带 from 来源，返回时回到本页。
 */
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as orderApi from '@/api/order'
import { STATUS_COLORS, orderStatusLabel } from '@/utils/status'
import type { OrderStatus, OrderVO } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'
import AssignDialog from '@/components/AssignDialog.vue'

const route = useRoute()
const router = useRouter()

/* ---------- 分页状态 ---------- */
const pageNum = ref(1)
const pageSize = ref(10)

/* ---------- 表格数据 ---------- */
const loading = ref(false)
const orders = ref<OrderVO[]>([])
const total = ref(0)

/* ---------- 派单弹窗 ---------- */
const assignDialogVisible = ref(false)
const assignTarget = ref<OrderVO | null>(null)

/** 拉取待受理工单（固定 PENDING，不提供状态切换） */
async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const res = await orderApi.adminOrders({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 'PENDING',
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

/* ---------- 派单 ---------- */
function openAssign(row: OrderVO): void {
  assignTarget.value = row
  assignDialogVisible.value = true
}

/** 派单成功后刷新列表（弹窗内部已提示成功） */
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
    // 错误提示已由 axios 拦截器统一处理
  }
}

/* ---------- 详情跳转：带 from 来源，返回时回到派单中心 ---------- */
function goDetail(row: OrderVO): void {
  void router.push({
    path: `/orders/${row.id}`,
    query: { from: route.path },
  })
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <div class="dispatch-view">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>派单中心</h1>
        <p class="page-sub">仅展示待受理工单，集中派单给维修师傅</p>
      </div>
    </div>

    <!-- ============ 待派发统计卡 ============ -->
    <section class="card summary-card rise" aria-label="待派发统计">
      <div class="summary-icon" aria-hidden="true">
        <IconSvg name="i-send" :size="22" />
      </div>
      <div class="summary-text">
        <p>待派发工单</p>
        <b class="num">{{ total }}</b>
      </div>
      <p class="summary-hint">当前有 <b>{{ total }}</b> 个工单等待派发</p>
    </section>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" style="animation-delay: 0.06s" aria-label="待受理工单表格">
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
          min-width="240"
          show-overflow-tooltip
          align="center"
        />
        <el-table-column label="分类" width="130" align="center">
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报修人" width="110" align="center" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :style="statusTagStyle(row.status)" size="small" effect="plain">
              {{ orderStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="175" align="center" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row as OrderVO)">详情</el-button>
            <el-button link type="primary" @click="openAssign(row as OrderVO)">派单</el-button>
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
          <el-empty description="暂无待受理工单" />
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

    <!-- ============ 派单弹窗（复用公共组件） ============ -->
    <AssignDialog
      v-model="assignDialogVisible"
      mode="assign"
      :order="assignTarget"
      @success="onAssignSuccess"
    />
  </div>
</template>

<style scoped>
.dispatch-view {
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

/* ===== 待派发统计卡 ===== */
.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  margin-bottom: 18px;
}
.summary-icon {
  width: 46px;
  height: 46px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: var(--blue-600);
  background: var(--blue-50);
}
.summary-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.summary-text p {
  font-size: 12px;
  color: var(--muted);
}
.summary-text b {
  font-size: 26px;
  font-weight: 700;
  color: var(--blue-700);
  margin-top: 2px;
}
.summary-hint {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--muted-2);
}
.summary-hint b {
  color: var(--blue-700);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ===== 表格卡片 ===== */
.table-card {
  overflow: hidden;
}
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

/* ===== 响应式 ===== */
@media (max-width: 780px) {
  .summary-card {
    flex-wrap: wrap;
  }
  .summary-hint {
    width: 100%;
    margin-left: 0;
  }
  .pagination-bar {
    justify-content: center;
    padding: 14px 12px;
    overflow-x: auto;
  }
}
</style>
