<script setup lang="ts">
/**
 * 工单详情 —— 展示工单完整信息与操作日志（design.md §3.2 GET /api/orders/{id}）。
 * 管理员从工单列表「详情」按钮进入。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as orderApi from '@/api/order'
import { STATUS_COLORS, orderStatusLabel } from '@/utils/status'
import type { OrderDetailVO, OrderStatus } from '@/types/api'
import IconSvg from '@/components/IconSvg.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<OrderDetailVO | null>(null)
/** 加载失败原因（工单被删除 / 无权限 / 网络异常），为空表示尚未失败 */
const errorText = ref('')

const orderId = computed(() => Number(route.params.id))

async function load(): Promise<void> {
  errorText.value = ''
  if (!Number.isFinite(orderId.value)) {
    errorText.value = '工单编号无效'
    return
  }
  loading.value = true
  try {
    detail.value = await orderApi.orderDetail(orderId.value)
    if (!detail.value) {
      // 后端返回空数据（异常兜底）同样按不存在处理
      errorText.value = '该工单已被删除或不存在'
    }
  } catch {
    // 接口非 200：后端对不存在/无权限统一返回 ORDER_NOT_FOUND
    detail.value = null
    errorText.value = '该工单已被删除或不存在'
    ElMessage.error(errorText.value)
    // 延迟退回列表页，让用户先看到提示（1.2s 后再跳，避免提示瞬间被页面切换打断）
    window.setTimeout(() => goBack(), 1200)
  } finally {
    loading.value = false
  }
}

function goBack(): void {
  // 通用化返回：优先回「来源页」（from 参数），缺省回工单列表。
  const from = route.query.from
  if (typeof from === 'string' && from) {
    void router.push(from)
    return
  }
  // 无来源时回列表页，并恢复之前的 Tab 状态（从 query.status 读取；无则回「全部」）
  const status = route.query.status
  const query = typeof status === 'string' && status ? { status } : {}
  void router.push({ name: 'orders', query })
}

/** 状态胶囊配色（与列表页一致） */
function statusStyle(status: OrderStatus): Record<string, string> {
  const color = STATUS_COLORS[status] ?? '#1890FF'
  return { color, backgroundColor: `${color}1A`, borderColor: `${color}40` }
}

/** 日志动作映射为中文 */
function actionLabel(action: string): string {
  const map: Record<string, string> = {
    SUBMIT: '提交报修',
    ASSIGN: '派单',
    ACCEPT: '接单',
    REJECT: '驳回/拒单',
    FINISH: '维修完成',
    CONFIRM: '确认评价',
    CANCEL: '取消',
    REMIND: '催单',
  }
  return map[action] ?? action
}

onMounted(load)
</script>

<template>
  <div class="order-detail">
    <div class="page-head">
      <button type="button" class="back-btn" @click="goBack">
        <IconSvg name="i-chevron-left" :size="15" />
        返回列表
      </button>
      <h1>工单详情</h1>
    </div>

    <div v-loading="loading" class="detail-body">
      <template v-if="detail">
        <!-- 基本信息 -->
        <section class="card info-card rise">
          <div class="card-title">
            <h2>{{ detail.title }}</h2>
            <el-tag :style="statusStyle(detail.status)" size="small" effect="plain">
              {{ orderStatusLabel(detail.status) }}
            </el-tag>
          </div>
          <div class="info-grid">
            <div class="info-item"><span>工单号</span><b>{{ detail.orderNo }}</b></div>
            <div class="info-item"><span>分类</span><b>{{ detail.categoryName || '-' }}</b></div>
            <div class="info-item"><span>报修人</span><b>{{ detail.reporterName || '-' }}</b></div>
            <div class="info-item"><span>联系电话</span><b>{{ detail.contactPhone || '-' }}</b></div>
            <div class="info-item"><span>维修师傅</span><b>{{ detail.repairerName || '-' }}</b></div>
            <div class="info-item"><span>报修地址</span><b>{{ detail.address || '-' }}</b></div>
            <div class="info-item"><span>创建时间</span><b>{{ detail.createdAt }}</b></div>
            <div class="info-item"><span>评分</span><b>{{ detail.rating ?? '-' }}</b></div>
          </div>
          <div v-if="detail.description" class="info-desc">
            <span>故障描述</span>
            <p>{{ detail.description }}</p>
          </div>
          <div v-if="detail.repairResult" class="info-desc">
            <span>维修结果</span>
            <p>{{ detail.repairResult }}</p>
          </div>
        </section>

        <!-- 操作日志 -->
        <section class="card logs-card rise" style="animation-delay: 0.06s">
          <h2>操作日志</h2>
          <el-timeline v-if="detail.logs.length > 0">
            <el-timeline-item
              v-for="log in detail.logs"
              :key="log.id"
              :timestamp="log.createdAt"
              placement="top"
            >
              <div class="log-line">
                <b>{{ actionLabel(log.action) }}</b>
                <span v-if="log.operatorName" class="log-operator">{{ log.operatorName }}</span>
              </div>
              <p v-if="log.remark" class="log-remark">{{ log.remark }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作日志" />
        </section>
      </template>

      <div v-else-if="!loading" class="not-found">
        <el-empty :description="errorText || '该工单已被删除或不存在'">
          <el-button type="primary" @click="goBack">返回列表</el-button>
        </el-empty>
        <p v-if="errorText" class="not-found-tip">即将自动返回工单列表…</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-detail {
  min-width: 0;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--line-strong);
  border-radius: 9px;
  background: #fff;
  color: #4c647c;
  font-size: 12.5px;
  cursor: pointer;
  transition: border-color 0.18s, color 0.18s;
}
.back-btn:hover {
  border-color: var(--blue-200);
  color: var(--blue-700);
}
.page-head h1 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--ink);
}

.detail-body {
  min-height: 320px;
}
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 40px 0;
}
.not-found-tip {
  font-size: 12px;
  color: var(--muted-2);
}
.info-card,
.logs-card {
  padding: 22px;
  margin-bottom: 18px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.card-title h2 {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.015em;
  color: var(--ink-2);
  flex: 1;
  min-width: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px 22px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.info-item span {
  font-size: 11.5px;
  color: var(--muted-2);
}
.info-item b {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-2);
  word-break: break-all;
}
.info-desc {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f0f6fc;
}
.info-desc span {
  font-size: 11.5px;
  color: var(--muted-2);
}
.info-desc p {
  margin-top: 6px;
  font-size: 13px;
  color: #3e5872;
  line-height: 1.7;
  white-space: pre-wrap;
}

.logs-card h2 {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 18px;
  color: var(--ink-2);
}
.log-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.log-line b {
  font-size: 13px;
  color: var(--ink-2);
}
.log-operator {
  font-size: 11.5px;
  color: var(--blue-600);
  background: var(--blue-50);
  padding: 1px 7px;
  border-radius: 5px;
}
.log-remark {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .info-card,
  .logs-card {
    padding: 16px;
  }
}
</style>
