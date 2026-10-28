<script setup lang="ts">
/**
 * 师傅工单详情页：对接 GET /api/orders/{id}。
 * - 基础信息 + 故障图片网格 + 报修人电话 + 操作日志时间线
 * - 底部固定操作栏按状态动态渲染：
 *   ASSIGNED  → 接单 / 申请改派
 *   PROCESSING→ 提交完工 / 申请驳回（恶意报修）
 */
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useToast, useDialog } from '@wot-ui/ui'
import { orderDetail, uploadImage } from '@/api/order'
import { acceptOrder, completeOrder, reassignRequest, arbitrationRequest } from '@/api/repairer'
import { getToken } from '@/utils/token'
import { orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderDetailVO } from '@/types/api'

// 用于表单校验的本地 toast（不随页面销毁，仅在校验分支内触发）
const toast = useToast()
/** 确认弹窗（wd-dialog，替换 uni.showModal，深色模式友好） */
const dialog = useDialog()

/** 申请证据图片上限 */
const MAX_APPLY_IMAGES = 3

const detail = ref<OrderDetailVO | null>(null)
const loading = ref(true)
const loadError = ref(false)

/** 故障图片 URL 列表（images 为 JSON 数组字符串） */
const imageList = computed<string[]>(() => {
  const raw = detail.value?.images
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((v) => typeof v === 'string' && v) : []
  } catch {
    return []
  }
})

/* ---------- 申请改派 / 申请仲裁 弹窗 ---------- */
const applyVisible = ref(false)
/** 当前申请类型：REASSIGN 改派 / ARBITRATE 仲裁 */
const applyType = ref<'REASSIGN' | 'ARBITRATE'>('REASSIGN')
const applyReason = ref('')
const applySubmitting = ref(false)
/** 申请证据图片 URL 列表（选填，最多 3 张） */
const applyImages = ref<string[]>([])
const applyUploading = ref(false)

/* ---------- 提交完工弹窗 ---------- */
const completeVisible = ref(false)
const repairResult = ref('')
const completeSubmitting = ref(false)

const applyTitle = computed(() => (applyType.value === 'REASSIGN' ? '申请改派' : '申请驳回（恶意报修）'))
const applyPlaceholder = computed(() =>
  applyType.value === 'REASSIGN'
    ? '请填写申请改派理由（如：临时有事无法接单）'
    : '请填写申请驳回理由（如：报修地址不实、非本类别故障等）',
)

function formatTime(time: string | null): string {
  if (!time) return '—'
  return time.replace('T', ' ').slice(0, 16)
}

async function fetchDetail(id: number) {
  loading.value = true
  loadError.value = false
  try {
    detail.value = await orderDetail(id)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 预览图片 */
function previewImage(url: string) {
  uni.previewImage({ urls: imageList.value, current: url })
}

/** 打开申请弹窗（改派 / 仲裁） */
function openApply(type: 'REASSIGN' | 'ARBITRATE') {
  // 互斥：确保完工弹窗关闭，避免两个弹层同时渲染造成重叠
  completeVisible.value = false
  applyType.value = type
  applyReason.value = ''
  applyImages.value = []
  applyVisible.value = true
}

/* ---------- 申请证据图片上传（复用报修页图片逻辑） ---------- */

/** 前端压缩图片（质量 70%）；H5 等不支持 uni.compressImage 的环境直接返回原图 */
function compressApplyImage(src: string): Promise<string> {
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

function chooseApplyImage() {
  if (applyUploading.value) return
  const remain = MAX_APPLY_IMAGES - applyImages.value.length
  if (remain <= 0) {
    toast.info(`最多上传 ${MAX_APPLY_IMAGES} 张图片`)
    return
  }
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const paths = (res.tempFilePaths as string[]) ?? []
      await handleApplyUpload(paths)
    },
  })
}

async function handleApplyUpload(paths: string[]) {
  if (!paths.length) return
  applyUploading.value = true
  uni.showLoading({ title: '上传中...', mask: true })
  try {
    for (const src of paths) {
      if (applyImages.value.length >= MAX_APPLY_IMAGES) break
      try {
        const compressed = await compressApplyImage(src)
        const url = await uploadImage(compressed)
        applyImages.value.push(url)
      } catch (err) {
        console.error('证据图片上传失败', err)
        uni.showToast({ title: '图片上传失败，请重试', icon: 'none' })
      }
    }
  } catch (err) {
    console.error('证据图片处理失败', err)
    uni.showToast({ title: '图片处理失败，请重试', icon: 'none' })
  } finally {
    uni.hideLoading()
    applyUploading.value = false
  }
}

function previewApplyImage(url: string) {
  uni.previewImage({ urls: applyImages.value, current: url })
}

function removeApplyImage(index: number) {
  applyImages.value.splice(index, 1)
}

/** 提交申请（改派 / 仲裁） */
async function submitApply() {
  if (!detail.value) return
  if (!applyReason.value.trim()) {
    toast.error('请填写申请理由')
    return
  }
  const applyImagesJson = applyImages.value.length
    ? JSON.stringify(applyImages.value)
    : undefined
  applySubmitting.value = true
  try {
    if (applyType.value === 'REASSIGN') {
      await reassignRequest(detail.value.id, applyReason.value.trim(), applyImagesJson)
      console.log('[repairer] 申请改派成功, orderId=', detail.value.id)
      // 用原生全局 toast，避免 navigateBack 卸载页面级 wd-toast 导致提示消失
      uni.showToast({ title: '申请改派已提交', icon: 'success' })
    } else {
      await arbitrationRequest(detail.value.id, applyReason.value.trim(), applyImagesJson)
      console.log('[repairer] 申请仲裁成功, orderId=', detail.value.id)
      uni.showToast({ title: '申请驳回已提交', icon: 'success' })
    }
    applyVisible.value = false
    setTimeout(() => uni.navigateBack(), 600)
  } catch (err) {
    console.error('[repairer] 提交申请失败', err)
  } finally {
    applySubmitting.value = false
  }
}

/** 接单：二次确认（wd-dialog，深色模式友好） */
async function handleAccept() {
  if (!detail.value) return
  try {
    const { action } = await dialog.confirm({
      title: '接单确认',
      msg: '确定接单并开始维修吗？',
      confirmButtonText: '确认接单',
      cancelButtonText: '取消',
    })
    if (action !== 'confirm') return
  } catch {
    return // 用户取消
  }

  try {
    await acceptOrder(detail.value!.id)
    console.log('[repairer] 接单成功, orderId=', detail.value!.id)
    uni.showToast({ title: '接单成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (err) {
    console.error('[repairer] 接单失败', err)
  }
}

/** 打开提交完工弹窗 */
function openComplete() {
  // 互斥：确保申请弹窗关闭，避免两个弹层同时渲染造成重叠
  applyVisible.value = false
  repairResult.value = ''
  completeVisible.value = true
}

/** 提交完工（维修结果描述为选填，未填写时使用默认文案） */
async function submitComplete() {
  if (!detail.value) return
  // 选填：不填描述时传默认值，避免后端记录为空
  const result = repairResult.value.trim() || '已维修完成'
  completeSubmitting.value = true
  try {
    await completeOrder(detail.value.id, result)
    console.log('[repairer] 提交完工成功, orderId=', detail.value.id)
    uni.showToast({ title: '已提交完工', icon: 'success' })
    completeVisible.value = false
    setTimeout(() => uni.navigateBack(), 600)
  } catch (err) {
    console.error('[repairer] 提交完工失败', err)
  } finally {
    completeSubmitting.value = false
  }
}

onLoad((query) => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  const id = Number(query?.id)
  if (!id || Number.isNaN(id)) {
    uni.showToast({ title: '工单参数缺失', icon: 'none' })
    return
  }
  fetchDetail(id)
})
</script>

<template>
  <view class="page">
    <wd-navbar
      title="工单详情"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      bordered
      @click-left="uni.navigateBack()"
    />

    <view class="body">
      <!-- 加载中 -->
      <view v-if="loading" class="tip">
        <wd-loading />
        <text class="tip-text">加载中...</text>
      </view>

      <!-- 加载失败 -->
      <view v-else-if="loadError" class="tip">
        <text class="tip-text">加载失败</text>
        <wd-button type="primary" size="small" round @click="fetchDetail(detail?.id ?? 0)">
          重试
        </wd-button>
      </view>

      <!-- 详情内容 -->
      <template v-else-if="detail">
        <!-- 状态 Banner -->
        <view class="status-banner" :class="orderStatusCls(detail.status, 'repairer')">
          <view class="status-banner__inner">
            <text class="status-banner__label">当前状态</text>
            <text class="status-banner__value">{{ orderStatusLabel(detail.status, 'repairer') }}</text>
          </view>
        </view>

        <!-- 基础信息卡片 -->
        <view class="card">
          <view class="card__head">
            <text class="card__hash">#</text>
            <text class="card__no">{{ detail.orderNo }}</text>
          </view>

          <view class="card__title">{{ detail.title }}</view>

          <!-- 故障描述 -->
          <view v-if="detail.description" class="desc-block">
            <text class="desc-label">故障描述</text>
            <text class="desc-text">{{ detail.description }}</text>
          </view>

          <!-- 故障图片 -->
          <view v-if="imageList.length" class="img-grid">
            <image
              v-for="(url, index) in imageList"
              :key="index"
              class="img-grid__item"
              :src="url"
              mode="aspectFill"
              @click="previewImage(url)"
            />
          </view>

          <view class="kv-list">
            <view class="kv-row">
              <view class="kv-label">
                <wd-icon name="tag" size="14px" custom-class="kv-icon" />
                <text>故障分类</text>
              </view>
              <text class="kv-value">{{ detail.categoryName || '未分类' }}</text>
            </view>
            <view class="kv-row">
              <view class="kv-label">
                <wd-icon name="location" size="14px" custom-class="kv-icon" />
                <text>报修地址</text>
              </view>
              <text class="kv-value">{{ detail.address || '未填写' }}</text>
            </view>
            <view class="kv-row">
              <view class="kv-label">
                <wd-icon name="user" size="14px" custom-class="kv-icon" />
                <text>报修人</text>
              </view>
              <text class="kv-value">{{ detail.reporterName || '—' }}</text>
            </view>
            <view class="kv-row">
              <view class="kv-label">
                <wd-icon name="phone" size="14px" custom-class="kv-icon" />
                <text>报修人电话</text>
              </view>
              <text class="kv-value kv-value--phone">{{ detail.contactPhone || '未填写' }}</text>
            </view>
            <view class="kv-row">
              <view class="kv-label">
                <wd-icon name="clock-circle" size="14px" custom-class="kv-icon" />
                <text>报修时间</text>
              </view>
              <text class="kv-value">{{ formatTime(detail.createdAt) }}</text>
            </view>
          </view>
        </view>

        <!-- 操作日志时间线 -->
        <view class="card">
          <view class="section-title">操作日志</view>

          <view v-if="detail.logs && detail.logs.length === 0" class="empty-logs">
            <wd-empty tip="暂无操作记录" />
          </view>

          <view v-else class="timeline">
            <view v-for="(log, index) in detail.logs" :key="log.id" class="timeline-item">
              <view class="timeline-axis">
                <view
                  class="timeline-dot"
                  :class="[
                    { 'is-first': index === 0 },
                    log.action ? 'dot-' + log.action.toLowerCase() : '',
                  ]"
                ></view>
                <view v-if="index < detail.logs.length - 1" class="timeline-line"></view>
              </view>

              <view class="timeline-content">
                <view class="timeline-head">
                  <text class="timeline-action">{{ log.remark || log.action }}</text>
                  <text class="timeline-time">{{ formatTime(log.createdAt) }}</text>
                </view>
                <view v-if="log.operatorName || log.operatorRole" class="timeline-sub">
                  {{ log.operatorName || '' }}
                  <text v-if="log.operatorRole" class="timeline-role">{{ log.operatorRole }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </template>

      <wd-empty v-else tip="工单不存在" />
    </view>

    <!-- 底部固定操作栏：仅 ASSIGNED / PROCESSING 显示 -->
    <view
      v-if="detail && (detail.status === 'ASSIGNED' || detail.status === 'PROCESSING')"
      class="action-bar"
    >
      <template v-if="detail.status === 'ASSIGNED'">
        <wd-button
          type="primary"
          custom-class="action-bar__primary"
          @click="handleAccept"
        >
          接单
        </wd-button>
        <wd-button
          plain
          custom-class="action-bar__ghost"
          @click="openApply('REASSIGN')"
        >
          申请改派
        </wd-button>
      </template>
      <template v-else-if="detail.status === 'PROCESSING'">
        <wd-button
          type="primary"
          custom-class="action-bar__primary"
          @click="openComplete"
        >
          提交完工
        </wd-button>
        <wd-button
          plain
          type="danger"
          custom-class="action-bar__ghost action-bar__ghost--danger"
          @click="openApply('ARBITRATE')"
        >
          申请驳回（恶意报修）
        </wd-button>
      </template>
    </view>

    <!-- 申请理由弹窗（改派 / 仲裁 共用） -->
    <wd-popup
      v-model="applyVisible"
      position="bottom"
      round
      :z-index="200"
      safe-area-inset-bottom
      custom-class="popup-panel"
      @click-modal="applyVisible = false"
    >
      <view class="popup-panel__title">{{ applyTitle }}</view>
      <view class="popup-panel__body">
        <wd-textarea
          v-model="applyReason"
          :placeholder="applyPlaceholder"
          :maxlength="200"
          show-word-limit
          auto-height
        />

        <!-- 申请证据图片（选填，最多 3 张） -->
        <view class="apply-upload">
          <view class="apply-upload__head">
            <text class="apply-upload__label">证据图片</text>
            <text class="apply-upload__tip">选填，最多 {{ MAX_APPLY_IMAGES }} 张</text>
          </view>
          <view class="apply-upload__grid">
            <view
              v-for="(img, idx) in applyImages"
              :key="img"
              class="apply-upload__item"
            >
              <image
                :src="img"
                mode="aspectFill"
                class="apply-upload__img"
                @click="previewApplyImage(img)"
              />
              <view class="apply-upload__remove" @click.stop="removeApplyImage(idx)">
                <wd-icon name="close" :size="10" color="#ffffff" />
              </view>
            </view>
            <view
              v-if="applyImages.length < MAX_APPLY_IMAGES"
              class="apply-upload__add"
              @click="chooseApplyImage"
            >
              <wd-icon name="camera" :size="20" color="#a8b0bd" />
              <text class="apply-upload__add-text">
                {{ applyUploading ? '上传中...' : '添加图片' }}
              </text>
            </view>
          </view>
        </view>
      </view>
      <view class="popup-panel__footer">
        <wd-button block custom-class="popup-panel__cancel" @click="applyVisible = false">取消</wd-button>
        <wd-button
          block
          type="primary"
          :loading="applySubmitting"
          custom-class="popup-panel__confirm"
          @click="submitApply"
        >
          提交申请
        </wd-button>
      </view>
    </wd-popup>

    <!-- 提交完工弹窗 -->
    <wd-popup
      v-model="completeVisible"
      position="bottom"
      round
      :z-index="200"
      safe-area-inset-bottom
      custom-class="popup-panel"
      @click-modal="completeVisible = false"
    >
      <view class="popup-panel__title">提交完工</view>
      <view class="popup-panel__body">
        <wd-textarea
          v-model="repairResult"
          placeholder="请填写维修结果描述（选填）"
          :maxlength="500"
          show-word-limit
          auto-height
        />
      </view>
      <view class="popup-panel__footer">
        <wd-button block custom-class="popup-panel__cancel" @click="completeVisible = false">取消</wd-button>
        <wd-button
          block
          type="primary"
          :loading="completeSubmitting"
          custom-class="popup-panel__confirm"
          @click="submitComplete"
        >
          确认完工
        </wd-button>
      </view>
    </wd-popup>

    <wd-toast />
    <wd-dialog />
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg-page);
}

.body {
  padding: 16px;
  /* 预留底部操作栏高度，避免内容被遮挡 */
  padding-bottom: 140px;
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
  color: var(--text-muted);
}

/* 状态 Banner（大号浅色渐变） */
.status-banner {
  margin-bottom: 12px;
  padding: 24px 20px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.status-banner__inner {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-banner__label {
  font-size: 13px;
  font-weight: 500;
  opacity: 0.7;
}

.status-banner__value {
  font-size: 26px;
  font-weight: 700;
}

.card {
  margin-bottom: 12px;
  padding: 16px;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.card__head {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.card__hash {
  margin-right: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.card__no {
  font-size: 12px;
  color: var(--text-muted);
}

.card__title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

/* 故障描述区块 */
.desc-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-input);
  border-radius: 8px;
}

.desc-label {
  font-size: 12px;
  color: var(--text-muted);
}

.desc-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-sub);
  word-break: break-all;
}

/* 故障图片网格 */
.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.img-grid__item {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  background: var(--bg-input);
}

/* 键值对列表 */
.kv-list {
  border-top: 1px solid var(--line);
}

.kv-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.kv-row:last-child {
  border-bottom: none;
}

.kv-label {
  display: flex;
  align-items: center;
  width: 120px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.kv-icon {
  margin-right: 4px;
  color: var(--text-muted);
}

.kv-value {
  flex: 1;
  font-size: 13px;
  color: var(--text-main);
  word-break: break-all;
}

.kv-value--phone {
  color: #1890ff;
  font-weight: 500;
}

.section-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.empty-logs {
  padding: 40rpx 0;
}

.timeline-item {
  display: flex;
}

.timeline-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 40rpx;
  margin-right: 12px;
  flex-shrink: 0;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--text-muted);
}

.timeline-dot.is-first {
  background: #1890ff;
}

/* 按动作区分的彩色圆点 */
.timeline-dot.dot-submit {
  background: #1890ff;
}
.timeline-dot.dot-assign {
  background: #2f54eb;
}
.timeline-dot.dot-accept {
  background: #fa8c16;
}
.timeline-dot.dot-finish {
  background: #52c41a;
}
.timeline-dot.dot-confirm {
  background: #52c41a;
}
.timeline-dot.dot-cancel {
  background: #909399;
}
.timeline-dot.dot-reject {
  background: #f5222d;
}
.timeline-dot.dot-remind {
  background: #fa8c16;
}
.timeline-dot.dot-update {
  background: #909399;
}
.timeline-dot.dot-reassign-request {
  background: #2f54eb;
}
.timeline-dot.dot-arbitrate-request {
  background: #fa8c16;
}
.timeline-dot.dot-audit {
  background: #722ed1;
}

.timeline-line {
  flex: 1;
  width: 2rpx;
  min-height: 40rpx;
  margin-top: 6px;
  background: var(--line);
}

.timeline-content {
  flex: 1;
  padding-bottom: 20px;
}

.timeline-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.timeline-action {
  flex: 1;
  margin-right: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  word-break: break-all;
}

.timeline-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.timeline-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.timeline-role {
  margin-left: 8px;
  padding: 2px 8px;
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  border-radius: 4px;
}

/* ===== 底部固定操作栏 ===== */
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  padding-bottom: calc(12px + constant(safe-area-inset-bottom));
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: var(--bg-card);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  z-index: 50;
}

.action-bar .wd-button {
  flex: 1;
}

:deep(.action-bar__primary) {
  flex: 1;
  border-radius: 22px !important;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important;
}

:deep(.action-bar__ghost) {
  flex: 1;
  border-radius: 22px !important;
  border: 1px solid #d6e4f5 !important;
  background: var(--bg-hover) !important;
  color: #1890ff !important;
}

:deep(.action-bar__ghost--danger) {
  border: 1px solid #ffccc7 !important;
  background: #fff1f0 !important;
  color: #f5222d !important;
}

/* ===== 底部弹层 ===== */
.popup-panel {
  padding: 20px 16px 16px;
  background: var(--bg-card);
}

.popup-panel__title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  text-align: center;
}

.popup-panel__body {
  margin-bottom: 16px;
}

/* ===== 申请证据图片上传 ===== */
.apply-upload {
  margin-top: 14px;
}

.apply-upload__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.apply-upload__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}

.apply-upload__tip {
  font-size: 12px;
  color: var(--text-muted);
}

.apply-upload__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.apply-upload__item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
}

.apply-upload__img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

.apply-upload__remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: rgba(0, 0, 0, 0.55);
  border-bottom-left-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.apply-upload__add {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  border: 1px dashed var(--line-strong);
  background: var(--bg-input);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.apply-upload__add-text {
  font-size: 11px;
  color: var(--text-muted);
}

.popup-panel__footer {
  display: flex;
  gap: 12px;
}

.popup-panel__footer .popup-panel__cancel {
  flex: 1;
  border-radius: 22px;
}

.popup-panel__footer .popup-panel__confirm {
  flex: 1;
  border-radius: 22px;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important;
}

/* 状态浅色胶囊标签（Banner 复用） */
.tag-pending {
  background: linear-gradient(135deg, #e6f7ff, #bae7ff);
  color: #1890ff;
}

.tag-assigned {
  background: linear-gradient(135deg, #f0f5ff, #d6e4ff);
  color: #2f54eb;
}

.tag-reassigning {
  background: linear-gradient(135deg, #f0f5ff, #d6e4ff);
  color: #2f54eb;
}

.tag-processing {
  background: linear-gradient(135deg, #fff7e6, #ffe7ba);
  color: #fa8c16;
}

.tag-arbitrating {
  background: linear-gradient(135deg, #fff7e6, #ffe7ba);
  color: #fa8c16;
}

.tag-completed,
.tag-confirmed {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #52c41a;
}

.tag-cancelled {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #909399;
}

.tag-rejected {
  background: linear-gradient(135deg, #fff1f0, #ffccc7);
  color: #f5222d;
}
</style>
