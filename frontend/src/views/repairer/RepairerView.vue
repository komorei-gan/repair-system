<script setup lang="ts">
/**
 * 师傅业绩榜 —— 纯展示全部维修师傅的累计完成单量与平均评分。
 * 数据源：GET /api/admin/stats/repairers（按完成量/评分降序返回）。
 */
import { onMounted, ref } from 'vue'
import { getRepairerLeaderboard, type RepairerLeaderboardItem } from '@/api/dashboard'

const loading = ref(false)
const repairers = ref<RepairerLeaderboardItem[]>([])

async function load(): Promise<void> {
  loading.value = true
  try {
    repairers.value = await getRepairerLeaderboard()
  } catch {
    // 错误提示已由 axios 拦截器统一处理；此处重置空态
    repairers.value = []
  } finally {
    loading.value = false
  }
}

/** 评分展示：保留 1 位小数；暂无评价显示 '-' */
function ratingText(rating: number | null): string {
  if (rating === null || rating === undefined) return '-'
  return rating.toFixed(1)
}

/** 名次徽标：前三名用奖牌色，其余统一灰色序号 */
function rankCls(index: number): string {
  if (index === 0) return 'rank rank-1'
  if (index === 1) return 'rank rank-2'
  if (index === 2) return 'rank rank-3'
  return 'rank'
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="repairer-view">
    <!-- ============ 页头 ============ -->
    <div class="page-head">
      <div>
        <h1>师傅业绩榜</h1>
        <p class="page-sub">按累计完成单量与平均评分排序 · 纯展示</p>
      </div>
    </div>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" aria-label="师傅业绩榜">
      <el-table
        v-loading="loading"
        :data="repairers"
        row-key="id"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--th-bg)', color: 'var(--muted)', fontWeight: 600 }"
      >
        <el-table-column label="名次" width="80" align="center">
          <template #default="{ $index }">
            <span class="rank" :class="rankCls($index)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="师傅姓名" min-width="140" align="center">
          <template #default="{ row }">{{ row.name }}</template>
        </el-table-column>
        <el-table-column label="账号" min-width="120" align="center">
          <template #default="{ row }">{{ row.username }}</template>
        </el-table-column>
        <el-table-column label="累计完成单量" min-width="130" align="center">
          <template #default="{ row }">
            <b class="finish-count">{{ row.finishCount }}</b>
          </template>
        </el-table-column>
        <el-table-column label="平均评分" min-width="110" align="center">
          <template #default="{ row }">
            <span class="rating" :class="{ none: row.rating === null }">
              {{ ratingText(row.rating) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无维修师傅" />
        </template>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.repairer-view {
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

/* ===== 名次徽标 ===== */
.rank {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #7e92a8;
  background: #f0f5fb;
  font-variant-numeric: tabular-nums;
}
.rank-1 {
  color: #b26b00;
  background: #fff7e6;
}
.rank-2 {
  color: #5e7489;
  background: #eef1f5;
}
.rank-3 {
  color: #9c5a28;
  background: #fbf0e6;
}

.finish-count {
  font-size: 14px;
  font-weight: 700;
  color: var(--blue-700);
  font-variant-numeric: tabular-nums;
}
.rating {
  font-size: 13px;
  font-weight: 600;
  color: #d48806;
  font-variant-numeric: tabular-nums;
}
.rating.none {
  color: var(--muted-2);
  font-weight: 400;
}
</style>
