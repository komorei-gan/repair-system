<script setup lang="ts">
/**
 * 修改报修页：回显工单原数据，提交 PUT /api/orders/{id}。
 * - 仅 PENDING 状态可进入修改（入口由首页控制，后端兜底校验）
 * - 分类必选、title 必填、description/address/contactPhone 选填
 * - 图片选填，最多 3 张：回显原图 + 新增上传，提交时 JSON 回写 images
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useToast } from '@wot-ui/ui'
import { listCategories } from '@/api/category'
import { orderDetail, updateOrder, uploadImage } from '@/api/order'
import { getToken } from '@/utils/token'
import type { Category } from '@/types/api'

const toast = useToast()

const MAX_IMAGES = 3

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

/** 图片 URL 列表（回显原图 + 新增上传） */
const images = ref<string[]>([])
const uploading = ref(false)

function ensureLogin(): boolean {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

/** 返回上一页；页面栈为空（如 H5 刷新后）时兜底回首页 */
function goBack() {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.reLaunch({ url: '/pages/index/index' })
    },
  })
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
    images.value = parseImages(detail.images)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 解析后端 images JSON 字符串为 URL 数组 */
function parseImages(raw: string | null): string[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((v): v is string => typeof v === 'string') : []
  } catch {
    return []
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

/** 前端压缩图片（质量 70%）；H5 等不支持 uni.compressImage 的环境直接返回原图 */
function compressImage(src: string): Promise<string> {
  // H5 环境下 uni-app 未实现 compressImage，运行时判断直接跳过压缩
  if (typeof uni.compressImage !== 'function') {
    return Promise.resolve(src)
  }
  return new Promise((resolve) => {
    uni.compressImage({
      src,
      quality: 70,
      success: (res) => resolve(res.tempFilePath),
      fail: (err) => {
        console.error('图片压缩失败，回退使用原图', err)
        resolve(src)
      },
    })
  })
}

function chooseImage() {
  if (uploading.value) return
  const remain = MAX_IMAGES - images.value.length
  if (remain <= 0) {
    toast.info(`最多上传 ${MAX_IMAGES} 张图片`)
    return
  }
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const paths = (res.tempFilePaths as string[]) ?? []
      await handleUpload(paths)
    },
  })
}

async function handleUpload(paths: string[]) {
  if (!paths.length) return
  uploading.value = true
  uni.showLoading({ title: '上传中...', mask: true })
  try {
    for (const src of paths) {
      if (images.value.length >= MAX_IMAGES) break
      try {
        const compressed = await compressImage(src)
        const url = await uploadImage(compressed)
        images.value.push(url)
      } catch (err) {
        // 单张上传失败：记录并提示，跳过该张继续
        console.error('图片上传失败', err)
        uni.showToast({ title: '图片上传失败，请重试', icon: 'none' })
      }
    }
  } catch (err) {
    console.error('图片处理失败', err)
    uni.showToast({ title: '图片处理失败，请重试', icon: 'none' })
  } finally {
    uni.hideLoading()
    uploading.value = false
  }
}

function previewImage(url: string) {
  uni.previewImage({ urls: images.value, current: url })
}

function removeImage(index: number) {
  images.value.splice(index, 1)
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
      images: images.value.length ? JSON.stringify(images.value) : undefined,
      address: address.value || undefined,
      contactPhone: contactPhone.value || undefined,
    })
    toast.success('修改成功')
    setTimeout(() => {
      goBack()
    }, 500)
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
    setTimeout(() => goBack(), 600)
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
      @click-left="goBack"
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
        <!-- 基础信息白卡：一行行结构（左图标 + 右输入/选择） -->
        <view class="form-card">
          <view class="form-row" @click="openPicker">
            <view class="row-icon">
              <wd-icon name="tag" :size="18" color="#1890ff" />
            </view>
            <view class="row-body">
              <view class="form-value" :class="{ 'is-placeholder': !selectedCategoryId }">
                {{ selectedCategoryId ? selectedCategoryName : '请选择分类' }}
              </view>
            </view>
            <wd-icon name="arrow-right" :size="14" color="#c0c4cc" />
          </view>

          <view class="form-row">
            <view class="row-icon">
              <wd-icon name="edit" :size="18" color="#1890ff" />
            </view>
            <view class="row-body">
              <wd-input
                v-model="title"
                placeholder="请输入故障标题"
                :maxlength="100"
                clearable
                compact
                custom-class="row-input"
              />
            </view>
          </view>

          <view class="form-row">
            <view class="row-icon">
              <wd-icon name="location" :size="18" color="#1890ff" />
            </view>
            <view class="row-body">
              <wd-input
                v-model="address"
                placeholder="请输入报修地址（选填）"
                :maxlength="200"
                clearable
                compact
                custom-class="row-input"
              />
            </view>
          </view>

          <view class="form-row no-border">
            <view class="row-icon">
              <wd-icon name="phone" :size="18" color="#1890ff" />
            </view>
            <view class="row-body">
              <wd-input
                v-model="contactPhone"
                type="tel"
                placeholder="请输入联系电话（选填）"
                :maxlength="11"
                clearable
                compact
                custom-class="row-input"
              />
            </view>
          </view>
        </view>

        <!-- 故障描述：浅灰背景大文本域，右上角字数统计 -->
        <view class="desc-block">
          <view class="desc-head">
            <text class="desc-label">故障描述</text>
            <text class="desc-count">{{ description.length }}/1000</text>
          </view>
          <wd-textarea
            v-model="description"
            placeholder="请描述故障情况（选填）"
            :maxlength="1000"
            :auto-height="false"
            custom-textarea-class="desc-textarea"
          />
        </view>

        <!-- 图片上传区（选填，最多 3 张） -->
        <view class="upload-card">
          <view class="upload-head">
            <text class="upload-label">故障图片</text>
            <text class="upload-tip">选填，最多 {{ MAX_IMAGES }} 张</text>
          </view>
          <view class="upload-grid">
            <view v-for="(img, idx) in images" :key="img" class="upload-item">
              <image :src="img" mode="aspectFill" class="upload-img" @click="previewImage(img)" />
              <view class="upload-remove" @click.stop="removeImage(idx)">
                <wd-icon name="close" :size="10" color="#ffffff" />
              </view>
            </view>
            <view v-if="images.length < MAX_IMAGES" class="upload-add" @click="chooseImage">
              <wd-icon name="camera" :size="22" color="#a8b0bd" />
              <text class="upload-add-text">{{ uploading ? '上传中...' : '添加图片' }}</text>
            </view>
          </view>
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

/* 白色卡片 */
.form-card,
.upload-card {
  padding: 4px 20px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

/* 一行行结构：左图标 + 右内容，无竖线，行间细线分隔 */
.form-row {
  display: flex;
  align-items: center;
  min-height: 52px;
  border-bottom: 1px solid #f0f3f7;
}

.form-row.no-border {
  border-bottom: none;
}

.row-icon {
  width: 30px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.form-value {
  font-size: 14px;
  color: #1f2d3d;
}

.form-value.is-placeholder {
  color: #a8b0bd;
}

/* 行内输入框：无背景无边框 */
.row-input {
  width: 100%;
  font-size: 14px;
}

/* 故障描述块：浅灰背景 */
.desc-block {
  margin-top: 12px;
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 16px;
}

.desc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.desc-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.desc-count {
  font-size: 12px;
  color: #a8b0bd;
}

.desc-textarea {
  font-size: 14px;
  line-height: 1.6;
}

/* 图片上传区 */
.upload-card {
  margin-top: 12px;
  padding-top: 14px;
  padding-bottom: 16px;
}

.upload-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.upload-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.upload-tip {
  font-size: 12px;
  color: #a8b0bd;
}

.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-item {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
}

.upload-img {
  width: 100%;
  height: 100%;
  border-radius: 10px;
}

.upload-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.55);
  border-bottom-left-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-add {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  border: 1px dashed #d3d9e3;
  background: #fafbfd;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.upload-add-text {
  font-size: 12px;
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
