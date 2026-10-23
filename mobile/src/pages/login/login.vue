<script setup lang="ts">
/**
 * 登录页：对接 POST /api/auth/login。
 * 仅允许报修人（REPORTER）账号登录；其余角色拦截并提示。
 */
import { ref } from 'vue'
import { useToast } from '@wot-ui/ui'
import { login } from '@/api/auth'
import { setCachedUser, setToken, clearToken } from '@/utils/token'

const toast = useToast()

const username = ref('')
const password = ref('')
const loading = ref(false)

const inputStyle = 'background:#f5f6f8;border-radius:16rpx;padding:24rpx 28rpx;margin-top:24rpx;'

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
    // 仅报修人账号可进入移动端
    if (res.user.role !== 'REPORTER') {
      clearToken()
      toast.error('请使用报修人账号登录')
      return
    }
    setToken(res.token)
    setCachedUser(res.user)
    toast.success('登录成功')
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 300)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
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
    <view class="login-header">
      <text class="login-title">报修工单</text>
      <text class="login-subtitle">社区/校园报修移动端</text>
    </view>

    <view class="login-form">
      <wd-input
        v-model="username"
        placeholder="请输入用户名"
        prefix-icon="user"
        clearable
        :custom-style="inputStyle"
      />
      <wd-input
        v-model="password"
        placeholder="请输入密码"
        prefix-icon="lock"
        show-password
        clearable
        :custom-style="inputStyle"
        @confirm="handleLogin"
      />
      <wd-button
        type="primary"
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
  padding: 120rpx 48rpx 0;
  box-sizing: border-box;
  background: #f7f8fa;
}

.login-header {
  margin-bottom: 80rpx;
}

.login-title {
  display: block;
  font-size: 56rpx;
  font-weight: 700;
  color: #1f2d3d;
}

.login-subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #8a94a6;
}

.login-form {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx 32rpx 48rpx;
}

.login-btn {
  margin-top: 48rpx;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 40rpx;
}

.login-footer__text {
  font-size: 26rpx;
  color: #8a94a6;
}

.login-footer__link {
  font-size: 26rpx;
  color: #1890ff;
  font-weight: 500;
}
</style>
