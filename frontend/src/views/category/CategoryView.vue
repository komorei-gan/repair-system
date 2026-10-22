<script setup lang="ts">
/**
 * 故障分类 —— 分类增删改与排序管理。
 * 保持与工单列表一致的 Element Plus UI 风格（白色卡片、大圆角、清晰表格）。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createCategory, deleteCategory, listAllCategories, updateCategory } from '@/api/category'
import type { Category } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'

/* ---------- 表格数据 ---------- */
const loading = ref(false)
const categories = ref<Category[]>([])

/* ---------- 弹窗状态 ---------- */
const dialogVisible = ref(false)
const submitting = ref(false)
/** 编辑目标（null 表示新增） */
const editing = ref<Category | null>(null)

const formRef = ref<FormInstance>()
const form = ref({
  name: '',
  sort: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

/** 弹窗标题 */
const dialogTitle = () => (editing.value ? '编辑分类' : '新增分类')

/** 拉取全部分类（含停用） */
async function loadCategories(): Promise<void> {
  loading.value = true
  try {
    categories.value = await listAllCategories()
  } catch {
    // 错误提示已由 axios 拦截器统一处理；此处重置空态
    categories.value = []
  } finally {
    loading.value = false
  }
}

/* ---------- 新增 / 编辑 ---------- */
/**
 * 计算下一个排序值：当前列表最大 sort + 1。
 * 列表已按 sort 升序返回，默认取尾部即最大值，但对历史脏数据（非升序）也安全。
 */
function nextSort(): number {
  if (categories.value.length === 0) return 1
  const max = Math.max(...categories.value.map((c) => c.sort ?? 0))
  return max + 1
}

function openCreate(): void {
  editing.value = null
  // 未手动填写时默认排到最底部，避免 sort=0 抢到列表最上方
  form.value = { name: '', sort: nextSort() }
  dialogVisible.value = true
}

function openEdit(row: Category): void {
  editing.value = row
  form.value = { name: row.name, sort: row.sort ?? 0 }
  dialogVisible.value = true
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return // 校验失败
  }

  submitting.value = true
  try {
    if (editing.value) {
      await updateCategory(editing.value.id, { name: form.value.name, sort: form.value.sort })
      ElMessage.success('分类已更新')
    } else {
      await createCategory({ name: form.value.name, sort: form.value.sort })
      ElMessage.success('分类已新增')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

/* ---------- 删除 ---------- */
async function onDelete(row: Category): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${row.name}」吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return // 用户取消
  }

  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    await loadCategories()
  } catch {
    // 错误提示已由 axios 拦截器统一处理（含「分类下存在待处理工单」）
  }
}

onMounted(() => {
  void loadCategories()
})
</script>

<template>
  <div class="category-view">
    <!-- ============ 页头 + 操作栏 ============ -->
    <div class="page-head">
      <div>
        <h1>故障分类</h1>
        <p class="page-sub">共 <b>{{ categories.length }}</b> 个分类 · 支持增删改与排序管理</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <IconSvg name="i-plus" :size="15" />
        &nbsp;新增分类
      </el-button>
    </div>

    <!-- ============ 表格区 ============ -->
    <section class="card table-card rise" aria-label="分类表格">
      <el-table
        v-loading="loading"
        :data="categories"
        row-key="id"
        style="width: 100%"
        :header-cell-style="{ background: '#f8fbff', color: '#5e7489', fontWeight: 600 }"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="分类名称" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="175">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as Category)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row as Category)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无分类" />
        </template>
      </el-table>
    </section>

    <!-- ============ 新增/编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle()"
      width="440px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：水电 / 门窗 / 网络" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">
          {{ editing ? '保存' : '新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-view {
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

/* ===== 表格卡片 ===== */
.table-card {
  overflow: hidden;
}
.table-card :deep(.el-table) {
  --el-table-border-color: #f0f6fc;
  --el-table-row-hover-bg-color: #f8fbff;
  font-size: 12.5px;
}
.table-card :deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid var(--line);
}
.table-card :deep(.el-table .cell) {
  color: #3e5872;
}
</style>
