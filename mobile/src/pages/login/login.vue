<script setup lang="ts">
/**
 * 登录页：对接 POST /api/auth/login。
 * - 报修人（REPORTER）正常进入报修首页；
 * - 维修师傅（REPAIRER）已通过审核：本期提示「师傅端建设中」；
 * - 待审核 / 已拒绝的师傅：由后端返回 403 + 明确文案，request 层统一 toast 提示。
 */
import { ref } from 'vue'
import { useToast } from '@wot-ui/ui'
import { login } from '@/api/auth'
import { setCachedUser, setToken, clearToken } from '@/utils/token'

const toast = useToast()

const username = ref('')
const password = ref('')
const loading = ref(false)
/** 密码是否明文显示（由我们自己的眼睛图标控制） */
const showPwd = ref(false)

function togglePwd() {
  showPwd.value = !showPwd.value
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
    </view>

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
</style>
