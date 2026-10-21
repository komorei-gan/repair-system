<script setup lang="ts">
/**
 * 登录页 —— 视觉语言沿用 design-web.html（科技蓝 + 渐变品牌区）。
 * 对接真实接口 POST /api/auth/login，成功存 token 后跳转 redirect 或 /dashboard。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import IconSvg from '@/components/IconSvg.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const features = [
  { icon: 'i-send', text: '一键派单，服务高效触达' },
  { icon: 'i-bell', text: '状态变更，消息实时推送' },
  { icon: 'i-clock', text: '全程跟踪，进度透明可查' },
]
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <section class="brand-panel">
      <div class="brand-inner">
        <div class="brand-row">
          <div class="brand-mark" aria-hidden="true">
            <IconSvg name="i-wrench" :size="22" />
          </div>
          <div class="brand-text">
            <b>校园易修</b>
            <span>CAMPUS FIX</span>
          </div>
        </div>

        <h1>校园报修，一站搞定</h1>
        <p class="slogan">
          报修提交、智能派单、维修跟踪、服务评价全流程线上化，<br />
          让每一张工单都高效闭环。
        </p>

        <ul class="features">
          <li v-for="f in features" :key="f.text">
            <span class="f-icon"><IconSvg :name="f.icon" :size="15" /></span>
            {{ f.text }}
          </li>
        </ul>
      </div>
      <div class="brand-note">© 2026 校园易修 · 后勤保障中心</div>
    </section>

    <!-- 右侧表单区 -->
    <section class="form-panel">
      <div class="form-card">
        <h2>欢迎回来</h2>
        <p class="sub">请使用管理员账号登录工单管理系统</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="onSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="undefined"
              autocomplete="username"
            >
              <template #prefix>
                <IconSvg name="i-user" :size="15" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
            >
              <template #prefix>
                <IconSvg name="i-sliders" :size="15" />
              </template>
            </el-input>
          </el-form-item>

          <button type="button" class="submit-btn" :disabled="loading" @click="onSubmit">
            <span v-if="loading" class="spinner" aria-hidden="true"></span>
            {{ loading ? '登录中…' : '登 录' }}
          </button>
        </el-form>

        <div class="hint">
          演示账号 <code>admin</code> / <code>admin123</code>（需先启动后端服务）
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  background: var(--bg);
}

/* ===== 左侧品牌区 ===== */
.brand-panel {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  color: #fff;
  background: linear-gradient(150deg, #35a0ff 0%, #1890ff 48%, #0b6fd0 100%);
}
.brand-panel::before {
  content: '';
  position: absolute;
  right: -180px;
  top: -180px;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 0 0 70px rgba(255, 255, 255, 0.05), 0 0 0 140px rgba(255, 255, 255, 0.03);
}
.brand-panel::after {
  content: '';
  position: absolute;
  left: -120px;
  bottom: -140px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 480px;
}
.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 64px;
}
.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.brand-text b {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.brand-text span {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.22em;
  opacity: 0.66;
  margin-top: 4px;
}
h1 {
  font-size: clamp(30px, 3.4vw, 44px);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin-bottom: 18px;
}
.slogan {
  font-size: 14.5px;
  line-height: 1.9;
  opacity: 0.86;
  margin-bottom: 44px;
}
.features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.features li {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 13.5px;
  opacity: 0.92;
}
.f-icon {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}
.brand-note {
  position: absolute;
  left: 60px;
  bottom: 32px;
  font-size: 11.5px;
  opacity: 0.62;
}

/* ===== 右侧表单区 ===== */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
}
.form-card {
  width: 100%;
  max-width: 400px;
}
.form-card h2 {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}
.form-card .sub {
  font-size: 13px;
  color: var(--muted);
  margin: 8px 0 30px;
}

/* Element Plus 表单项微调 */
.form-card :deep(.el-form-item) {
  margin-bottom: 22px;
}
.form-card :deep(.el-form-item__label) {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-2);
  padding-bottom: 7px;
}
.form-card :deep(.el-input__wrapper) {
  border-radius: 11px;
  padding: 2px 14px;
  box-shadow: 0 0 0 1px var(--line-strong) inset;
  background: #fafdff;
  transition: box-shadow 0.18s, background 0.18s;
}
.form-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--blue-200) inset;
}
.form-card :deep(.el-input__inner) {
  height: 42px;
  font-size: 13.5px;
}
.form-card :deep(.el-input__prefix) {
  color: #8fa3b8;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: 0;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #fff;
  background: linear-gradient(180deg, #35a0ff 0%, #1890ff 55%, #0f7ee4 100%);
  box-shadow: 0 12px 22px -10px rgba(24, 144, 255, 0.95), inset 0 1px 0 rgba(255, 255, 255, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  transition: transform 0.18s var(--ease), box-shadow 0.18s, opacity 0.18s;
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px -10px rgba(24, 144, 255, 0.9), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.spinner {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.hint {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px dashed var(--line);
  text-align: center;
  font-size: 12px;
  color: var(--muted-2);
}
.hint code {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  color: var(--blue-700);
  background: var(--blue-50);
  border-radius: 5px;
  padding: 1px 6px;
  font-size: 11.5px;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .brand-panel {
    display: none;
  }
  .form-panel {
    padding: 32px 22px;
    align-items: flex-start;
    padding-top: 12vh;
  }
}
</style>
