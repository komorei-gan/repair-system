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
import IconSvg from '@/components/IconSvg.vue'
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

/* ---------- 正文分段与重点高亮 ---------- */

/** 需要高亮的核心业务关键词（命中后加粗 + 主色） */
const HIGHLIGHT_WORDS = [
  '申请改派',
  '申请仲裁',
  '密码重置',
  '师傅注册',
  '改派申请',
  '重新指派',
  '已通过',
  '未通过',
  '已驳回',
  '已取消',
  '已接单',
  '维修完成',
  '催单',
  '信誉分',
]

/** 一段文本拆成若干片段：命中关键词的片段标记为高亮 */
interface Segment {
  text: string
  highlight: boolean
}

/**
 * 将文本按「关键词 / 《标题》 / 【账号】」切分为片段，供模板渲染 <strong> 高亮。
 * 采用单次正则匹配（关键词与《…》、【…】合并），保证片段顺序与原文一致、无重叠错乱。
 */
function splitSegments(text: string): Segment[] {
  if (!text) return []
  const escaped = HIGHLIGHT_WORDS.map((w) => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
  // 《…》为工单标题、【…】为账号名，两者都作为高亮单元
  const pattern = new RegExp(`(《[^》]{1,80}》|【[^】]{1,30}】|${escaped.join('|')})`, 'g')

  const segments: Segment[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ text: text.slice(lastIndex, match.index), highlight: false })
    }
    segments.push({ text: match[0], highlight: true })
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < text.length) {
    segments.push({ text: text.slice(lastIndex), highlight: false })
  }
  return segments
}

/* ---------- 「原因：」独立成行 + 中括号账号高亮 ---------- */

/** 正文中「原因：」及其后内容的独立展示（浅灰底 + 左侧边框，层级分明） */
const reasonText = computed(() => {
  const text = matchText.value
  const idx = text.indexOf('原因：')
  if (idx === -1) return ''
  return text.slice(idx).trim()
})

/** 去除「原因：」段后的正文段落（避免主文与原因块重复展示） */
const mainParagraphs = computed<Segment[][]>(() => {
  const raw = props.notification?.content ?? ''
  if (!raw.trim()) return []
  // 定位首个「原因：」，其之前的部分作为主文，之后的部分交给原因块单独展示
  const idx = raw.indexOf('原因：')
  const main = idx === -1 ? raw : raw.slice(0, idx)
  if (!main.trim()) return []
  return main
    .split(/(?<=[。；\n])/)
    .map((p) => p.trim())
    .filter(Boolean)
    .map((p) => splitSegments(p))
})

/**
 * 将【zhangsan】这类中括号账号渲染为蓝色加粗高亮 HTML。
 * 注意：v-html 渲染的字符串由本函数拼接，账号部分仅来自通知原文，
 * 我们对特殊字符做转义，避免 XSS 注入。
 */
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** 中括号账号高亮后的原因块 HTML（【xxx】→ 蓝色加粗 span） */
const reasonHtml = computed(() => {
  const text = reasonText.value
  if (!text) return ''
  return escapeHtml(text).replace(
    /【[^】\s]{1,30}】/g,
    (m) => `<span class="account-hl">${m}</span>`,
  )
})

/** 工单单号（若正文中带有「单号：RO…」则提取展示） */
const orderNo = computed(() => {
  const text = matchText.value
  const m = text.match(/单号[:：]\s*([A-Za-z0-9]+)/)
  return m ? m[1] : ''
})

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
    width="550px"
    class="notif-detail-dialog"
    append-to-body
    align-center
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="notif-detail-body">
      <!-- 正文：按句分段，核心词 / 《标题》高亮 -->
      <div class="notif-detail-content">
        <p v-for="(segments, i) in mainParagraphs" :key="i" class="notif-para">
          <template v-for="(seg, j) in segments" :key="j">
            <strong v-if="seg.highlight" class="notif-hl">{{ seg.text }}</strong>
            <template v-else>{{ seg.text }}</template>
          </template>
        </p>
      </div>

      <!-- 原因块：若正文含「原因：」则独立成行，浅灰底 + 左侧边框，账号【xxx】蓝色高亮；
           证据图直接渲染在原因文字下方，保持图文连贯 -->
      <div v-if="reasonText" class="notif-reason">
        <!-- eslint-disable-next-line vue/no-v-html -->
        <span class="notif-reason-label">原因说明</span>
        <p class="notif-reason-text" v-html="reasonHtml"></p>
        <div v-if="loadingImages" class="notif-images loading">证据图加载中…</div>
        <div v-else-if="applyImages.length" class="notif-images notif-images--inline">
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

      <!-- 元信息：时间 / 单号，弱化为辅助信息放正文下方 -->
      <div class="notif-detail-meta">
        <span class="meta-item">
          <IconSvg name="i-clock" :size="13" />
          {{ displayTime }}
        </span>
        <span v-if="orderNo" class="meta-item">
          <IconSvg name="i-clipboard" :size="13" />
          单号：{{ orderNo }}
        </span>
      </div>

      <!-- 证据图：无「原因说明」时，退化为底部展示（普通带图通知） -->
      <template v-if="!reasonText">
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
      </template>
    </div>

    <template #footer>
      <div class="notif-detail-footer">
        <a v-if="hasOrderId" class="notif-detail-link" href="javascript:;" @click="onViewOrder">
          查看工单详情
          <IconSvg name="i-chevron-right" :size="13" />
        </a>
        <span v-else class="footer-spacer"></span>

        <el-button v-if="smartAction" type="primary" @click="onSmartGo">
          {{ smartAction.label }}
        </el-button>
        <el-button v-else type="primary" @click="visible = false">我知道了</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
/*
 * 弹窗通过 append-to-body 渲染到 body，scoped 样式无法命中弹窗外壳，
 * 因此统一用 :global 前缀，并以 .notif-detail-dialog 作为命名空间，避免污染全局。
 */
:global(.notif-detail-dialog.el-dialog) {
  padding: 0;
  border: 0;
  border-radius: 16px;
  box-shadow: 0 24px 64px -20px rgba(10, 42, 80, 0.32);
  overflow: hidden;
}
/* 头部：紧凑留白 + 深色标题（需 !important 覆盖 Element Plus 默认标题色） */
:global(.notif-detail-dialog .el-dialog__header) {
  padding: 16px 24px;
  margin: 0;
  border-bottom: 1px solid #f0f4f9;
}
:global(.notif-detail-dialog .el-dialog__title),
:global(.notif-detail-dialog .el-dialog__header .el-dialog__title) {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #303133 !important;
  letter-spacing: 0;
  line-height: 1.5;
}
/* 关闭按钮：不覆写任何定位/边距，沿用 Element Plus 默认右上角位置 */
/* 主体：紧凑内边距 */
:global(.notif-detail-dialog .el-dialog__body) {
  padding: 16px 24px;
}
/* 底部：左右分栏（左链接 / 右主按钮），去掉默认上边框 */
:global(.notif-detail-dialog .el-dialog__footer) {
  padding: 0 24px 16px;
  border-top: 0;
}

.notif-detail-body {
  padding: 0;
}

/* ===== 正文：分段落 + 关键词高亮 ===== */
.notif-detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.notif-para {
  margin: 0;
  font-size: 14px;
  line-height: 1.85;
  color: #3e5872;
  word-break: break-word;
  white-space: pre-wrap;
}
.notif-hl {
  font-weight: 700;
  color: var(--blue-700);
  background: linear-gradient(180deg, transparent 62%, rgba(24, 144, 255, 0.14) 62%);
  padding: 0 1px;
}

/* ===== 原因块：浅灰底 + 左侧边框，层级分明 ===== */
.notif-reason {
  margin-top: 16px;
  padding: 12px 14px;
  background: #f7f9fc;
  border-left: 3px solid var(--blue-500);
  border-radius: 8px;
}
.notif-reason-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  letter-spacing: 0.04em;
}
.notif-reason-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: #4c647c;
  word-break: break-word;
  white-space: pre-wrap;
}
.notif-reason-text :deep(.account-hl) {
  color: #1890ff;
  font-weight: 600;
}
html.dark .notif-reason {
  background: #23272e;
  border-left-color: #4096e0;
}
html.dark .notif-reason-text {
  color: #c2cfdd;
}

/* ===== 元信息：弱化的辅助信息 ===== */
.notif-detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px dashed #eaf0f7;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #909399;
  font-variant-numeric: tabular-nums;
}
.meta-item :deep(.ico) {
  color: #b8c6d4;
  flex: none;
}

/* ===== 底部操作区 ===== */
.notif-detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.footer-spacer {
  flex: 1;
}
.notif-detail-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 600;
  color: var(--blue-600);
  text-decoration: none;
  transition: color 0.16s;
}
.notif-detail-link:hover {
  color: var(--blue-700);
}
.notif-detail-link :deep(.ico) {
  transition: transform 0.16s;
}
.notif-detail-link:hover :deep(.ico) {
  transform: translateX(2px);
}
.notif-detail-footer :deep(.el-button) {
  border-radius: 9px;
  padding: 0 18px;
  font-weight: 600;
}

/* ===== 证据图 ===== */
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
/* 内嵌在「原因说明」框内的证据图：顶部留小间距，紧贴原因文字 */
.notif-images--inline {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #e3eaf2;
}
html.dark .notif-images--inline {
  border-top-color: #36424f;
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

/* 深色模式适配 */
html.dark :global(.notif-detail-dialog .el-dialog__header) {
  border-bottom-color: #2a343f;
}
/* 深色底需覆盖浅色标题，保留与 .el-dialog__title 相同的 !important 优先级 */
html.dark :global(.notif-detail-dialog .el-dialog__title) {
  color: #e5eaf3 !important;
}
html.dark .notif-para {
  color: #c2cfdd;
}
html.dark .notif-detail-meta {
  border-top-color: #2a343f;
}
html.dark .meta-item {
  color: #7a90a8;
}

/* 移动端：窄屏改为接近全宽，内边距与桌面版保持同一节奏 */
@media (max-width: 780px) {
  :global(.notif-detail-dialog.el-dialog) {
    width: calc(100vw - 32px) !important;
  }
  :global(.notif-detail-dialog .el-dialog__header) {
    padding: 16px 18px;
  }
  :global(.notif-detail-dialog .el-dialog__body) {
    padding: 16px 18px;
  }
  :global(.notif-detail-dialog .el-dialog__footer) {
    padding: 0 18px 16px;
  }
}
</style>
