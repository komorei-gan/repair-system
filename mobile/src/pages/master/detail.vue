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
import { useToast } from '@wot-ui/ui'
import { orderDetail } from '@/api/order'
import { acceptOrder, completeOrder, reassignRequest, arbitrationRequest } from '@/api/repairer'
import { getToken } from '@/utils/token'
import { orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderDetailVO } from '@/types/api'

// 用于表单校验的本地 toast（不随页面销毁，仅在校验分支内触发）
const toast = useToast()

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
  applyVisible.value = true
}

/** 提交申请（改派 / 仲裁） */
async function submitApply() {
  if (!detail.value) return
  if (!applyReason.value.trim()) {
    toast.error('请填写申请理由')
    return
  }
  applySubmitting.value = true
  try {
    if (applyType.value === 'REASSIGN') {
      await reassignRequest(detail.value.id, applyReason.value.trim())
      console.log('[repairer] 申请改派成功, orderId=', detail.value.id)
      // 用原生全局 toast，避免 navigateBack 卸载页面级 wd-toast 导致提示消失
      uni.showToast({ title: '申请改派已提交', icon: 'success' })
    } else {
      await arbitrationRequest(detail.value.id, applyReason.value.trim())
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

/** 接单：二次确认 */
function handleAccept() {
  if (!detail.value) return
  uni.showModal({
    title: '接单确认',
    content: '确定接单并开始维修吗？',
    confirmColor: '#1890ff',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await acceptOrder(detail.value!.id)
        console.log('[repairer] 接单成功, orderId=', detail.value!.id)
        uni.showToast({ title: '接单成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
      } catch (err) {
        console.error('[repairer] 接单失败', err)
      }
    },
  })
}

/** 打开提交完工弹窗 */
function openComplete() {
  // 互斥：确保申请弹窗关闭，避免两个弹层同时渲染造成重叠
  applyVisible.value = false
  repairResult.value = ''
  completeVisible.value = true
}

/** 提交完工 */
async function submitComplete() {
  if (!detail.value) return
  if (!repairResult.value.trim()) {
    toast.error('请填写维修结果')
    return
  }
  completeSubmitting.value = true
  try {
    await completeOrder(detail.value.id, repairResult.value.trim())
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
        <view class="status-banner" :class="orderStatusCls(detail.status)">
          <view class="status-banner__inner">
            <text class="status-banner__label">当前状态</text>
            <text class="status-banner__value">{{ orderStatusLabel(detail.status) }}</text>
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
          placeholder="请填写维修结果描述"
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
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f4f7f9;
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
  color: #909399;
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
  background: #ffffff;
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
  color: #909399;
}

.card__no {
  font-size: 12px;
  color: #909399;
}

.card__title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

/* 故障描述区块 */
.desc-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f4f7f9;
  border-radius: 8px;
}

.desc-label {
  font-size: 12px;
  color: #909399;
}

.desc-text {
  font-size: 14px;
  line-height: 1.6;
  color: #3a4655;
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
  background: #f0f2f5;
}

/* 键值对列表 */
.kv-list {
  border-top: 1px solid #f2f2f2;
}

.kv-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f2f2f2;
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
  color: #909399;
}

.kv-icon {
  margin-right: 4px;
  color: #909399;
}

.kv-value {
  flex: 1;
  font-size: 13px;
  color: #1f2d3d;
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
  color: #1f2d3d;
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
  background: #c9d2de;
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
  background: #e6ebf2;
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
  color: #1f2d3d;
  word-break: break-all;
}

.timeline-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}

.timeline-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
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
  background: #ffffff;
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
  background: #f5f9ff !important;
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
  background: #ffffff;
}

.popup-panel__title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
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
