<script setup lang="ts">
/**
 * 工单详情页：对接 GET /api/orders/{id}。
 * - 顶部 wd-navbar（返回）
 * - 基础信息卡片：单号 / 标题 / 分类 / 报修人 / 维修师傅 / 当前状态 / 报修时间
 * - 操作日志时间线：列表渲染后端 logs 数据
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderDetail } from '@/api/order'
import { getToken } from '@/utils/token'
import { orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderDetailVO } from '@/types/api'

const detail = ref<OrderDetailVO | null>(null)
const loading = ref(true)
const loadError = ref(false)

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
        <view class="status-banner" :class="orderStatusCls(detail.status)">
          <view class="status-banner__inner">
            <text class="status-banner__label">当前状态</text>
            <text class="status-banner__value">{{ orderStatusLabel(detail.status) }}</text>
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

          <view v-if="detail.logs && detail.logs.length === 0" class="empty-logs">
            <wd-empty tip="暂无操作记录" />
          </view>

          <view v-else class="timeline">
            <view v-for="(log, index) in detail.logs" :key="log.id" class="timeline-item">
              <!-- 左侧时间轴 -->
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

    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg-page);
}

.body {
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
