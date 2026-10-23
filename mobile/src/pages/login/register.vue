<script setup lang="ts">
/**
 * 注册页：对接 POST /api/auth/register。
 * 表单四项（账号/密码/真实姓名/手机号）均必填，成功后返回登录页。
 */
import { ref } from 'vue'
import { useToast } from '@wot-ui/ui'
import { register } from '@/api/auth'

const toast = useToast()

const username = ref('')
const password = ref('')
const realName = ref('')
const phone = ref('')
const loading = ref(false)

const inputStyle = 'background:#f5f6f8;border-radius:16rpx;padding:24rpx 28rpx;margin-top:24rpx;'

async function handleRegister() {
  const name = username.value.trim()
  if (!name) {
    toast.error('请输入用户名')
    return
  }
  if (name.length < 3 || name.length > 50) {
    toast.error('用户名长度须为 3-50 个字符')
    return
  }
  if (!password.value) {
    toast.error('请输入密码')
    return
  }
  if (password.value.length < 6 || password.value.length > 20) {
    toast.error('密码长度须为 6-20 位')
    return
  }
  if (!realName.value.trim()) {
    toast.error('请输入真实姓名')
    return
  }
  if (realName.value.trim().length > 50) {
    toast.error('姓名长度不能超过 50 个字符')
    return
  }
  if (!phone.value.trim()) {
    toast.error('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone.value.trim())) {
    toast.error('手机号格式不正确')
    return
  }

  loading.value = true
  try {
    await register({
      username: name,
      password: password.value,
      realName: realName.value.trim(),
      phone: phone.value.trim(),
    })
    toast.success('注册成功')
    setTimeout(() => {
      uni.navigateBack()
    }, 400)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="register-page">
    <wd-navbar
      title="注册"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      bordered
      @click-left="uni.navigateBack()"
    />

    <view class="register-form">
      <wd-input
        v-model="username"
        placeholder="请输入用户名（3-50 个字符）"
        prefix-icon="user"
        clearable
        :custom-style="inputStyle"
      />
      <wd-input
        v-model="password"
        placeholder="请输入密码（6-20 位）"
        prefix-icon="lock"
        show-password
        clearable
        :custom-style="inputStyle"
      />
      <wd-input
        v-model="realName"
        placeholder="请输入真实姓名"
        prefix-icon="user"
        clearable
        :custom-style="inputStyle"
      />
      <wd-input
        v-model="phone"
        type="tel"
        placeholder="请输入手机号"
        prefix-icon="phone"
        :maxlength="11"
        clearable
        :custom-style="inputStyle"
      />
      <wd-button
        type="primary"
        block
        :loading="loading"
        custom-class="register-btn"
        @click="handleRegister"
      >
        注 册
      </wd-button>
    </view>

    <!-- 反馈类组件需显式声明（Wot UI 规范） -->
    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  padding: 24rpx 48rpx 0;
  box-sizing: border-box;
  background: #f7f8fa;
}

.register-form {
  margin-top: 40rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx 32rpx 48rpx;
}

.register-btn {
  margin-top: 48rpx;
}
</style>
