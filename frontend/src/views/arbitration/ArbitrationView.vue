<script setup lang="ts">
/**
 * 仲裁中心 —— 集中处理「申请改派（REASSIGNING）」与「申请仲裁（ARBITRATING）」工单审批。
 * 双 Tab（申请改派 / 申请仲裁）+ 证据图片预览 + 同意/驳回（填写审批备注，选填）。
 * 复用 PUT /api/admin/orders/{id}/audit，审批后刷新列表并同步左侧菜单红点。
 */
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditingOrders, auditOrder } from '@/api/order'
import { useAppStore } from '@/stores/app'
import type { OrderVO } from '@/types/api'

/* ---------- 审批类型（Tab 对应后端 type 参数） ---------- */
type AuditType = 'REASSIGN' | 'ARBITRATE'

const TABS: { label: string; value: AuditType }[] = [
  { label: '申请改派', value: 'REASSIGN' },
  { label: '申请仲裁', value: 'ARBITRATE' },
]

const route = useRoute()
const appStore = useAppStore()

/** 解析路由 query 的 tab 参数（REASSIGN / ARBITRATE），非法或缺省回「申请改派」 */
function resolveTab(value: unknown): AuditType {
  return value === 'ARBITRATE' ? 'ARBITRATE' : 'REASSIGN'
}

/* ---------- 筛选与分页状态 ---------- */
const activeTab = ref<AuditType>(resolveTab(route.query.tab))
const pageNum = ref(1)
const pageSize = ref(10)

/* ---------- 表格数据 ---------- */
const loading = ref(false)
const list = ref<OrderVO[]>([])
const total = ref(0)
/** 正在操作的行（禁用按钮防重复点击） */
const operatingId = ref<number | null>(null)

/** 解析 applyImages JSON 字符串为 URL 数组（解析失败返回空数组） */
function parseApplyImages(raw?: string | null): string[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((v): v is string => typeof v === 'string' && v) : []
  } catch {
    return []
  }
}

/** 拉取待审批分页数据 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await auditingOrders({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: activeTab.value,
    })
    list.value = res.records ?? []
    total.value = Number(res.total ?? 0)
  } catch {
    // 错误提示已由 axios 拦截器统一处理；此处重置空态
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onTabChange(name: string | number): void {
  // 注意：此处使用 :model-value 单向绑定 + @tab-change，需手动同步 activeTab，
  // 否则切换后 load() 仍会带着旧 Tab 的 type 请求，导致「申请改派/申请仲裁」数据串台。
  activeTab.value = name === 'ARBITRATE' ? 'ARBITRATE' : 'REASSIGN'
  pageNum.value = 1
  void load()
}

function onPageChange(p: number): void {
  pageNum.value = p
  void load()
}

function onSizeChange(s: number): void {
  pageSize.value = s
  pageNum.value = 1
  void load()
}

/* ---------- 同意 / 驳回 ---------- */
async function onAudit(row: OrderVO, approve: boolean): Promise<void> {
  const action = approve ? '同意' : '驳回'
  // 用 prompt 填写审批备注（选填，限制 200 字），取消则不调用接口
  let remark: string
  try {
    const { value } = await ElMessageBox.prompt(
      `工单「${row.orderNo}」的${activeTab.value === 'REASSIGN' ? '改派' : '仲裁'}申请，` +
        `请填写审批备注（选填）：`,
      `${action}确认`,
      {
        confirmButtonText: `确认${action}`,
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '审批备注（选填，不超过 200 字）',
        inputValidator: (val: string) => {
          if (val && val.length > 200) return '备注不能超过 200 字'
          return true
        },
      },
    )
    remark = value
  } catch {
    return // 用户取消
  }

  operatingId.value = row.id
  try {
    await auditOrder(row.id, {
      approve,
      type: activeTab.value,
      ...(remark && remark.trim() ? { remark: remark.trim() } : {}),
    })
    ElMessage.success(`已${action}`)
    // 审批完成一件，待审批数 -1，同步左侧菜单红点
    appStore.adjustArbitrationPendingCount(-1)
    await load()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    operatingId.value = null
  }
}

/** 监听 query.tab 变化：已在本页时再次点击「仲裁中心」相关通知可定位到对应 Tab */
watch(
  () => route.query.tab,
  (value) => {
    const tab = resolveTab(value)
    if (tab === activeTab.value) return
    activeTab.value = tab
    pageNum.value = 1
    void load()
  },
)

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="arbitration-view">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>仲裁中心</h1>
        <p class="page-sub">共 <b>{{ total }}</b> 条待审批 · 申请改派 / 申请仲裁统一在此处理</p>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <section class="card filter-card rise" aria-label="审批类型">
      <el-tabs :model-value="activeTab" @tab-change="onTabChange">
        <el-tab-pane
          v-for="t in TABS"
          :key="t.value"
          :label="t.label"
          :name="t.value"
        />
      </el-tabs>
    </section>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" style="animation-delay: 0.06s" aria-label="待审批工单表格">
      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--th-bg)', color: 'var(--muted)', fontWeight: 600 }"
      >
        <el-table-column prop="orderNo" label="工单号" min-width="170" align="center" />
        <!-- 故障标题：直接取后端返回的 title -->
        <el-table-column label="故障标题" min-width="180" align="center">
          <template #default="{ row }">{{ row.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="师傅姓名" min-width="110" align="center">
          <template #default="{ row }">{{ row.repairerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="申请理由" min-width="240">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.applyReason"
              :content="row.applyReason"
              placement="top"
              :show-after="300"
            >
              <span class="reason-cell">{{ row.applyReason }}</span>
            </el-tooltip>
            <span v-else class="reason-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="证据图片" min-width="180" align="center">
          <template #default="{ row }">
            <div v-if="parseApplyImages(row.applyImages).length" class="apply-images">
              <el-image
                v-for="(img, idx) in parseApplyImages(row.applyImages)"
                :key="img"
                class="apply-thumb"
                :src="img"
                :preview-src-list="parseApplyImages(row.applyImages)"
                :initial-index="idx"
                fit="cover"
                preview-teleported
              />
            </div>
            <span v-else class="reason-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报修人姓名" min-width="110" align="center" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :loading="operatingId === row.id"
              @click="onAudit(row as OrderVO, true)"
            >
              同意
            </el-button>
            <el-button
              link
              type="danger"
              :loading="operatingId === row.id"
              @click="onAudit(row as OrderVO, false)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无待审批工单" />
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
  </div>
</template>

<style scoped>
.arbitration-view {
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
  padding: 6px 22px 0;
  margin-bottom: 18px;
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

.reason-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  color: var(--ink-2);
}
.reason-empty {
  color: var(--muted-2);
}

/* 证据图片缩略图 */
.apply-images {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}
.apply-thumb {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--line);
  cursor: pointer;
  flex: none;
}
.apply-thumb :deep(.el-image__inner) {
  border-radius: 6px;
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
  .filter-card {
    padding: 6px 16px 0;
  }
  .pagination-bar {
    justify-content: center;
    padding: 14px 12px;
    overflow-x: auto;
  }
}
</style>
