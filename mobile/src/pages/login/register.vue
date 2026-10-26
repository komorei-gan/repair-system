<script setup lang="ts">
/**
 * 注册页：对接 POST /api/auth/register（双角色）。
 * - 角色切换「我是用户 / 我是维修师傅」；
 * - 选师傅时多出「申请理由」输入框（必填）；
 * - 报修人注册成功直接返回登录页，师傅提交后提示等待审核。
 */
import { ref } from 'vue'
import { useToast } from '@wot-ui/ui'
import { register } from '@/api/auth'

const toast = useToast()

const role = ref<'REPORTER' | 'REPAIRER'>('REPORTER')
const username = ref('')
const password = ref('')
const realName = ref('')
const phone = ref('')
const reason = ref('')
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
  'background:transparent;border-radius:20rpx;padding:28rpx 24rpx;color:var(--text-main);'

const textareaStyle =
  'background:transparent;border-radius:20rpx;padding:24rpx;color:var(--text-main);min-height:180rpx;'

function switchRole(value: 'REPORTER' | 'REPAIRER') {
  role.value = value
  reason.value = ''
}

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
  if (role.value === 'REPAIRER') {
    if (!reason.value.trim()) {
      toast.error('请输入申请理由')
      return
    }
    if (reason.value.trim().length > 500) {
      toast.error('申请理由不能超过 500 字')
      return
    }
  }

  loading.value = true
  try {
    await register({
      username: name,
      password: password.value,
      realName: realName.value.trim(),
      phone: phone.value.trim(),
      role: role.value,
      ...(role.value === 'REPAIRER' ? { reason: reason.value.trim() } : {}),
    })
    toast.success(role.value === 'REPAIRER' ? '提交成功，请等待管理员审核' : '注册成功')
    setTimeout(() => {
      uni.navigateBack()
    }, 600)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理
  } finally {
    loading.value = false
  }
}

function goBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="register-page">
    <!-- 顶部渐变品牌区 -->
    <view class="brand">
      <view class="back" @click="goBack">
        <wd-icon name="arrow-left" size="22px" custom-class="back__icon" />
      </view>
      <image class="brand-logo" src="/static/logo.png" mode="aspectFit" />
      <text class="brand-title">创建账号</text>
      <text class="brand-subtitle">选择身份，开启报修服务</text>
    </view>

    <!-- 悬浮卡片表单 -->
    <view class="register-card">
      <!-- 角色切换 -->
      <view class="role-switch">
        <view
          class="role-item"
          :class="{ 'is-active': role === 'REPORTER' }"
          @click="switchRole('REPORTER')"
        >
          <wd-icon name="user" size="20px" custom-class="role-item__icon" />
          <text class="role-item__text">我是用户</text>
        </view>
        <view
          class="role-item"
          :class="{ 'is-active': role === 'REPAIRER' }"
          @click="switchRole('REPAIRER')"
        >
          <wd-icon name="tool" size="20px" custom-class="role-item__icon" />
          <text class="role-item__text">我是维修师傅</text>
        </view>
      </view>

      <view class="field">
        <wd-input
          v-model="username"
          placeholder="请输入用户名（3-50 个字符）"
          prefix-icon="user"
          clearable
          :custom-style="inputStyle"
        />
      </view>
      <view class="field">
        <view class="pwd-field">
          <wd-icon name="lock" size="18px" custom-class="pwd-field__prefix" />
          <!-- #ifdef H5 -->
          <!-- H5：有内容时才 text-security 遮蔽，空态保留文字 placeholder -->
          <input
            v-model="password"
            class="pwd-field__inner"
            :class="{ 'is-masked': password && !showPwd }"
            type="text"
            placeholder="请输入密码（6-20 位）"
            placeholder-style="color:var(--text-placeholder);font-size:28rpx;"
            :maxlength="20"
          />
          <!-- #endif -->
          <!-- #ifndef H5 -->
          <input
            v-model="password"
            class="pwd-field__inner"
            :password="password && !showPwd"
            placeholder="请输入密码（6-20 位）"
            placeholder-style="color:var(--text-placeholder);font-size:28rpx;"
            :maxlength="20"
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
      <view class="field">
        <wd-input
          v-model="realName"
          placeholder="请输入真实姓名"
          prefix-icon="user"
          clearable
          :custom-style="inputStyle"
        />
      </view>
      <view class="field">
        <wd-input
          v-model="phone"
          type="tel"
          placeholder="请输入手机号"
          prefix-icon="phone"
          :maxlength="11"
          clearable
          :custom-style="inputStyle"
        />
      </view>

      <!-- 仅师傅：申请理由 -->
      <view v-if="role === 'REPAIRER'" class="field field--textarea">
        <wd-textarea
          v-model="reason"
          placeholder="请填写申请理由（如：本人有 5 年水电维修经验）"
          :maxlength="500"
          show-word-limit
          auto-height
          :custom-style="textareaStyle"
        />
      </view>

      <wd-button
        block
        :loading="loading"
        custom-class="register-btn"
        @click="handleRegister"
      >
        {{ role === 'REPAIRER' ? '提交申请' : '注 册' }}
      </wd-button>
    </view>

    <!-- 反馈类组件需显式声明（Wot UI 规范） -->
    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  box-sizing: border-box;
  background: var(--bg-page);
}

/* ===== 顶部渐变品牌区 ===== */
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 48rpx 160rpx;
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
.back {
  position: absolute;
  left: 28rpx;
  top: 88rpx;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  border: 1rpx solid rgba(255, 255, 255, 0.24);
}
.back__icon {
  color: #ffffff;
}
.brand-logo {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 20rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: 0 16rpx 32rpx -12rpx rgba(11, 111, 208, 0.6);
}
.brand-title {
  font-size: 48rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  color: #ffffff;
}
.brand-subtitle {
  margin-top: 12rpx;
  font-size: 26rpx;
  letter-spacing: 1rpx;
  color: rgba(255, 255, 255, 0.82);
}

/* ===== 悬浮卡片 ===== */
.register-card {
  margin: -96rpx 40rpx 60rpx;
  padding: 44rpx 36rpx 48rpx;
  background: var(--bg-card);
  border-radius: 32rpx;
  box-shadow: 0 24rpx 60rpx -20rpx rgba(16, 62, 118, 0.24);
  position: relative;
  z-index: 1;
}

/* ===== 角色切换 ===== */
.role-switch {
  display: flex;
  gap: 20rpx;
  margin-bottom: 36rpx;
}
.role-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 24rpx 12rpx;
  border-radius: 20rpx;
  background: var(--bg-input);
  border: 2rpx solid transparent;
  color: var(--text-sub);
  transition: all 0.2s;
}
.role-item__icon {
  color: var(--text-muted);
}
.role-item__text {
  font-size: 28rpx;
  font-weight: 500;
}
.role-item.is-active {
  background: #eaf4ff;
  border-color: #1890ff;
  color: #1890ff;
}
.role-item.is-active .role-item__icon {
  color: #1890ff;
}

/* 输入框外层：悬浮阴影卡片态 */
.field {
  margin-bottom: 28rpx;
  background: var(--bg-input);
  border-radius: 20rpx;
  box-shadow: 0 6rpx 18rpx -6rpx rgba(31, 45, 61, 0.08);
  overflow: hidden;
}
.field--textarea {
  padding: 8rpx 0;
}

/* ===== 自定义密码框（眼睛与清除按钮完全自控） ===== */
.pwd-field {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
}
.pwd-field__prefix {
  margin-right: 12rpx;
  color: var(--text-muted);
}
.pwd-field__inner {
  flex: 1;
  min-width: 0;
  height: 44rpx;
  padding: 0;
  font-size: 28rpx;
  color: var(--text-main);
  background: transparent;
  border: none;
  outline: none;
}
/* 兼容 uni-app H5：真实可编辑元素在 <uni-input> 包裹层内部 */
.pwd-field__inner :deep(input) {
  height: 44rpx;
  font-size: 28rpx;
  color: var(--text-main);
}
/* H5：用字符遮蔽代替原生密码框，避免浏览器叠加自带的眼睛图标 */
.pwd-field__inner.is-masked,
.pwd-field__inner.is-masked :deep(input) {
  -webkit-text-security: disc;
}
.pwd-field__suffix {
  margin-left: 16rpx;
  color: var(--text-muted);
}

/* ===== 渐变蓝大圆角按钮 ===== */
:deep(.register-btn) {
  margin-top: 24rpx;
  border-radius: 999rpx !important;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  box-shadow: 0 16rpx 32rpx -12rpx rgba(24, 144, 255, 0.65) !important;
  border: none !important;
}
</style>
