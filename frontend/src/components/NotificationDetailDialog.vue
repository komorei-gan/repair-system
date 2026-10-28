<script setup lang="ts">
/**
 * 通知详情弹窗 —— “智能提示弹窗”体验。
 * - 头部展示通知标题；主体完整展示内容与发送时间（不截断）。
 * - 底部按通知文案智能映射跳转按钮；普通通知仅显示【我知道了】。
 * - 存在 orderId 时提供“查看工单详情”文字链接，并异步拉取该工单的证据图（applyImages）。
 */
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import * as orderApi from '@/api/order'
import type { NotificationVO } from '@/types/api'

const props = defineProps<{
  notification: Pick<NotificationVO, 'title' | 'content' | 'createdAt' | 'orderId'> | null
}>()

/** 弹窗显隐（v-model） */
const visible = defineModel<boolean>({ default: false })

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

/** 归一化展示时间：YYYY-MM-DD HH:mm:ss（兼容 T 分隔与带毫秒的 ISO 串） */
const displayTime = computed(() => {
  const raw = props.notification?.createdAt ?? ''
  return raw.replace('T', ' ').slice(0, 19)
})

/** 拼接 title + content 后的匹配文本 */
const matchText = computed(() => {
  const n = props.notification
  return `${n?.title ?? ''}${n?.content ?? ''}`
})

/** 智能跳转映射结果：null 表示普通通知，不显示跳转按钮 */
const smartAction = computed<{ label: string; path: string } | null>(() => {
  const text = matchText.value
  if (text.includes('密码重置')) {
    return { label: '前往用户管理', path: '/users' }
  }
  if (text.includes('师傅注册')) {
    return { label: '前往师傅审核', path: '/repairer-audit' }
  }
  if (text.includes('申请改派') || text.includes('申请仲裁')) {
    return { label: '前往仲裁中心', path: '/arbitration' }
  }
  // 改派审批通过后的通知（“改派申请已通过/重新指派”）→ 派单中心
  if (text.includes('改派申请') || text.includes('重新指派')) {
    return { label: '前往派单中心', path: '/dispatch' }
  }
  if (text.includes('待受理') || text.includes('新报修')) {
    return { label: '前往派单中心', path: '/dispatch' }
  }
  return null
})

const hasOrderId = computed(() => props.notification?.orderId != null)

/* ---------- 证据图（applyImages）异步拉取 ---------- */
const applyImages = ref<string[]>([])
const loadingImages = ref(false)

/** 解析 applyImages JSON 字符串为 URL 数组（解析失败返回空数组） */
function parseApplyImages(raw?: string | null): string[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((v): v is string => typeof v === 'string' && v !== '') : []
  } catch {
    return []
  }
}

/** 打开弹窗时（orderId 存在）异步拉取工单详情并解析证据图；失败静默置空 */
watch(
  () => props.notification,
  (n) => {
    applyImages.value = []
    loadingImages.value = false
    const orderId = n?.orderId
    if (orderId == null) return
    loadingImages.value = true
    orderApi
      .orderDetail(orderId)
      .then((detail) => {
        applyImages.value = parseApplyImages(detail.applyImages)
      })
      .catch(() => {
        applyImages.value = []
      })
      .finally(() => {
        loadingImages.value = false
      })
  },
  { immediate: true },
)

/** 跳转前先关闭弹窗，并吞掉重复导航 rejection */
function go(path: string): void {
  visible.value = false
  void router.push(path).catch(() => {
    /* 忽略重复导航 / 导航被中止的 rejection */
  })
}

/** 智能跳转按钮 */
function onSmartGo(): void {
  if (smartAction.value) go(smartAction.value.path)
}

/** 查看工单详情：已在详情页时改触发全局刷新，避免重复导航 */
function onViewOrder(): void {
  const orderId = props.notification?.orderId
  if (orderId == null) return
  const target = `/orders/${orderId}`
  visible.value = false
  if (route.path === target) {
    appStore.triggerOrderDetailRefresh()
    return
  }
  void router.push(target).catch(() => {
    /* 忽略重复导航 / 导航被中止的 rejection */
  })
}
</script>

<template>
  <el-dialog
    v-if="notification"
    v-model="visible"
    :title="notification.title"
    width="520px"
    append-to-body
    align-center
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="notif-detail-body">
      <p class="notif-detail-content">{{ notification.content }}</p>
      <time class="notif-detail-time">发送时间：{{ displayTime }}</time>

      <a v-if="hasOrderId" class="notif-detail-link" href="javascript:;" @click="onViewOrder">
        查看工单详情
      </a>

      <!-- 证据图（申请改派 / 申请仲裁时师傅上传） -->
      <div v-if="loadingImages" class="notif-images loading">证据图加载中…</div>
      <div v-else-if="applyImages.length" class="notif-images">
        <el-image
          v-for="(img, idx) in applyImages"
          :key="img"
          class="notif-image-thumb"
          :src="img"
          :preview-src-list="applyImages"
          :initial-index="idx"
          fit="cover"
          preview-teleported
        />
      </div>
    </div>

    <template #footer>
      <el-button v-if="smartAction" type="primary" @click="onSmartGo">
        {{ smartAction.label }}
      </el-button>
      <el-button v-else type="primary" @click="visible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.notif-detail-body {
  padding: 0 2px;
}
.notif-detail-content {
  font-size: 13.5px;
  color: var(--ink-2);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.notif-detail-time {
  display: block;
  font-size: 12px;
  color: #8ba0b5;
  margin-top: 14px;
  font-variant-numeric: tabular-nums;
}
.notif-detail-link {
  display: inline-block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--blue-600);
  text-decoration: none;
  margin-top: 12px;
}
.notif-detail-link:hover {
  color: var(--blue-700);
  text-decoration: underline;
}

/* 证据图 */
.notif-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}
.notif-images.loading {
  font-size: 12px;
  color: #8ba0b5;
}
.notif-image-thumb {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--line);
  cursor: pointer;
  flex: none;
}
.notif-image-thumb :deep(.el-image__inner) {
  border-radius: 8px;
}
</style>
