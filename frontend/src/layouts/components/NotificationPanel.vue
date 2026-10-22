<script setup lang="ts">
/**
 * 顶栏通知面板 —— 样式复刻 design-web.html；
 * 数据接真实接口：GET /api/notifications + 全部已读（后端未启动时静默降级为空态）。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as notifApi from '@/api/notification'
import { useAppStore } from '@/stores/app'
import { NOTIFICATION_TYPE_META } from '@/utils/status'
import type { NotificationVO } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'

const router = useRouter()
const appStore = useAppStore()

const list = ref<NotificationVO[]>([])
const unreadTotal = ref(0)
const loading = ref(false)
const markAllDone = ref(false)

const unreadCount = computed(() => list.value.filter((n) => n.isRead === 0).length)

/** 加载前 5 条通知 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await notifApi.listNotifications(1, 5)
    list.value = res.records
    unreadTotal.value = res.total
  } catch {
    // 后端未启动等场景静默降级，保留空态
  } finally {
    loading.value = false
  }
}

/** 全部已读 */
async function onMarkAll(): Promise<void> {
  if (markAllDone.value || unreadCount.value === 0) return
  try {
    await notifApi.markAllRead()
    list.value = list.value.map((n) => ({ ...n, isRead: 1 as const }))
    markAllDone.value = true
    // 通知父组件刷新角标
    emit('read-all')
  } catch {
    /* 静默失败 */
  }
}

/**
 * 点击单条通知：
 * 1) 调用 PUT /api/notifications/{id}/read 标记该条已读（幂等，失败静默不阻断跳转）；
 * 2) 关闭通知面板；
 * 3) 工单通知（有 orderId）跳转工单详情页。
 */
async function onReadOne(item: NotificationVO): Promise<void> {
  if (item.isRead === 0) {
    try {
      await notifApi.markRead(item.id)
      item.isRead = 1
      // 通知父组件刷新未读角标
      emit('read-all')
    } catch {
      /* 已读标记失败不阻断跳转 */
    }
  }
  appStore.toggleNotif(false)
  if (item.orderId !== null) {
    void router.push(`/orders/${item.orderId}`)
  }
}

const emit = defineEmits<{ 'read-all': []; 'view-all': [] }>()

/** 查看全部通知：跳转与关闭面板交由父组件（TopBar）统一处理 */
function onViewAll(): void {
  emit('view-all')
}

/** 相对时间格式化 */
function timeAgo(value: string): string {
  const diff = Date.now() - new Date(value.replace(' ', 'T')).getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return value.slice(5, 10)
}

onMounted(load)

defineExpose({ load })
</script>

<template>
  <div class="notif-panel" role="dialog" aria-label="消息通知">
    <div class="notif-head">
      <h3>消息通知</h3>
      <span class="count-chip">{{ unreadCount }} 条未读</span>
      <button type="button" :disabled="markAllDone || unreadCount === 0" @click="onMarkAll">
        {{ markAllDone ? '已全部读' : '全部已读' }}
      </button>
    </div>

    <div class="notif-scroll">
      <p v-if="loading" class="empty">加载中…</p>
      <p v-else-if="list.length === 0" class="empty">暂无通知</p>
      <article
        v-for="n in list"
        :key="n.id"
        class="notif-item"
        :class="{ unread: n.isRead === 0 }"
        role="button"
        tabindex="0"
        @click="onReadOne(n)"
        @keydown.enter.prevent="onReadOne(n)"
      >
        <div class="notif-ico" :class="NOTIFICATION_TYPE_META[n.type]?.cls ?? 'notif-ico slate'">
          <IconSvg :name="n.type === 'ORDER' ? 'i-clipboard' : 'i-message'" :size="16" />
        </div>
        <div class="notif-body">
          <b>{{ n.title }}</b>
          <p>{{ n.content }}</p>
          <time>{{ timeAgo(n.createdAt) }}</time>
        </div>
        <span v-if="n.isRead === 0" class="notif-dot" aria-hidden="true"></span>
      </article>
    </div>

    <button type="button" class="notif-foot" @click="onViewAll">
      查看全部通知
      <IconSvg name="i-chevron-right" :size="14" />
    </button>
  </div>
</template>

<style scoped>
.notif-panel {
  position: absolute;
  top: calc(100% + 13px);
  right: -2px;
  z-index: 80;
  width: 364px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow-pop);
  overflow: visible;
}
.notif-panel::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 19px;
  width: 10px;
  height: 10px;
  background: #fff;
  border-left: 1px solid var(--line);
  border-top: 1px solid var(--line);
  transform: rotate(45deg);
  border-radius: 2px 0 0 0;
}
.notif-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #f1f6fc;
}
.notif-head h3 {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.notif-head .count-chip {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--blue-700);
  background: var(--blue-50);
  padding: 1px 7px;
  border-radius: 6px;
}
.notif-head button {
  margin-left: auto;
  border: 0;
  background: none;
  cursor: pointer;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--blue-600);
  padding: 2px 4px;
  border-radius: 5px;
}
.notif-head button:hover:not(:disabled) {
  color: var(--blue-700);
  background: #f2f8ff;
}
.notif-head button:disabled {
  color: var(--muted-2);
  cursor: default;
}
.notif-scroll {
  max-height: 302px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #dee9f4 transparent;
}
.notif-scroll::-webkit-scrollbar {
  width: 5px;
}
.notif-scroll::-webkit-scrollbar-thumb {
  background: #e2ebf5;
  border-radius: 99px;
}
.empty {
  padding: 28px 16px;
  text-align: center;
  font-size: 12px;
  color: var(--muted-2);
}
.notif-item {
  position: relative;
  display: flex;
  gap: 11px;
  padding: 13px 18px 13px 16px;
  border-bottom: 1px solid #f4f8fd;
  cursor: pointer;
  transition: background 0.16s;
}
.notif-item:last-child {
  border-bottom: 0;
}
.notif-item:hover {
  background: #f8fbff;
}
.notif-item:focus-visible {
  outline: 2px solid var(--blue-500);
  outline-offset: -2px;
}
.notif-item.unread {
  background: linear-gradient(90deg, #f6faff 0%, #ffffff 72%);
}
.notif-item.unread:hover {
  background: #f5faff;
}
.notif-ico {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
}
.notif-ico.blue {
  background: var(--blue-50);
  color: var(--blue-600);
}
.notif-ico.green {
  background: var(--green-50);
  color: var(--green-600);
}
.notif-ico.amber {
  background: var(--amber-50);
  color: var(--amber-600);
}
.notif-ico.slate {
  background: #f0f5fb;
  color: #5e7a96;
}
.notif-body {
  min-width: 0;
  flex: 1;
}
.notif-body b {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-2);
  line-height: 1.45;
  padding-right: 14px;
}
.notif-body p {
  font-size: 11.5px;
  color: #6b8096;
  margin-top: 2px;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
.notif-body time {
  display: block;
  font-size: 10.5px;
  color: #6b8095;
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}
.notif-dot {
  position: absolute;
  right: 16px;
  top: 17px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--blue-500);
}
.notif-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-top: 1px solid #f1f6fc;
  background: none;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  color: var(--blue-600);
  cursor: pointer;
  border-radius: 0 0 13px 13px;
  transition: background 0.16s;
}
.notif-foot:hover {
  background: #f7fbff;
}

@media (max-width: 780px) {
  .notif-panel {
    position: fixed;
    top: 66px;
    left: 12px;
    right: 12px;
    width: auto;
    max-width: none;
    transform-origin: top center;
  }
  .notif-panel::before {
    display: none;
  }
  .notif-scroll {
    max-height: min(46vh, 320px);
  }
}
</style>
