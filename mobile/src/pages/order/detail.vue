<script setup lang="ts">
/**
 * 工单详情页：对接 GET /api/orders/{id}。
 * - 顶部 wd-navbar（返回）
 * - 基础信息卡片：单号 / 标题 / 分类 / 报修人 / 维修师傅 / 当前状态 / 报修时间
 * - 操作日志时间线：列表渲染后端 logs 数据
 */
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderDetail, cancelOrder } from '@/api/order'
import { getToken } from '@/utils/token'
import { orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderDetailVO, OrderLogVO } from '@/types/api'

const detail = ref<OrderDetailVO | null>(null)
const loading = ref(true)
const loadError = ref(false)

/* ---------- 撤回弹窗（仅待受理 PENDING 可撤回） ---------- */
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelSubmitting = ref(false)

/** 跳转修改页（仅 PENDING 可进入） */
function goEdit() {
  if (!detail.value) return
  uni.navigateTo({ url: `/pages/order/edit?id=${detail.value.id}` })
}

/** 打开撤回弹窗 */
function openCancel() {
  cancelReason.value = ''
  cancelVisible.value = true
}

/** 确认撤回：调用 PUT /api/orders/{id}/cancel，成功后刷新详情（状态变 CANCELLED） */
async function submitCancel() {
  if (!detail.value) return
  if (!cancelReason.value.trim()) {
    uni.showToast({ title: '请输入撤回原因', icon: 'none' })
    return
  }
  cancelSubmitting.value = true
  try {
    await cancelOrder(detail.value.id, cancelReason.value.trim())
    uni.showToast({ title: '已撤回', icon: 'success' })
    cancelVisible.value = false
    await fetchDetail(detail.value.id)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    cancelSubmitting.value = false
  }
}

/** 驳回证据图片 URL 列表（applyImages 为 JSON 数组字符串） */
const applyImageList = computed<string[]>(() => {
  const raw = detail.value?.applyImages
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((v): v is string => typeof v === 'string' && !!v) : []
  } catch {
    return []
  }
})

/** 是否有可展示的驳回说明（状态为 REJECTED 且至少存在理由或证据图） */
const hasRejectNotice = computed(() => {
  if (!detail.value || detail.value.status !== 'REJECTED') return false
  return Boolean(detail.value.rejectReason) || applyImageList.value.length > 0
})

/** 预览驳回证据图片 */
function previewApplyImage(url: string) {
  uni.previewImage({ urls: applyImageList.value, current: url })
}

/**
 * 用户端操作日志：过滤师傅申请类动作，并将审批最终结果中性化。
 * - 过滤 REASSIGN_REQUEST（师傅申请改派）、ARBITRATE_REQUEST（师傅申请仲裁），用户不需要看。
 * - AUDIT 为审批最终结果，改写为不暴露「仲裁/改派」术语的中性文案。
 */
const visibleLogs = computed<OrderLogVO[]>(() => {
  const logs = detail.value?.logs ?? []
  return logs
    .filter((log) => log.action !== 'REASSIGN_REQUEST' && log.action !== 'ARBITRATE_REQUEST')
    .map((log) => {
      if (log.action !== 'AUDIT') return log
      const from = log.fromStatus
      const to = log.toStatus
      let remark = log.remark
      if (from === 'ARBITRATING' && to === 'REJECTED') {
        remark = '管理员已同意驳回，工单已关闭'
      } else if (from === 'ARBITRATING' && to === 'PROCESSING') {
        remark = '管理员已打回，师傅将继续维修'
      } else if (from === 'REASSIGNING' && to === 'PENDING') {
        remark = '已为您重新安排维修师傅'
      } else if (from === 'REASSIGNING' && to === 'ASSIGNED') {
        remark = '维修师傅将继续为您服务'
      }
      return { ...log, remark }
    })
})

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
        <view class="status-banner" :class="orderStatusCls(detail.status, 'reporter')">
          <view class="status-banner__inner">
            <text class="status-banner__label">当前状态</text>
            <text class="status-banner__value">{{ orderStatusLabel(detail.status, 'reporter') }}</text>
          </view>
        </view>

        <!-- 驳回说明（REJECTED 时显著展示师傅驳回理由与证据图） -->
        <view v-if="hasRejectNotice" class="card reject-card">
          <view class="section-title section-title--danger">驳回说明</view>
          <text v-if="detail.rejectReason" class="reject-reason">{{ detail.rejectReason }}</text>
          <view v-if="applyImageList.length" class="img-grid">
            <image
              v-for="(url, index) in applyImageList"
              :key="index"
              class="img-grid__item"
              :src="url"
              mode="aspectFill"
              @click="previewApplyImage(url)"
            />
          </view>
        </view>

        <!-- 基础信息卡片（键值对） -->
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
                <wd-icon name="user-group" size="14px" custom-class="kv-icon" />
                <text>维修师傅</text>
              </view>
              <text class="kv-value">{{ detail.repairerName || '待指派' }}</text>
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

          <view v-if="visibleLogs.length === 0" class="empty-logs">
            <wd-empty tip="暂无操作记录" />
          </view>

          <view v-else class="timeline">
            <view v-for="(log, index) in visibleLogs" :key="log.id" class="timeline-item">
              <!-- 左侧时间轴 -->
              <view class="timeline-axis">
                <view
                  class="timeline-dot"
                  :class="[
                    { 'is-first': index === 0 },
                    log.action ? 'dot-' + log.action.toLowerCase() : '',
                  ]"
                ></view>
                <view v-if="index < visibleLogs.length - 1" class="timeline-line"></view>
              </view>

              <!-- 右侧内容 -->
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

      <!-- 空态 -->
      <wd-empty v-else tip="工单不存在" />
    </view>

    <!-- 底部操作栏：报修人在「待受理（PENDING）」下可修改 / 撤回 -->
    <view v-if="detail && detail.status === 'PENDING'" class="action-bar">
      <wd-button plain custom-class="action-bar__ghost" @click="goEdit">修改</wd-button>
      <wd-button type="danger" custom-class="action-bar__danger" @click="openCancel">撤回</wd-button>
    </view>

    <!-- 撤回原因弹窗 -->
    <wd-popup
      v-model="cancelVisible"
      position="bottom"
      round
      safe-area-inset-bottom
      custom-class="popup-panel"
      @click-modal="cancelVisible = false"
    >
      <view class="popup-panel__title">撤回工单</view>
      <view class="popup-panel__body">
        <wd-textarea
          v-model="cancelReason"
          placeholder="请输入撤回原因"
          :maxlength="200"
          show-word-limit
          auto-height
        />
      </view>
      <view class="popup-panel__footer">
        <wd-button block custom-class="popup-panel__cancel" @click="cancelVisible = false">取消</wd-button>
        <wd-button
          block
          type="danger"
          :loading="cancelSubmitting"
          custom-class="popup-panel__confirm"
          @click="submitCancel"
        >
          确认撤回
        </wd-button>
      </view>
    </wd-popup>

    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg-page);
}

/* ===== 底部操作栏（仅 PENDING 显示） ===== */
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

:deep(.action-bar__ghost) {
  flex: 1;
  border-radius: 22px !important;
  border: 1px solid #d6e4f5 !important;
  background: var(--bg-hover) !important;
  color: #1890ff !important;
}

:deep(.action-bar__danger) {
  flex: 1;
  border-radius: 22px !important;
  background: linear-gradient(135deg, #ff7875 0%, #f5222d 100%) !important;
  border: none !important;
  color: #ffffff !important;
}

/* ===== 撤回弹层 ===== */
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
  background: linear-gradient(135deg, #ff7875 0%, #f5222d 100%) !important;
  border: none !important;
}

.body {
  padding: 16px;
  /* 预留底部操作栏高度，避免内容被遮挡 */
  padding-bottom: 100px;
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
  width: 96px;
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

.section-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.section-title--danger {
  color: #f5222d;
}

/* 驳回说明卡片 */
.reject-reason {
  display: block;
  margin-bottom: 12px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #f5222d;
  background: #fff1f0;
  border-radius: 8px;
  word-break: break-all;
}

/* 驳回证据图片网格 */
.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.img-grid__item {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  background: var(--bg-input);
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

/* 状态浅色胶囊标签（详情页顶部 Banner 复用） */
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
  color: var(--text-muted);
}

.tag-rejected {
  background: linear-gradient(135deg, #fff1f0, #ffccc7);
  color: #f5222d;
}
</style>
