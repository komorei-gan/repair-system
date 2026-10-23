<script setup lang="ts">
/**
 * 登录页：对接 POST /api/auth/login。
 * - 报修人（REPORTER）正常进入报修首页；
 * - 维修师傅（REPAIRER）已通过审核：本期提示「师傅端建设中」；
 * - 待审核 / 已拒绝的师傅：由后端返回 403 + 明确文案，request 层统一 toast 提示。
 */
import { ref } from 'vue'
import { useToast } from '@wot-ui/ui'
import { login, forgotPassword, resetPassword } from '@/api/auth'
import { setCachedUser, setToken, clearToken } from '@/utils/token'

const toast = useToast()

const username = ref('')
const password = ref('')
const loading = ref(false)
/** 密码是否明文显示（由我们自己的眼睛图标控制） */
const showPwd = ref(false)

/* ---------- 忘记密码弹窗 ---------- */
const forgotVisible = ref(false)
const forgotUsername = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const forgotLoading = ref(false)
const resetSubmitting = ref(false)
/** 新密码是否明文显示 */
const showNewPwd = ref(false)
/** 确认新密码是否明文显示 */
const showConfirmPwd = ref(false)

function togglePwd() {
  showPwd.value = !showPwd.value
}

function toggleNewPwd() {
  showNewPwd.value = !showNewPwd.value
}

function toggleConfirmPwd() {
  showConfirmPwd.value = !showConfirmPwd.value
}

function clearPassword() {
  password.value = ''
}

const inputStyle =
  'background:transparent;border-radius:20rpx;padding:28rpx 24rpx;color:#1f2d3d;'

async function handleLogin() {
  if (!username.value.trim()) {
    toast.error('请输入用户名')
    return
  }
  if (!password.value) {
    toast.error('请输入密码')
    return
  }

  loading.value = true
  try {
    const res = await login(username.value.trim(), password.value)
    // 已通过审核的师傅：本期师傅端尚未建设，仅提示、不进入报修人首页
    if (res.user.role === 'REPAIRER') {
      clearToken()
      toast.show('师傅端建设中，敬请期待')
      return
    }
    setToken(res.token)
    setCachedUser(res.user)
    toast.success('登录成功')
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 300)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理（含待审核/已拒绝的审核拦截文案）
  } finally {
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/login/register' })
}

/* ---------- 忘记密码 ---------- */
function openForgot() {
  forgotVisible.value = true
  // 预填当前已输入的账号，减少重复输入
  if (username.value.trim() && !forgotUsername.value) {
    forgotUsername.value = username.value.trim()
  }
}

/** 申请重置：通知管理员 */
async function submitForgot() {
  if (!forgotUsername.value.trim()) {
    toast.error('请输入账号')
    return
  }
  forgotLoading.value = true
  try {
    await forgotPassword(forgotUsername.value.trim())
    toast.success('申请已提交，请等待管理员批准')
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    forgotLoading.value = false
  }
}

/** 提交新密码：自助重置 */
async function submitReset() {
  if (!forgotUsername.value.trim()) {
    toast.error('请输入账号')
    return
  }
  if (!newPassword.value) {
    toast.error('请输入新密码')
    return
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 20) {
    toast.error('密码长度须为 6-20 位')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast.error('两次输入的密码不一致')
    return
  }

  resetSubmitting.value = true
  try {
    await resetPassword(forgotUsername.value.trim(), newPassword.value)
    toast.success('密码重置成功，请登录')
    // 关闭弹窗并清空表单，回填账号到登录框
    forgotVisible.value = false
    username.value = forgotUsername.value.trim()
    password.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    forgotUsername.value = ''
  } catch {
    // 未批准等错误提示已由 request 层统一 showToast 处理（如「您的申请尚未被管理员批准」）
  } finally {
    resetSubmitting.value = false
  }
}
</script>

<template>
  <view class="login-page">
    <!-- 顶部渐变品牌区 -->
    <view class="brand">
      <image class="brand-logo" src="/static/logo.png" mode="aspectFit" />
      <text class="brand-title">报修工单</text>
      <text class="brand-subtitle">社区 / 校园报修移动端</text>
    </view>

    <!-- 悬浮卡片表单 -->
    <view class="login-card">
      <view class="card-title">欢迎登录</view>
      <view class="card-subtitle">请使用您的账号登录</view>

      <view class="field">
        <wd-input
          v-model="username"
          placeholder="请输入用户名"
          prefix-icon="user"
          clearable
          :custom-style="inputStyle"
        />
      </view>
      <view class="field">
        <view class="pwd-field">
          <wd-icon name="lock" size="18px" custom-class="pwd-field__prefix" />
          <!-- #ifdef H5 -->
          <!-- H5：始终 text + text-security 遮蔽，浏览器不会出现自带的密码眼睛 -->
          <input
            v-model="password"
            class="pwd-field__inner"
            :class="{ 'is-masked': !showPwd }"
            type="text"
            placeholder="请输入密码"
            placeholder-style="color:#b0b8c4;font-size:28rpx;"
            :maxlength="20"
            @confirm="handleLogin"
          />
          <!-- #endif -->
          <!-- #ifndef H5 -->
          <input
            v-model="password"
            class="pwd-field__inner"
            :password="!showPwd"
            placeholder="请输入密码"
            placeholder-style="color:#b0b8c4;font-size:28rpx;"
            :maxlength="20"
            @confirm="handleLogin"
          />
          <!-- #endif -->
          <wd-icon
            v-if="password"
            name="close-circle"
            size="18px"
            custom-class="pwd-field__suffix"
            @click="clearPassword"
          />
          <wd-icon
            :name="showPwd ? 'eye' : 'eye-invisible'"
            size="18px"
            custom-class="pwd-field__suffix"
            @click="togglePwd"
          />
        </view>
      </view>

      <wd-button
        block
        :loading="loading"
        custom-class="login-btn"
        @click="handleLogin"
      >
        登 录
      </wd-button>
    </view>

    <view class="login-footer">
      <text class="login-footer__text">还没有账号？</text>
      <text class="login-footer__link" @click="goRegister">去注册</text>
      <text class="login-footer__divider">|</text>
      <text class="login-footer__link" @click="openForgot">忘记密码？</text>
    </view>

    <!-- 忘记密码弹窗 -->
    <wd-popup
      v-model="forgotVisible"
      position="bottom"
      round
      safe-area-inset-bottom
      custom-class="forgot-panel"
      @click-modal="forgotVisible = false"
    >
      <view class="forgot-panel__title">忘记密码</view>
      <view class="forgot-panel__body">
        <view class="field">
          <wd-input
            v-model="forgotUsername"
            placeholder="请输入账号"
            prefix-icon="user"
            clearable
            :custom-style="inputStyle"
          />
        </view>
        <view class="field">
          <view class="pwd-field">
            <wd-icon name="lock" size="18px" custom-class="pwd-field__prefix" />
            <!-- #ifdef H5 -->
            <input
              v-model="newPassword"
              class="pwd-field__inner"
              :class="{ 'is-masked': !showNewPwd }"
              type="text"
              placeholder="请输入新密码（6-20 位）"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="20"
            />
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <input
              v-model="newPassword"
              class="pwd-field__inner"
              :password="!showNewPwd"
              placeholder="请输入新密码（6-20 位）"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="20"
            />
            <!-- #endif -->
            <wd-icon
              v-if="newPassword"
              name="close-circle"
              size="18px"
              custom-class="pwd-field__suffix"
              @click="newPassword = ''"
            />
            <wd-icon
              :name="showNewPwd ? 'eye' : 'eye-invisible'"
              size="18px"
              custom-class="pwd-field__suffix"
              @click="toggleNewPwd"
            />
          </view>
        </view>
        <view class="field">
          <view class="pwd-field">
            <wd-icon name="lock" size="18px" custom-class="pwd-field__prefix" />
            <!-- #ifdef H5 -->
            <input
              v-model="confirmPassword"
              class="pwd-field__inner"
              :class="{ 'is-masked': !showConfirmPwd }"
              type="text"
              placeholder="请再次输入新密码"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="20"
            />
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <input
              v-model="confirmPassword"
              class="pwd-field__inner"
              :password="!showConfirmPwd"
              placeholder="请再次输入新密码"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="20"
            />
            <!-- #endif -->
            <wd-icon
              v-if="confirmPassword"
              name="close-circle"
              size="18px"
              custom-class="pwd-field__suffix"
              @click="confirmPassword = ''"
            />
            <wd-icon
              :name="showConfirmPwd ? 'eye' : 'eye-invisible'"
              size="18px"
              custom-class="pwd-field__suffix"
              @click="toggleConfirmPwd"
            />
          </view>
        </view>
        <text class="forgot-tip">
          首次使用请先点击「申请重置」，管理员批准后才能提交新密码。
        </text>
      </view>
      <view class="forgot-panel__footer">
        <wd-button
          block
          :loading="forgotLoading"
          custom-class="forgot-panel__apply"
          @click="submitForgot"
        >
          申请重置
        </wd-button>
        <wd-button
          block
          type="primary"
          :loading="resetSubmitting"
          custom-class="forgot-panel__confirm"
          @click="submitReset"
        >
          提交新密码
        </wd-button>
      </view>
    </wd-popup>

    <!-- 反馈类组件需显式声明（Wot UI 规范） -->
    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  box-sizing: border-box;
  background: #f4f7f9;
}

/* ===== 顶部渐变品牌区 ===== */
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 48rpx 160rpx;
  background: linear-gradient(160deg, #35a0ff 0%, #1890ff 48%, #0b6fd0 100%);
  border-radius: 0 0 48rpx 48rpx;
  position: relative;
  overflow: hidden;
}
.brand::after {
  content: '';
  position: absolute;
  right: -160rpx;
  top: -120rpx;
  width: 480rpx;
  height: 480rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 0 0 60rpx rgba(255, 255, 255, 0.05), 0 0 0 120rpx rgba(255, 255, 255, 0.03);
}
.brand-logo {
  width: 140rpx;
  height: 140rpx;
  margin-bottom: 24rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: 0 16rpx 32rpx -12rpx rgba(11, 111, 208, 0.6);
}
.brand-title {
  font-size: 52rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  color: #ffffff;
}
.brand-subtitle {
  margin-top: 14rpx;
  font-size: 26rpx;
  letter-spacing: 1rpx;
  color: rgba(255, 255, 255, 0.82);
}

/* ===== 悬浮卡片 ===== */
.login-card {
  margin: -96rpx 40rpx 0;
  padding: 56rpx 40rpx 48rpx;
  background: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 24rpx 60rpx -20rpx rgba(16, 62, 118, 0.24);
  position: relative;
  z-index: 1;
}
.card-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2d3d;
}
.card-subtitle {
  margin-top: 10rpx;
  margin-bottom: 40rpx;
  font-size: 26rpx;
  color: #8a94a6;
}

/* 输入框外层：悬浮阴影卡片态 */
.field {
  margin-bottom: 28rpx;
  background: #f5f7fa;
  border-radius: 20rpx;
  box-shadow: 0 6rpx 18rpx -6rpx rgba(31, 45, 61, 0.08);
  overflow: hidden;
}

/* ===== 自定义密码框（眼睛与清除按钮完全自控） ===== */
.pwd-field {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
}
.pwd-field__prefix {
  margin-right: 12rpx;
  color: #8a94a6;
}
.pwd-field__inner {
  flex: 1;
  min-width: 0;
  height: 44rpx;
  padding: 0;
  font-size: 28rpx;
  color: #1f2d3d;
  background: transparent;
  border: none;
  outline: none;
}
/* 兼容 uni-app H5：真实可编辑元素在 <uni-input> 包裹层内部 */
.pwd-field__inner :deep(input) {
  height: 44rpx;
  font-size: 28rpx;
  color: #1f2d3d;
}
/* H5：用字符遮蔽代替原生密码框，避免浏览器叠加自带的眼睛图标 */
.pwd-field__inner.is-masked,
.pwd-field__inner.is-masked :deep(input) {
  -webkit-text-security: disc;
}
.pwd-field__suffix {
  margin-left: 16rpx;
  color: #8a94a6;
}

/* ===== 渐变蓝大圆角按钮 ===== */
:deep(.login-btn) {
  margin-top: 24rpx;
  border-radius: 999rpx !important;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  box-shadow: 0 16rpx 32rpx -12rpx rgba(24, 144, 255, 0.65) !important;
  border: none !important;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 44rpx;
  padding-bottom: 60rpx;
}

.login-footer__text {
  font-size: 26rpx;
  color: #8a94a6;
}

.login-footer__link {
  font-size: 26rpx;
  color: #1890ff;
  font-weight: 600;
}

.login-footer__divider {
  margin: 0 12rpx;
  font-size: 26rpx;
  color: #c6ced8;
}

/* ===== 忘记密码弹窗 ===== */
.forgot-panel__title {
  padding: 40rpx 40rpx 8rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2d3d;
}
.forgot-panel__body {
  padding: 16rpx 40rpx 8rpx;
}
.forgot-panel__body .field {
  margin-bottom: 24rpx;
}
.forgot-tip {
  display: block;
  font-size: 24rpx;
  color: #8a94a6;
  line-height: 1.6;
  margin-bottom: 8rpx;
}
.forgot-panel__footer {
  display: flex;
  gap: 20rpx;
  padding: 8rpx 40rpx 48rpx;
}
:deep(.forgot-panel__apply) {
  border-radius: 999rpx !important;
  border: 1px solid #d6e4f5 !important;
  background: #f5f9ff !important;
  color: #1890ff !important;
}
:deep(.forgot-panel__confirm) {
  border-radius: 999rpx !important;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important;
}
</style>
