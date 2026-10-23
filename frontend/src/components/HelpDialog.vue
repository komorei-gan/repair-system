<script setup lang="ts">
/**
 * 全局帮助中心 —— el-dialog 弹窗。
 * 内容：快速上手（工单提交流程）+ 常见问题 FAQ。
 * 入口：顶栏右上角「?」图标、系统设置「帮助中心」。
 */
const visible = defineModel<boolean>({ default: false })

const faqs = [
  {
    q: '忘记密码怎么办？',
    a: '请在移动端登录页点击「忘记密码」发起申请，管理员在【用户管理】批准后，即可输入新密码完成重置。',
  },
  {
    q: '师傅账号如何登录？',
    a: '师傅注册后需等待管理员在【师傅审核】中通过审核，通过后方可登录。',
  },
  {
    q: '为什么有些工单无法删除？',
    a: '仅「待受理」或「已取消」状态的工单允许删除，其余流转中的工单需先取消或驳回。',
  },
]
</script>

<template>
  <el-dialog
    v-model="visible"
    title="帮助中心"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="help-body">
      <!-- 快速上手 -->
      <section class="help-section">
        <h3 class="help-title">快速上手</h3>
        <p class="help-text">
          报修人在移动端提交报修工单后，管理员在【工单列表】或【派单中心】将工单派给维修师傅，
          师傅接单并完成维修后，报修人确认评价，一条报修流程即完成。
        </p>
      </section>

      <!-- 常见问题 -->
      <section class="help-section">
        <h3 class="help-title">常见问题</h3>
        <div v-for="faq in faqs" :key="faq.q" class="faq-item">
          <p class="faq-q">Q：{{ faq.q }}</p>
          <p class="faq-a">A：{{ faq.a }}</p>
        </div>
      </section>
    </div>

    <template #footer>
      <el-button type="primary" @click="visible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.help-body {
  max-height: 60vh;
  overflow-y: auto;
  padding: 0 2px;
}
.help-section {
  margin-bottom: 20px;
}
.help-section:last-child {
  margin-bottom: 0;
}
.help-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink-2);
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid var(--blue-500);
  line-height: 1.3;
}
.help-text {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.7;
}
.faq-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7fbff;
  margin-bottom: 10px;
}
.faq-item:last-child {
  margin-bottom: 0;
}
html.dark .faq-item {
  background: #20252c;
}
.faq-q {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
  line-height: 1.6;
}
.faq-a {
  font-size: 12.5px;
  color: var(--muted);
  line-height: 1.7;
  margin-top: 4px;
}
</style>
