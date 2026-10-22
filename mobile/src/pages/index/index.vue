<script setup lang="ts">
/**
 * 首页：我的工单列表。
 * - 顶部 navbar（右侧「+ 提交报修」入口）
 * - 状态筛选 tabs
 * - 工单卡片列表：分页（触底加载）、下拉刷新、空态、加载失败重试
 */
import { ref } from 'vue'
import { onLoad, onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { myOrders } from '@/api/order'
import { getToken } from '@/utils/token'
import { STATUS_FILTERS, orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderVO, OrderStatus } from '@/types/api'

const PAGE_SIZE = 10

const orders = ref<OrderVO[]>([])
const activeFilter = ref<OrderStatus | ''>('')
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const initialLoading = ref(true)
const loadState = ref<'loading' | 'error' | 'finished'>('loading')
const loadError = ref(false)

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
      title="我的工单"
      fixed
      placeholder
      safe-area-inset-top
      bordered
    />

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
        </view>

        <wd-loadmore :state="loadState" loading-text="加载中" finished-text="没有更多了" />
      </template>
    </view>

    <!-- 右下角悬浮「提交报修」按钮 -->
    <view class="fab-btn" @click="goCreate">
      <wd-icon name="plus" size="24px" custom-class="fab-btn__icon" />
    </view>

    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f4f7f9;
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
