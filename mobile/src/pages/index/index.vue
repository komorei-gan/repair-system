<script setup lang="ts">
/**
 * 首页：我的工单列表。
 * - 顶部 navbar（右侧「+ 提交报修」入口）
 * - 状态筛选 tabs
 * - 工单卡片列表：分页（触底加载）、下拉刷新、空态、加载失败重试
 */
import { ref, computed } from 'vue'
import { onLoad, onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useToast } from '@wot-ui/ui'
import { myOrders, cancelOrder, confirmOrder } from '@/api/order'
import { clearToken, getCachedUser, getToken } from '@/utils/token'
import { STATUS_FILTERS, orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderVO, OrderStatus, UserInfo } from '@/types/api'

const toast = useToast()

/** 本地缓存的登录用户（用于导航栏标题展示） */
const user = ref<UserInfo | null>(getCachedUser<UserInfo>())
const displayName = computed(() => user.value?.realName || user.value?.username || '')

const PAGE_SIZE = 10

const orders = ref<OrderVO[]>([])
const activeFilter = ref<OrderStatus | ''>('')
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const initialLoading = ref(true)
const loadState = ref<'loading' | 'error' | 'finished'>('loading')
const loadError = ref(false)

// 撤回弹窗
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelTargetId = ref<number | null>(null)
const cancelSubmitting = ref(false)

// 评价弹窗
const rateVisible = ref(false)
const rating = ref(0)
const ratingComment = ref('')
const rateTargetId = ref<number | null>(null)
const rateSubmitting = ref(false)

const hasMore = () => orders.value.length < total.value

/** 进入首页前校验登录态 */
function ensureLogin(): boolean {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

async function fetchOrders(reset = false) {
  if (!ensureLogin()) return
  if (reset) {
    pageNum.value = 1
    orders.value = []
  }
  loading.value = true
  loadError.value = false
  loadState.value = 'loading'
  try {
    const res = await myOrders(pageNum.value, PAGE_SIZE, activeFilter.value || undefined)
    total.value = res.total
    orders.value = reset ? res.records : [...orders.value, ...res.records]
    loadState.value = 'finished'
  } catch {
    loadError.value = true
    loadState.value = 'error'
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

function loadMore() {
  if (loading.value || loadError.value) return
  if (!hasMore()) return
  pageNum.value += 1
  fetchOrders(false)
}

function switchFilter(value: OrderStatus | '') {
  if (activeFilter.value === value) return
  activeFilter.value = value
  fetchOrders(true)
}

function goCreate() {
  uni.navigateTo({ url: '/pages/order/create' })
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages/order/edit?id=${id}` })
}

/** 退出登录：二次确认后清除凭证并回到登录页 */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    confirmColor: '#f5222d',
    success: (res) => {
      if (!res.confirm) return
      // 清除本地凭证（等价于 removeStorageSync('repair_token' / 'repair_user')）
      clearToken()
      uni.reLaunch({ url: '/pages/login/login' })
    },
  })
}

/** 打开撤回弹窗 */
function openCancel(order: OrderVO) {
  cancelTargetId.value = order.id
  cancelReason.value = ''
  cancelVisible.value = true
}

/** 确认撤回：二次确认后调用 cancel 接口 */
async function submitCancel() {
  if (cancelTargetId.value === null) return
  if (!cancelReason.value.trim()) {
    toast.error('请输入撤回原因')
    return
  }
  cancelSubmitting.value = true
  try {
    await cancelOrder(cancelTargetId.value, cancelReason.value.trim())
    toast.success('已撤回')
    cancelVisible.value = false
    fetchOrders(true)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    cancelSubmitting.value = false
  }
}

/** 打开评价弹窗 */
function openRate(order: OrderVO) {
  rateTargetId.value = order.id
  rating.value = 0
  ratingComment.value = ''
  rateVisible.value = true
}

/** 提交评价：星级 + 评价内容调用 confirm 接口 */
async function submitRate() {
  if (rateTargetId.value === null) return
  if (rating.value < 1) {
    toast.error('请评分')
    return
  }
  rateSubmitting.value = true
  try {
    await confirmOrder(rateTargetId.value, {
      rating: rating.value,
      ratingComment: ratingComment.value || undefined,
    })
    toast.success('评价成功')
    rateVisible.value = false
    fetchOrders(true)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    rateSubmitting.value = false
  }
}

function formatTime(time: string): string {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

onLoad(() => {
  ensureLogin()
  fetchOrders(true)
})

// 提交报修返回后刷新列表
onShow(() => {
  if (!initialLoading.value) {
    fetchOrders(true)
  }
})

onPullDownRefresh(() => {
  fetchOrders(true).finally(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  loadMore()
})
</script>

<template>
  <view class="page">
    <wd-navbar
      :title="displayName ? `${displayName}的报修` : '我的工单'"
      fixed
      placeholder
      safe-area-inset-top
      bordered
    >
      <template #right>
        <view class="nav-logout" @click="handleLogout">
          <wd-icon name="poweroff" size="16px" custom-class="nav-logout__icon" />
          <text class="nav-logout__text">退出</text>
        </view>
      </template>
    </wd-navbar>

    <!-- 状态筛选（胶囊分段控制器） -->
    <view class="filter-wrap">
      <scroll-view scroll-x class="filter-scroll">
        <view class="filter-inner">
          <view
            v-for="item in STATUS_FILTERS"
            :key="item.value"
            :class="['filter-item', activeFilter === item.value ? 'is-active' : '']"
            @click="switchFilter(item.value)"
          >
            {{ item.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 工单列表 -->
    <view class="order-list">
      <view v-if="initialLoading" class="list-tip">
        <wd-loading />
        <text class="list-tip-text">加载中...</text>
      </view>

      <wd-empty v-else-if="!loadError && orders.length === 0" tip="暂无工单" />

      <view v-else-if="loadError" class="list-tip">
        <text class="list-tip-text">加载失败</text>
        <wd-button type="primary" size="small" round @click="fetchOrders(true)">重试</wd-button>
      </view>

      <template v-else>
        <view
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          hover-class="order-card--active"
          hover-stay-time="80"
          @click="goDetail(order.id)"
        >
          <view class="order-card__head">
            <view class="order-card__no-wrap">
              <text class="order-card__hash">#</text>
              <text class="order-card__no">{{ order.orderNo }}</text>
            </view>
            <text class="order-tag" :class="orderStatusCls(order.status)">{{ orderStatusLabel(order.status) }}</text>
          </view>
          <view class="order-card__title">{{ order.title }}</view>
          <view class="order-card__meta">
            <view class="order-card__cat-wrap">
              <wd-icon name="tag" size="12px" custom-class="order-card__cat-icon" />
              <text class="order-card__category">{{ order.categoryName || '未分类' }}</text>
            </view>
            <view class="order-card__time-wrap">
              <wd-icon name="clock-circle" size="12px" custom-class="order-card__time-icon" />
              <text class="order-card__time">{{ formatTime(order.createdAt) }}</text>
            </view>
          </view>

          <!-- 状态操作区：PENDING 可修改/撤回，COMPLETED 可评价 -->
          <view
            v-if="order.status === 'PENDING' || order.status === 'COMPLETED'"
            class="order-card__actions"
            @click.stop
          >
            <template v-if="order.status === 'PENDING'">
              <view class="order-card__action" @click="goEdit(order.id)">修改</view>
              <view class="order-card__action order-card__action--danger" @click="openCancel(order)">撤回</view>
            </template>
            <template v-else-if="order.status === 'COMPLETED'">
              <view class="order-card__action order-card__action--primary" @click="openRate(order)">评价</view>
            </template>
          </view>
        </view>

        <wd-loadmore :state="loadState" loading-text="加载中" finished-text="没有更多了" />
      </template>
    </view>

    <!-- 右下角悬浮「提交报修」按钮 -->
    <view class="fab-btn" @click="goCreate">
      <wd-icon name="plus" size="24px" custom-class="fab-btn__icon" />
    </view>

    <!-- 撤回弹窗 -->
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

    <!-- 评价弹窗 -->
    <wd-popup
      v-model="rateVisible"
      position="bottom"
      round
      safe-area-inset-bottom
      custom-class="popup-panel"
      @click-modal="rateVisible = false"
    >
      <view class="popup-panel__title">评价工单</view>
      <view class="popup-panel__body">
        <view class="rate-wrap">
          <wd-rate v-model="rating" :num="5" size="32px" space="12px" />
          <text class="rate-text">{{ rating > 0 ? `${rating} 分` : '请点击星星评分' }}</text>
        </view>
        <wd-textarea
          v-model="ratingComment"
          placeholder="说说这次维修体验（选填）"
          :maxlength="200"
          show-word-limit
          auto-height
        />
      </view>
      <view class="popup-panel__footer">
        <wd-button
          block
          type="primary"
          :loading="rateSubmitting"
          custom-class="popup-panel__confirm popup-panel__confirm--full"
          @click="submitRate"
        >
          提交评价
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

/* 导航栏右侧「退出」按钮（淡灰，不喧宾夺主） */
.nav-logout {
  display: flex;
  align-items: center;
  padding: 2px 6px;
  color: #a8b0bd;
}

.nav-logout__icon {
  margin-right: 2px;
  color: #a8b0bd;
}

.nav-logout__text {
  font-size: 13px;
  color: #a8b0bd;
}

/* 右下角悬浮按钮（原生 view） */
.fab-btn {
  position: fixed;
  right: 20px;
  bottom: 40px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff, #36cfc9);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  z-index: 99;
}

.fab-btn__icon {
  color: #fff;
}

.filter-wrap {
  position: sticky;
  top: 0;
  z-index: 9;
  background: #f4f7f9;
  padding: 8px 16px;
}

.filter-scroll {
  white-space: nowrap;
}

/*
 * 隐藏 scroll-view 的横向滚动条（H5 端默认显示的灰色滑动指示线），
 * 避免与胶囊分段控制器的视觉风格冲突。
 * 小程序端本身不渲染该滚动条，此规则无副作用。
 */
.filter-scroll :deep(*)::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
  background: transparent;
}

.filter-scroll :deep(*) {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.filter-inner {
  display: flex;
  padding: 4px;
  background: #e8eaed;
  border-radius: 20px;
}

.filter-item {
  flex-shrink: 0;
  margin-right: 4px;
  padding: 8px 20px;
  font-size: 13px;
  color: #606266;
  border-radius: 16px;
}

.filter-item:last-child {
  margin-right: 0;
}

.filter-item.is-active {
  color: #1890ff;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.order-list {
  padding: 0 16px;
}

.list-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding: 120rpx 0;
}

.list-tip-text {
  font-size: 26rpx;
  color: #8a94a6;
}

/* 工单卡片 */
.order-card {
  margin-bottom: 12px;
  padding: 16px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.order-card--active {
  transform: scale(0.98);
}

.order-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-card__no-wrap {
  display: flex;
  align-items: center;
}

.order-card__hash {
  margin-right: 4px;
  font-size: 12px;
  color: #909399;
}

.order-card__no {
  font-size: 12px;
  color: #909399;
}

.order-card__title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.order-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-card__cat-wrap {
  display: flex;
  align-items: center;
  padding: 2px 8px;
  background: #f0f2f5;
  border-radius: 4px;
}

.order-card__cat-icon {
  margin-right: 4px;
  color: #909399;
}

.order-card__category {
  font-size: 12px;
  color: #606266;
}

.order-card__time-wrap {
  display: flex;
  align-items: center;
}

.order-card__time-icon {
  margin-right: 4px;
  color: #909399;
}

.order-card__time {
  font-size: 12px;
  color: #909399;
}

/* 卡片底部操作区 */
.order-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f2f2f2;
}

.order-card__action {
  padding: 6px 18px;
  font-size: 13px;
  color: #606266;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  background: #ffffff;
}

.order-card__action--primary {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, #1890ff, #36cfc9);
}

.order-card__action--danger {
  color: #f5222d;
  border-color: #ffccc7;
  background: #fff1f0;
}

/* 底部弹层面板 */
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

.rate-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.rate-text {
  font-size: 12px;
  color: #909399;
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
}

.popup-panel__footer .popup-panel__confirm--full {
  flex: none;
  width: 100%;
}

/* 状态浅色胶囊标签 */
.order-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.tag-pending {
  background: #e6f7ff;
  color: #1890ff;
}

.tag-assigned {
  background: #f0f5ff;
  color: #2f54eb;
}

.tag-processing {
  background: #fff7e6;
  color: #fa8c16;
}

.tag-completed,
.tag-confirmed {
  background: #f6ffed;
  color: #52c41a;
}

.tag-cancelled {
  background: #f5f5f5;
  color: #909399;
}

.tag-rejected {
  background: #fff1f0;
  color: #f5222d;
}
</style>
