<script setup lang="ts">
/**
 * 师傅工作台：我的维修任务列表。
 * - 顶部 navbar（右侧「个人中心」入口）
 * - wd-tabs 三态切换：待接单(ASSIGNED) / 维修中(PROCESSING) / 已完成(COMPLETED)
 * - 工单卡片列表：分页（触底加载）、下拉刷新、空态、加载失败重试
 */
import { ref } from 'vue'
import { onLoad, onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { repairerOrders } from '@/api/repairer'
import { getToken } from '@/utils/token'
import { orderStatusLabel, orderStatusCls } from '@/utils/status'
import type { OrderVO, OrderStatus } from '@/types/api'

const PAGE_SIZE = 10

const orders = ref<OrderVO[]>([])
/** 当前激活的 tab（对应工单状态码） */
const activeTab = ref<OrderStatus>('ASSIGNED')
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const initialLoading = ref(true)
const loadState = ref<'loading' | 'error' | 'finished'>('loading')
const loadError = ref(false)

const TABS: { name: OrderStatus; title: string }[] = [
  { name: 'ASSIGNED', title: '待接单' },
  { name: 'PROCESSING', title: '维修中' },
  { name: 'COMPLETED', title: '已完成' },
]

const hasMore = () => orders.value.length < total.value

/** 进入工作台前校验登录态 */
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
    const res = await repairerOrders(pageNum.value, PAGE_SIZE, activeTab.value)
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

/** wd-tabs change 事件：name 即 tab 状态码 */
function handleTabChange(e: { name: OrderStatus }) {
  const next = e.name
  if (activeTab.value === next) return
  activeTab.value = next
  fetchOrders(true)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/master/detail?id=${id}` })
}

/** 前往个人中心 */
function goProfile() {
  uni.navigateTo({ url: '/pages/user/profile' })
}

function formatTime(time: string): string {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 16)
}

onLoad(() => {
  ensureLogin()
  fetchOrders(true)
})

// 从详情页返回后刷新列表，保证状态同步
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
      title="师傅工作台"
      fixed
      placeholder
      safe-area-inset-top
      bordered
    >
      <template #right>
        <view class="nav-profile" @click="goProfile">
          <wd-icon name="user-circle" size="18px" custom-class="nav-profile__icon" />
          <text class="nav-profile__text">个人中心</text>
        </view>
      </template>
    </wd-navbar>

    <!-- 状态切换 tabs -->
    <wd-tabs :model-value="activeTab" line-theme="underline" color="#1890ff" @change="handleTabChange">
      <wd-tab
        v-for="item in TABS"
        :key="item.name"
        :name="item.name"
        :title="item.title"
      />
    </wd-tabs>

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
            <view class="order-card__addr-wrap">
              <wd-icon name="location" size="12px" custom-class="order-card__addr-icon" />
              <text class="order-card__addr">{{ order.address || '未填写地址' }}</text>
            </view>
          </view>
          <view class="order-card__foot">
            <view class="order-card__reporter-wrap">
              <wd-icon name="user" size="12px" custom-class="order-card__reporter-icon" />
              <text class="order-card__reporter">报修人：{{ order.reporterName || '—' }}</text>
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
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f4f7f9;
}

/* 导航栏右侧「个人中心」入口 */
.nav-profile {
  display: flex;
  align-items: center;
  padding: 2px 6px;
  color: #1890ff;
}

.nav-profile__icon {
  margin-right: 2px;
  color: #1890ff;
}

.nav-profile__text {
  font-size: 13px;
  color: #1890ff;
}

.order-list {
  padding: 12px 16px 0;
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

/* 工单卡片（与报修端 index.vue 卡片风格一致） */
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
  margin-bottom: 10px;
}

.order-card__addr-wrap {
  display: flex;
  align-items: center;
}

.order-card__addr-icon {
  margin-right: 4px;
  color: #909399;
}

.order-card__addr {
  flex: 1;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}

.order-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f2f2f2;
}

.order-card__reporter-wrap {
  display: flex;
  align-items: center;
}

.order-card__reporter-icon {
  margin-right: 4px;
  color: #909399;
}

.order-card__reporter {
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

.tag-reassigning {
  background: #f0f5ff;
  color: #2f54eb;
}

.tag-processing {
  background: #fff7e6;
  color: #fa8c16;
}

.tag-arbitrating {
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
