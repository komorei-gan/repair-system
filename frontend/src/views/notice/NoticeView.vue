<script setup lang="ts">
/**
 * 通知中心 —— 全部通知的分页卡片列表。
 * - 顶部操作栏：右侧「全部标记已读」（PUT /api/notifications/read-all）
 * - 主体：卡片列表（GET /api/notifications 分页）
 * - 点击单条：标记已读（PUT /api/notifications/{id}/read），有 orderId 则跳转工单详情（带 from 来源）
 * - 已读卡片提供「标记未读」（PUT /api/notifications/{id}/unread）
 * - 所有读/未读操作同步更新 Pinia 未读数，顶栏铃铛角标无感刷新
 */
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as notifApi from '@/api/notification'
import { useAppStore } from '@/stores/app'
import { NOTIFICATION_TYPE_META } from '@/utils/status'
import type { NotificationVO } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

/* ---------- 分页状态 ---------- */
const pageNum = ref(1)
const pageSize = ref(10)

/* ---------- 列表数据 ---------- */
const loading = ref(false)
const list = ref<NotificationVO[]>([])
const total = ref(0)
/** 「全部标记已读」提交中状态（防重复点击） */
const markingAll = ref(false)

/** 拉取通知分页数据 */
async function loadNotifications(): Promise<void> {
  loading.value = true
  try {
    const res = await notifApi.listNotifications(pageNum.value, pageSize.value)
    list.value = res.records ?? []
    total.value = Number(res.total ?? 0)
  } catch {
    // 错误提示已由 axios 拦截器统一处理；此处重置空态
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ---------- 分页 ---------- */
function onPageChange(p: number): void {
  pageNum.value = p
  void loadNotifications()
}

function onSizeChange(s: number): void {
  pageSize.value = s
  pageNum.value = 1
  void loadNotifications()
}

/* ---------- 全部标记已读 ---------- */
async function onMarkAll(): Promise<void> {
  if (markingAll.value) return
  markingAll.value = true
  try {
    await notifApi.markAllRead()
    ElMessage.success('已全部标记为已读')
    // 当前页数据同步置为已读（分页其他页数据在翻页时自然刷新）
    list.value = list.value.map((n) => ({ ...n, isRead: 1 as const }))
    // 未读数归零，顶栏铃铛角标无感消失
    appStore.setUnreadCount(0)
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    markingAll.value = false
  }
}

/* ---------- 点击单条通知：标记已读 + 跳转详情 ---------- */
async function onReadOne(item: NotificationVO): Promise<void> {
  if (item.isRead === 0) {
    // 乐观更新：先本地置为已读，接口失败再回滚
    item.isRead = 1
    try {
      await notifApi.markRead(item.id)
      appStore.adjustUnreadCount(-1)
    } catch {
      item.isRead = 0 // 失败回滚，保持未读状态
      return // 标记失败不跳转，避免用户漏读
    }
  }
  if (item.orderId !== null) {
    void router.push({
      path: `/orders/${item.orderId}`,
      query: { from: route.path },
    }).catch(() => {
      /* 忽略重复导航 / 导航被中止的 rejection */
    })
  }
}

/* ---------- 标记未读 ---------- */
async function onMarkUnread(item: NotificationVO): Promise<void> {
  if (item.isRead === 0) return
  // 乐观更新：先本地置为未读，接口失败再回滚
  item.isRead = 0
  try {
    await notifApi.markUnread(item.id)
    appStore.adjustUnreadCount(1)
  } catch {
    item.isRead = 1 // 失败回滚
  }
}

/** 相对时间格式化（与顶栏通知面板一致） */
function timeAgo(value: string): string {
  const diff = Date.now() - new Date(value.replace(' ', 'T')).getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return value.slice(5, 10)
}

onMounted(() => {
  void loadNotifications()
})
</script>

<template>
  <div class="notice-view">
    <!-- ============ 页头 + 操作栏 ============ -->
    <div class="page-head">
      <div>
        <h1>通知中心</h1>
        <p class="page-sub">共 <b>{{ total }}</b> 条通知 · 点击工单通知可跳转详情</p>
      </div>
      <el-button
        type="primary"
        :loading="markingAll"
        :disabled="total === 0"
        @click="onMarkAll"
      >
        全部标记已读
      </el-button>
    </div>

    <!-- ============ 通知卡片列表 ============ -->
    <div v-loading="loading" class="notice-body">
      <el-empty v-if="!loading && list.length === 0" description="暂无通知" />

      <article
        v-for="n in list"
        :key="n.id"
        class="card notice-card"
        :class="{ unread: n.isRead === 0 }"
        role="button"
        tabindex="0"
        @click="onReadOne(n)"
        @keydown.enter.prevent="onReadOne(n)"
      >
        <div class="notice-icon" :class="NOTIFICATION_TYPE_META[n.type]?.cls ?? 'notif-ico slate'">
          <IconSvg :name="n.type === 'ORDER' ? 'i-clipboard' : 'i-message'" :size="18" />
        </div>

        <div class="notice-main">
          <div class="notice-title-row">
            <b class="notice-title">{{ n.title }}</b>
            <span class="notice-type">{{ NOTIFICATION_TYPE_META[n.type]?.label ?? '通知' }}</span>
            <span v-if="n.isRead === 0" class="notice-dot" aria-label="未读"></span>
          </div>
          <p class="notice-content">{{ n.content }}</p>
          <time class="notice-time">{{ timeAgo(n.createdAt) }}</time>
        </div>

        <!-- 已读卡片：标记未读（阻止冒泡，避免触发卡片的已读+跳转） -->
        <button
          v-if="n.isRead === 1"
          type="button"
          class="mark-unread-btn"
          title="标记未读"
          @click.stop="onMarkUnread(n)"
        >
          标记未读
        </button>

        <IconSvg
          v-else-if="n.orderId !== null"
          name="i-chevron-right"
          :size="15"
          class="notice-arrow"
        />
      </article>
    </div>

    <!-- ============ 分页 ============ -->
    <div v-if="total > 0" class="pagination-bar">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.notice-view {
  min-width: 0;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.page-head h1 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.2;
  color: var(--ink);
}
.page-sub {
  font-size: 12.5px;
  color: var(--muted);
  margin-top: 6px;
}
.page-sub b {
  color: var(--blue-700);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ===== 卡片列表 ===== */
.notice-body {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  cursor: pointer;
  transition: box-shadow 0.18s, border-color 0.18s, transform 0.18s;
}
.notice-card:hover {
  border-color: var(--blue-200);
  box-shadow: 0 10px 24px -14px rgba(10, 42, 80, 0.28);
  transform: translateY(-1px);
}
.notice-card:focus-visible {
  outline: 2px solid var(--blue-500);
  outline-offset: -2px;
}

/* 未读：左侧蓝条 + 浅蓝渐变底 */
.notice-card.unread {
  position: relative;
  background: linear-gradient(90deg, #f6faff 0%, #ffffff 72%);
}
html.dark .notice-card.unread {
  background: linear-gradient(90deg, rgba(24, 144, 255, 0.12) 0%, var(--surface) 72%);
}
.notice-card.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--blue-500);
}

.notice-icon {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
}
.notice-icon.blue {
  background: var(--blue-50);
  color: var(--blue-600);
}
.notice-icon.green {
  background: var(--green-50);
  color: var(--green-600);
}
.notice-icon.amber {
  background: var(--amber-50);
  color: var(--amber-600);
}
.notice-icon.slate {
  background: #f0f5fb;
  color: #5e7a96;
}

.notice-main {
  min-width: 0;
  flex: 1;
}
.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-2);
  line-height: 1.4;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-type {
  flex: none;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--blue-700);
  background: var(--blue-50);
  padding: 1px 7px;
  border-radius: 6px;
}
.notice-dot {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
  background: var(--blue-500);
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.13);
}
.notice-content {
  font-size: 12.5px;
  color: #6b8096;
  margin-top: 4px;
  line-height: 1.6;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notice-time {
  display: block;
  font-size: 11px;
  color: #8ba0b5;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}

.notice-arrow {
  flex: none;
  color: #b6c6d7;
  margin-top: 6px;
  transition: color 0.18s, transform 0.18s;
}
.notice-card:hover .notice-arrow {
  color: var(--blue-500);
  transform: translateX(2px);
}

/* ===== 标记未读按钮 ===== */
.mark-unread-btn {
  flex: none;
  align-self: center;
  border: 1px solid var(--line-strong);
  border-radius: 7px;
  background: var(--surface);
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 10px;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.18s, border-color 0.18s, background 0.18s;
}
.mark-unread-btn:hover {
  color: var(--blue-700);
  border-color: var(--blue-200);
  background: #f2f8ff;
}
.mark-unread-btn:focus-visible {
  outline: 2px solid var(--blue-500);
  outline-offset: 2px;
}

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

/* ===== 响应式 ===== */
@media (max-width: 780px) {
  .pagination-bar {
    justify-content: center;
    overflow-x: auto;
  }
  .notice-card {
    padding: 14px;
  }
}
</style>
