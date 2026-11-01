<script setup lang="ts">
/**
 * 表格骨架屏 —— 用于表格页「初始加载」（数据尚未返回）阶段，
 * 替代 v-loading 遮罩，模拟「表头 + 数据行」的矩形骨架，减少视觉闪烁。
 * 二次加载（翻页/搜索/切换 Tab）因已有数据，仍由 el-table 的 v-loading 承担。
 */
withDefaults(defineProps<{ rows?: number }>(), {
  rows: 5,
})

/** 每行模拟 6 个列单元格的宽度比例（对应：工单号/标题/分类/报修人/状态/时间） */
const COLUMN_WIDTHS = ['18%', '30%', '12%', '11%', '9%', '12%']
</script>

<template>
  <div class="table-skeleton">
    <el-skeleton animated>
      <template #template>
        <!-- 表头：全宽矩形，颜色略深 -->
        <div class="sk-header">
          <el-skeleton-item variant="rect" class="sk-header-bar" />
        </div>

        <!-- 数据行：每行横向排布 6 个宽窄不一的矩形，模拟各列 -->
        <div v-for="i in rows" :key="i" class="sk-row">
          <el-skeleton-item
            v-for="(w, idx) in COLUMN_WIDTHS"
            :key="idx"
            variant="rect"
            class="sk-cell"
            :style="{ width: w }"
          />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.table-skeleton {
  min-height: 400px;
  padding: 16px 22px 22px;
  box-sizing: border-box;
}

/* ===== 表头 ===== */
.sk-header {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 4px;
}
.sk-header-bar {
  width: 100%;
  height: 40px;
  border-radius: 8px;
}

/* ===== 数据行 ===== */
.sk-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 52px;
  border-bottom: 1px solid var(--line);
}
.sk-row:last-of-type {
  border-bottom: none;
}

/* 单个列单元格：高度 20px，宽度由行内 style 控制 */
.sk-cell {
  height: 20px;
  flex: none;
  border-radius: 6px;
}
</style>
