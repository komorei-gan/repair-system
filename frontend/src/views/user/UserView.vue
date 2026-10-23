<script setup lang="ts">
/**
 * 用户管理 —— 三端用户（管理员/师傅/报修人）分页查询与启用/禁用。
 * 保持与工单列表一致的 Element Plus UI 风格（白色卡片、大圆角、清晰表格）。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUsers, updateUserStatus, approveReset } from '@/api/user'
import type { UserInfo } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'

/* ---------- 角色筛选 ---------- */
type RoleFilter = 'ALL' | 'ADMIN' | 'REPAIRER' | 'REPORTER'
const ROLE_OPTIONS: { label: string; value: RoleFilter }[] = [
  { label: '全部', value: 'ALL' },
  { label: '管理员', value: 'ADMIN' },
  { label: '师傅', value: 'REPAIRER' },
  { label: '报修人', value: 'REPORTER' },
]

/* ---------- 筛选与分页状态 ---------- */
const keyword = ref('')
const role = ref<RoleFilter>('ALL')
const pageNum = ref(1)
const pageSize = ref(10)

/* ---------- 表格数据 ---------- */
const loading = ref(false)
const users = ref<UserInfo[]>([])
const total = ref(0)
/** 正在切换状态的行（禁用按钮防重复点击） */
const togglingId = ref<number | null>(null)
/** 正在批准重置的行（禁用按钮防重复点击） */
const approvingId = ref<number | null>(null)

/** 拉取用户分页数据 */
async function loadUsers(): Promise<void> {
  loading.value = true
  try {
    const res = await pageUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      role: role.value === 'ALL' ? undefined : role.value,
    })
    users.value = res.records ?? []
    total.value = Number(res.total ?? 0)
  } catch {
    // 错误提示已由 axios 拦截器统一处理；此处重置空态
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ---------- 搜索 / 筛选 / 分页 ---------- */
function onSearch(): void {
  pageNum.value = 1
  void loadUsers()
}

function onReset(): void {
  keyword.value = ''
  role.value = 'ALL'
  pageNum.value = 1
  void loadUsers()
}

function onRoleChange(): void {
  pageNum.value = 1
  void loadUsers()
}

function onPageChange(p: number): void {
  pageNum.value = p
  void loadUsers()
}

function onSizeChange(s: number): void {
  pageSize.value = s
  pageNum.value = 1
  void loadUsers()
}

/* ---------- 角色标签配色 ---------- */
function roleTagType(role: string): 'danger' | 'primary' | 'success' {
  if (role === 'ADMIN') return 'danger'
  if (role === 'REPAIRER') return 'primary'
  return 'success'
}

function roleLabel(role: string): string {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    REPAIRER: '师傅',
    REPORTER: '报修人',
  }
  return map[role] ?? role
}

/* ---------- 启用/禁用 ---------- */
async function onToggleStatus(row: UserInfo): Promise<void> {
  const current = row.status === 1
  const action = current ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户「${row.realName || row.username}」吗？`,
      `${action}确认`,
      { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return // 用户取消
  }

  togglingId.value = row.id
  try {
    await updateUserStatus(row.id, current ? 0 : 1)
    ElMessage.success(`已${action}该用户`)
    await loadUsers()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    togglingId.value = null
  }
}

/* ---------- 批准密码重置 ---------- */
async function onApproveReset(row: UserInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定批准用户「${row.realName || row.username}」的密码重置申请吗？批准后该用户即可在移动端输入新密码重置。`,
      '批准确认',
      { confirmButtonText: '批准', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return // 用户取消
  }

  approvingId.value = row.id
  try {
    await approveReset(row.id)
    ElMessage.success('已批准密码重置')
    await loadUsers()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    approvingId.value = null
  }
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <div class="user-view">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p class="page-sub">共 <b>{{ total }}</b> 个账号 · 支持按姓名/账号与角色筛选</p>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <section class="card filter-card rise" aria-label="筛选条件">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="姓名 / 账号"
          clearable
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #prefix>
            <IconSvg name="i-search" :size="15" />
          </template>
        </el-input>

        <el-select
          v-model="role"
          class="role-select"
          placeholder="全部角色"
          @change="onRoleChange"
        >
          <el-option v-for="r in ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>

        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </section>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" style="animation-delay: 0.06s" aria-label="用户表格">
      <el-table
        v-loading="loading"
        :data="users"
        row-key="id"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--th-bg)', color: 'var(--muted)', fontWeight: 600 }"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" align="center" />
        <el-table-column label="真实姓名" min-width="110" align="center">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="plain">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130" align="center">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="175" align="center">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              :loading="togglingId === row.id"
              @click="onToggleStatus(row as UserInfo)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="primary"
              :disabled="(row as UserInfo).resetApproved !== 1"
              :loading="approvingId === row.id"
              @click="onApproveReset(row as UserInfo)"
            >
              {{
                (row as UserInfo).resetApproved === 1
                  ? '批准重置'
                  : (row as UserInfo).resetApproved === 2
                    ? '已批准'
                    : '未申请'
              }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" />
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
.user-view {
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
  width: 260px;
  flex: none;
}
.role-select {
  width: 150px;
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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid var(--line);
  background: var(--surface-2);
}

/* ===== 响应式 ===== */
@media (max-width: 780px) {
  .search-input,
  .role-select {
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
