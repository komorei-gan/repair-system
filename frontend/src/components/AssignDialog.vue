<script setup lang="ts">
/**
 * 派单 / 改派 公共弹窗（看板与工单列表复用）。
 * - mode="assign"：派单（PENDING → ASSIGNED）
 * - mode="reassign"：改派（ASSIGNED/PROCESSING → ASSIGNED，换师傅）
 * 打开时自动拉取启用中的维修师傅列表；提交成功后 emit success。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { assignOrder, reassignOrder } from '@/api/order'
import { listUsersByRole } from '@/api/user'
import type { OrderVO, UserInfo } from '@/types/api'

const props = withDefaults(
  defineProps<{
    /** 是否显示（v-model） */
    modelValue: boolean
    /** assign 派单 / reassign 改派 */
    mode: 'assign' | 'reassign'
    /** 目标工单 */
    order: OrderVO | null
  }>(),
  { modelValue: false, mode: 'assign', order: null },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const visible = ref(props.modelValue)

const submitting = ref(false)
const repairers = ref<UserInfo[]>([])
const repairersLoading = ref(false)
const selectedRepairerId = ref<number | undefined>(undefined)

/** 弹窗标题 */
const title = computed(() => (props.mode === 'reassign' ? '改派' : '派单'))

/** 是否改派模式 */
const isReassign = computed(() => props.mode === 'reassign')

/** 同步父组件 modelValue */
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      // 每次打开重置选择，并确保师傅列表已加载
      selectedRepairerId.value = undefined
      if (repairers.value.length === 0) {
        void loadRepairers()
      }
    }
  },
  { immediate: true },
)

watch(visible, (val) => emit('update:modelValue', val))

/** 拉取启用中的维修师傅列表 */
async function loadRepairers(): Promise<void> {
  repairersLoading.value = true
  try {
    repairers.value = await listUsersByRole('REPAIRER')
  } catch {
    repairers.value = []
  } finally {
    repairersLoading.value = false
  }
}

/** 提交：派单或改派 */
async function submit(): Promise<void> {
  if (!props.order) return
  if (!selectedRepairerId.value) {
    ElMessage.warning('请选择维修师傅')
    return
  }

  submitting.value = true
  try {
    if (props.mode === 'reassign') {
      await reassignOrder(props.order.id, selectedRepairerId.value)
      ElMessage.success('改派成功')
    } else {
      await assignOrder(props.order.id, selectedRepairerId.value)
      ElMessage.success('派单成功')
    }
    visible.value = false
    emit('success')
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="440px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="order" class="assign-info">
      <p class="assign-row">工单号：<b>{{ order.orderNo }}</b></p>
      <p class="assign-row">故障标题：{{ order.title }}</p>
      <p class="assign-row">报修人：{{ order.reporterName || '未知' }}</p>
      <p v-if="isReassign && order.repairerName" class="assign-row">
        现任师傅：{{ order.repairerName }}
      </p>
    </div>

    <el-form label-position="top">
      <el-form-item :label="isReassign ? '选择新维修师傅' : '选择维修师傅'" required>
        <el-select
          v-model="selectedRepairerId"
          :placeholder="isReassign ? '请选择新维修师傅' : '请选择维修师傅'"
          style="width: 100%"
          :loading="repairersLoading"
          filterable
        >
          <el-option
            v-for="r in repairers"
            :key="r.id"
            :label="r.realName || r.username"
            :value="r.id"
            :disabled="isReassign && r.id === order?.repairerId"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ isReassign ? '确认改派' : '确认派单' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
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
</style>
