<script setup lang="ts">
/**
 * 修改报修页：回显工单原数据，提交 PUT /api/orders/{id}。
 * - 仅 PENDING 状态可进入修改（入口由首页控制，后端兜底校验）
 * - 分类必选、title 必填、description/address/contactPhone 选填
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useToast } from '@wot-ui/ui'
import { listCategories } from '@/api/category'
import { orderDetail, updateOrder } from '@/api/order'
import { getToken } from '@/utils/token'
import type { Category } from '@/types/api'

const toast = useToast()

const orderId = ref<number | null>(null)

const categories = ref<Category[]>([])
const categoryVisible = ref(false)
const categoryColumns = ref<{ label: string; value: number }[]>([])
const categoryValue = ref<number[]>([])
const selectedCategoryId = ref<number | null>(null)
const selectedCategoryName = ref('')

const title = ref('')
const description = ref('')
const address = ref('')
const contactPhone = ref('')
const submitting = ref(false)
const loading = ref(true)
const loadError = ref(false)

function ensureLogin(): boolean {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

async function loadCategories() {
  try {
    const list = await listCategories()
    categories.value = list
    categoryColumns.value = list.map((c) => ({ label: c.name, value: c.id }))
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  }
}

/** 回显工单原数据 */
async function fetchDetail(id: number) {
  loading.value = true
  loadError.value = false
  try {
    const detail = await orderDetail(id)
    title.value = detail.title ?? ''
    description.value = detail.description ?? ''
    address.value = detail.address ?? ''
    contactPhone.value = detail.contactPhone ?? ''
    selectedCategoryId.value = detail.categoryId
    const found = categories.value.find((c) => c.id === detail.categoryId)
    selectedCategoryName.value = found?.name ?? detail.categoryName ?? ''
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function openPicker() {
  if (categories.value.length === 0) {
    toast.info('暂无可用分类')
    return
  }
  categoryVisible.value = true
}

function onPickerConfirm(e: { value: (string | number)[]; selectedItems: { label: string; value: number }[] }) {
  const value = e.value?.[0]
  if (value !== undefined && value !== null) {
    selectedCategoryId.value = Number(value)
    const found = categories.value.find((c) => c.id === selectedCategoryId.value)
    selectedCategoryName.value = found?.name ?? ''
  }
  categoryVisible.value = false
}

async function handleSubmit() {
  if (!ensureLogin()) return
  if (orderId.value === null) return

  if (selectedCategoryId.value === null) {
    toast.error('请选择故障分类')
    return
  }
  if (!title.value.trim()) {
    toast.error('请输入故障标题')
    return
  }
  if (title.value.trim().length > 100) {
    toast.error('故障标题不能超过 100 字')
    return
  }
  if (description.value.length > 1000) {
    toast.error('故障描述不能超过 1000 字')
    return
  }
  if (address.value.length > 200) {
    toast.error('报修地址不能超过 200 字')
    return
  }
  // 联系电话：填写时须为大陆手机号格式（与后端校验一致）
  if (contactPhone.value && !/^1[3-9]\d{9}$/.test(contactPhone.value)) {
    toast.error('联系电话格式不正确')
    return
  }

  submitting.value = true
  try {
    await updateOrder(orderId.value, {
      categoryId: selectedCategoryId.value,
      title: title.value.trim(),
      description: description.value || undefined,
      address: address.value || undefined,
      contactPhone: contactPhone.value || undefined,
    })
    toast.success('修改成功')
    setTimeout(() => {
      uni.navigateBack()
    }, 400)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  if (!ensureLogin()) return
  const id = Number(query?.id)
  if (!id || Number.isNaN(id)) {
    uni.showToast({ title: '工单参数缺失', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
    return
  }
  orderId.value = id
  loadCategories()
  fetchDetail(id)
})
</script>

<template>
  <view class="page">
    <wd-navbar
      title="修改报修"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      bordered
      @click-left="uni.navigateBack()"
    />

    <view class="form-wrap">
      <!-- 加载中 -->
      <view v-if="loading" class="tip">
        <wd-loading />
        <text class="tip-text">加载中...</text>
      </view>

      <!-- 加载失败 -->
      <view v-else-if="loadError" class="tip">
        <text class="tip-text">加载失败</text>
        <wd-button type="primary" size="small" round @click="fetchDetail(orderId ?? 0)">重试</wd-button>
      </view>

      <!-- 表单 -->
      <template v-else>
        <view class="form-card">
          <wd-form :model="{ title, description, address, contactPhone }" error-type="none">
            <wd-form-item label="故障分类" required is-link :clickable="true" @click="openPicker">
              <view class="form-value" :class="{ 'is-placeholder': !selectedCategoryId }">
                {{ selectedCategoryId ? selectedCategoryName : '请选择分类' }}
              </view>
            </wd-form-item>

            <wd-form-item label="故障标题" required>
              <wd-input
                v-model="title"
                placeholder="请输入故障标题"
                :maxlength="100"
                clearable
                compact
              />
            </wd-form-item>

            <wd-form-item label="故障描述">
              <wd-textarea
                v-model="description"
                placeholder="请描述故障情况（选填）"
                :maxlength="1000"
                show-word-limit
              />
            </wd-form-item>

            <wd-form-item label="报修地址">
              <wd-input
                v-model="address"
                placeholder="请输入报修地址（选填）"
                :maxlength="200"
                clearable
                compact
              />
            </wd-form-item>

            <wd-form-item label="联系电话">
              <wd-input
                v-model="contactPhone"
                type="tel"
                placeholder="请输入联系电话（选填）"
                :maxlength="11"
                clearable
                compact
              />
            </wd-form-item>
          </wd-form>
        </view>

        <wd-button
          type="primary"
          block
          :loading="submitting"
          custom-class="submit-btn"
          @click="handleSubmit"
        >
          保存修改
        </wd-button>
      </template>
    </view>

    <!-- 分类选择弹层 -->
    <wd-picker
      v-model:visible="categoryVisible"
      v-model="categoryValue"
      :columns="categoryColumns"
      title="选择故障分类"
      label-key="label"
      value-key="value"
      @confirm="onPickerConfirm"
    />

    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f4f7f9;
}

.form-wrap {
  padding: 16px;
}

.tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding: 120rpx 0;
}

.tip-text {
  font-size: 26rpx;
  color: #909399;
}

.form-card {
  padding: 8px 20px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.form-value {
  font-size: 14px;
  color: #1f2d3d;
}

.form-value.is-placeholder {
  color: #a8b0bd;
}

/* 通栏渐变提交按钮 */
.submit-btn {
  margin-top: 20px;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #1890ff, #36cfc9);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.35);
}
</style>
