<script setup lang="ts">
/**
 * 师傅审核 —— 管理端审核维修师傅注册申请。
 * 三态 Tab（待审核 / 已通过 / 已拒绝）+ 关键字搜索 + 通过/拒绝操作。
 * 保持与用户管理一致的 Element Plus 卡片风格。
 */
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRepairerAudit, updateRepairerAudit } from '@/api/repairerAudit'
import { useAppStore } from '@/stores/app'
import { loadPageSize, savePageSize, PAGE_SIZE_KEY } from '@/utils/pageSize'
import type { AuditStatus, RepairerAuditVO } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'

/* ---------- 筛选 Tab ---------- */
const TABS: { label: string; value: AuditStatus }[] = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
]

const route = useRoute()
const appStore = useAppStore()

/**
 * 解析路由 query 中的 tab 参数（0 待审核 / 1 已通过 / 2 已拒绝）。
 * 非法或缺省时回落到「待审核」，保证从通知跳转进来时定位准确。
 */
function resolveTab(value: unknown): AuditStatus {
  const tab = Number(value)
  return tab === 1 || tab === 2 ? tab : 0
}

/* ---------- 筛选与分页状态 ---------- */
const keyword = ref('')
const activeTab = ref<AuditStatus>(resolveTab(route.query.tab))
const pageNum = ref(1)
const pageSize = ref(loadPageSize(PAGE_SIZE_KEY.repairerAudit))

// 切换每页条数时持久化，下次进入恢复
watch(pageSize, (v) => savePageSize(PAGE_SIZE_KEY.repairerAudit, v))

/* ---------- 表格数据 ---------- */
const loading = ref(false)
const list = ref<RepairerAuditVO[]>([])
const total = ref(0)
/** 正在操作的行（禁用按钮防重复点击） */
const operatingId = ref<number | null>(null)

/** 拉取审核分页数据 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await pageRepairerAudit({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      auditStatus: activeTab.value,
      keyword: keyword.value.trim() || undefined,
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

/* ---------- 筛选 / 分页 ---------- */
function onSearch(): void {
  pageNum.value = 1
  void load()
}

function onReset(): void {
  keyword.value = ''
  activeTab.value = 0
  pageNum.value = 1
  void load()
}

function onTabChange(): void {
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

/* ---------- 状态标签 ---------- */
function statusTagType(status: AuditStatus): 'warning' | 'success' | 'danger' {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  return 'danger'
}

function statusLabel(status: AuditStatus): string {
  const map: Record<AuditStatus, string> = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝',
  }
  return map[status]
}

/* ---------- 通过 / 拒绝 ---------- */
async function onAudit(row: RepairerAuditVO, auditStatus: 1 | 2): Promise<void> {
  const action = auditStatus === 1 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(
      `确定${action}师傅「${row.realName || row.username}」的申请吗？`,
      `${action}确认`,
      {
        confirmButtonText: action,
        cancelButtonText: '取消',
        type: auditStatus === 1 ? 'success' : 'warning',
      },
    )
  } catch {
    return // 用户取消
  }

  operatingId.value = row.id
  try {
    await updateRepairerAudit(row.id, auditStatus)
    ElMessage.success(`已${action}`)
    // 待审核项 -1（仅从待审核 Tab 操作时生效，其他 Tab 无待审核项）
    if (row.auditStatus === 0) {
      appStore.adjustRepairerAuditPendingCount(-1)
    }
    await load()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    operatingId.value = null
  }
}

/**
 * 监听 query.tab 变化：
 * 已在本页时再次点击「师傅注册申请」通知，路由仅 query 变化不会重新挂载组件，
 * 此处主动同步 Tab 并重新拉取，确保定位到「待审核」。
 */
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
  <div class="repairer-audit-view">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>师傅审核</h1>
        <p class="page-sub">共 <b>{{ total }}</b> 条申请 · 支持按审核状态与账号/姓名筛选</p>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <section class="card filter-card rise" aria-label="筛选条件">
      <div class="filter-bar">
        <el-radio-group v-model="activeTab" @change="onTabChange">
          <el-radio-button v-for="t in TABS" :key="t.value" :value="t.value">
            {{ t.label }}
          </el-radio-button>
        </el-radio-group>

        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="账号 / 姓名"
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #prefix>
            <IconSvg name="i-search" :size="15" />
          </template>
        </el-input>

        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </section>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" style="animation-delay: 0.06s" aria-label="师傅审核表格">
      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--th-bg)', color: 'var(--muted)', fontWeight: 600 }"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="账号" min-width="120" align="center" />
        <el-table-column label="姓名" min-width="110" align="center">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130" align="center">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="申请理由" min-width="220">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.reason"
              :content="row.reason"
              placement="top"
              :show-after="300"
            >
              <span class="reason-cell">{{ row.reason }}</span>
            </el-tooltip>
            <span v-else class="reason-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="175" align="center">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.auditStatus)" size="small" effect="plain">
              {{ statusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 0">
              <el-button
                link
                type="primary"
                :loading="operatingId === row.id"
                @click="onAudit(row as RepairerAuditVO, 1)"
              >
                通过
              </el-button>
              <el-button
                link
                type="danger"
                :loading="operatingId === row.id"
                @click="onAudit(row as RepairerAuditVO, 2)"
              >
                拒绝
              </el-button>
            </template>
            <span v-else class="op-done">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无申请" />
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
.repairer-audit-view {
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
  padding: 18px 22px;
  margin-bottom: 18px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-input {
  width: 240px;
  flex: none;
}
.search-input :deep(.ico) {
  color: #8fa3b8;
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
.op-done {
  color: var(--muted-2);
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
  .search-input {
    width: 100%;
  }
  .filter-card {
    padding: 16px;
  }
  .pagination-bar {
    justify-content: center;
    padding: 14px 12px;
    overflow-x: auto;
  }
}
</style>
