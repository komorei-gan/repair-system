<script setup lang="ts">
/**
 * 个人中心 —— 列表长条形 UI（类似系统设置）。
 * - 修改密码：wd-popup 弹窗，对接 PUT /api/auth/password
 * - 报修人：展示信誉分（缓存值）
 * - 维修师傅：展示我的评分 / 累计接单量（GET /api/repairer/stats）
 * - 退出登录：uni.showModal 二次确认后清凭证回登录页
 */
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useToast } from '@wot-ui/ui'
import { changePassword } from '@/api/auth'
import { repairerStats } from '@/api/repairer'
import { clearToken, getCachedUser } from '@/utils/token'
import type { UserInfo } from '@/types/api'

const toast = useToast()

/** 本地缓存的登录用户 */
const user = ref<UserInfo | null>(getCachedUser<UserInfo>())
const displayName = ref(user.value?.realName || user.value?.username || '用户')

const isRepairer = computed(() => user.value?.role === 'REPAIRER')
const creditScore = computed(() => user.value?.creditScore ?? 100)

/** 师傅统计数据（评分 / 累计接单量） */
const rating = ref<number | null>(null)
const finishCount = ref(0)

/** 修改密码弹窗 */
const pwdVisible = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const pwdSubmitting = ref(false)

/** 三个密码框的明文/密文切换 */
const showOldPwd = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)

const inputStyle =
  'background:transparent;border-radius:20rpx;padding:28rpx 24rpx;color:#1f2d3d;'

/** 拉取师傅统计（失败静默，保留兜底展示） */
async function fetchRepairerStats() {
  if (!isRepairer.value) return
  try {
    const stats = await repairerStats()
    rating.value = stats.rating
    finishCount.value = stats.finishCount ?? 0
  } catch {
    // 静默失败：保持「暂无 / 0」兜底
  }
}

function toggleOldPwd() {
  showOldPwd.value = !showOldPwd.value
}

function toggleNewPwd() {
  showNewPwd.value = !showNewPwd.value
}

function toggleConfirmPwd() {
  showConfirmPwd.value = !showConfirmPwd.value
}

/** 打开修改密码弹窗并重置表单 */
function openChangePwd() {
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  pwdVisible.value = true
}

/** 提交修改密码：成功后强制退出回登录页 */
async function submitChangePwd() {
  if (!oldPassword.value) {
    toast.error('请输入原密码')
    return
  }
  if (!newPassword.value) {
    toast.error('请输入新密码')
    return
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    toast.error('新密码长度需在 6~32 位之间')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast.error('两次输入的新密码不一致')
    return
  }

  pwdSubmitting.value = true
  try {
    await changePassword(oldPassword.value, newPassword.value)
    toast.success('密码修改成功，请重新登录')
    // 强制退出：清凭证并回登录页
    clearToken()
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/login' })
    }, 600)
  } catch {
    // 错误提示已由 request 层统一 showToast 处理（如原密码不正确）
  } finally {
    pwdSubmitting.value = false
  }
}

/** 退出登录 */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    confirmColor: '#f5222d',
    success: (res) => {
      if (!res.confirm) return
      clearToken()
      uni.reLaunch({ url: '/pages/login/login' })
    },
  })
}

onLoad(() => {
  fetchRepairerStats()
})
</script>

<template>
  <view class="profile-page">
    <wd-navbar
      title="个人中心"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      bordered
      @click-left="uni.navigateBack()"
    />

    <!-- 用户信息卡 -->
    <view class="profile-card">
      <view class="avatar">{{ displayName.charAt(0) }}</view>
      <view class="profile-meta">
        <text class="profile-name">{{ displayName }}</text>
        <text class="profile-sub">{{ isRepairer ? '维修师傅' : '报修用户' }}</text>
      </view>
    </view>

    <!-- 列表长条形菜单 -->
    <view class="menu-list">
      <!-- 师傅：我的评分 / 累计接单量 -->
      <template v-if="isRepairer">
        <view class="menu-item">
          <view class="menu-icon menu-icon--amber">
            <wd-icon name="star" size="18px" />
          </view>
          <text class="menu-label">我的评分</text>
          <text class="menu-value">{{ rating != null ? `${rating} 分` : '暂无' }}</text>
        </view>
        <view class="menu-item">
          <view class="menu-icon menu-icon--green">
            <wd-icon name="check" size="18px" />
          </view>
          <text class="menu-label">累计接单量</text>
          <text class="menu-value menu-value--green">{{ finishCount }} 单</text>
        </view>
      </template>

      <!-- 报修人：信誉分 -->
      <template v-else>
        <view class="menu-item">
          <view class="menu-icon menu-icon--amber">
            <wd-icon name="star" size="18px" />
          </view>
          <text class="menu-label">信誉分</text>
          <text class="menu-value">{{ creditScore }}</text>
        </view>
      </template>

      <view class="menu-item no-border" hover-class="menu-item--active" @click="openChangePwd">
        <view class="menu-icon menu-icon--blue">
          <wd-icon name="lock" size="18px" />
        </view>
        <text class="menu-label">修改密码</text>
        <wd-icon name="arrow-right" size="16px" custom-class="menu-arrow" />
      </view>
    </view>

    <!-- 底部退出登录大按钮 -->
    <view class="logout-wrap">
      <wd-button block type="danger" plain custom-class="logout-btn" @click="handleLogout">
        退出登录
      </wd-button>
    </view>

    <!-- 修改密码弹窗 -->
    <wd-popup
      v-model="pwdVisible"
      position="bottom"
      round
      safe-area-inset-bottom
      custom-class="pwd-panel"
      @click-modal="pwdVisible = false"
    >
      <view class="pwd-panel__title">修改密码</view>
      <view class="pwd-panel__body">
        <view class="field">
          <view class="pwd-field">
            <wd-icon name="lock" size="18px" custom-class="pwd-field__prefix" />
            <!-- #ifdef H5 -->
            <input
              v-model="oldPassword"
              class="pwd-field__inner"
              :class="{ 'is-masked': oldPassword && !showOldPwd }"
              type="text"
              placeholder="请输入原密码"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="32"
            />
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <input
              v-model="oldPassword"
              class="pwd-field__inner"
              :password="oldPassword && !showOldPwd"
              placeholder="请输入原密码"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="32"
            />
            <!-- #endif -->
            <wd-icon
              v-if="oldPassword"
              name="close-circle"
              size="18px"
              custom-class="pwd-field__suffix"
              @click="oldPassword = ''"
            />
            <wd-icon
              :name="showOldPwd ? 'eye' : 'eye-invisible'"
              size="18px"
              custom-class="pwd-field__suffix"
              @click="toggleOldPwd"
            />
          </view>
        </view>

        <view class="field">
          <view class="pwd-field">
            <wd-icon name="lock" size="18px" custom-class="pwd-field__prefix" />
            <!-- #ifdef H5 -->
            <input
              v-model="newPassword"
              class="pwd-field__inner"
              :class="{ 'is-masked': newPassword && !showNewPwd }"
              type="text"
              placeholder="请输入新密码（6-32 位）"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="32"
            />
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <input
              v-model="newPassword"
              class="pwd-field__inner"
              :password="newPassword && !showNewPwd"
              placeholder="请输入新密码（6-32 位）"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="32"
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
              :class="{ 'is-masked': confirmPassword && !showConfirmPwd }"
              type="text"
              placeholder="请再次输入新密码"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="32"
            />
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <input
              v-model="confirmPassword"
              class="pwd-field__inner"
              :password="confirmPassword && !showConfirmPwd"
              placeholder="请再次输入新密码"
              placeholder-style="color:#b0b8c4;font-size:28rpx;"
              :maxlength="32"
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
      </view>
      <view class="pwd-panel__footer">
        <wd-button
          block
          type="primary"
          :loading="pwdSubmitting"
          custom-class="pwd-panel__confirm"
          @click="submitChangePwd"
        >
          确认修改
        </wd-button>
      </view>
    </wd-popup>

    <wd-toast />
  </view>
</template>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f4f7f9;
}

/* ===== 用户信息卡 ===== */
.profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin: 24rpx;
  padding: 40rpx 32rpx;
  background: linear-gradient(135deg, #35a0ff 0%, #1890ff 60%, #0b6fd0 100%);
  border-radius: 28rpx;
  box-shadow: 0 16rpx 32rpx -16rpx rgba(11, 111, 208, 0.5);
}
.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  font-weight: 700;
  color: #1890ff;
  background: #ffffff;
}
.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.profile-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}
.profile-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
}

/* ===== 列表菜单 ===== */
.menu-list {
  margin: 0 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 30rpx 28rpx;
  border-bottom: 1rpx solid #f0f3f7;
}
.menu-item--active {
  background: #f5f9ff;
}
.menu-item.no-border {
  border-bottom: 0;
}
.menu-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}
.menu-icon--blue {
  background: #eaf4ff;
  color: #1890ff;
}
.menu-icon--amber {
  background: #fff7e6;
  color: #faad14;
}
.menu-icon--green {
  background: #f6ffed;
  color: #52c41a;
}
.menu-label {
  flex: 1;
  font-size: 29rpx;
  font-weight: 500;
  color: #1f2d3d;
}
.menu-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #faad14;
}
.menu-value--green {
  color: #52c41a;
}
.menu-arrow {
  color: #c6ced8;
}

/* ===== 退出登录 ===== */
.logout-wrap {
  margin: 48rpx 24rpx 0;
}
:deep(.logout-btn) {
  border-radius: 999rpx !important;
  border: 1px solid #ffb3b8 !important;
  color: #f5222d !important;
  background: #fff !important;
}

/* ===== 修改密码弹窗 ===== */
.pwd-panel__title {
  padding: 40rpx 40rpx 8rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2d3d;
}
.pwd-panel__body {
  padding: 16rpx 40rpx 8rpx;
}
.pwd-panel__body .field {
  margin-bottom: 24rpx;
  background: #f5f7fa;
  border-radius: 20rpx;
  box-shadow: 0 6rpx 18rpx -6rpx rgba(31, 45, 61, 0.08);
  overflow: hidden;
}
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
.pwd-field__inner :deep(input) {
  height: 44rpx;
  font-size: 28rpx;
  color: #1f2d3d;
}
.pwd-field__inner.is-masked,
.pwd-field__inner.is-masked :deep(input) {
  -webkit-text-security: disc;
}
.pwd-field__suffix {
  margin-left: 16rpx;
  color: #8a94a6;
}
.pwd-panel__footer {
  padding: 8rpx 40rpx 48rpx;
}
:deep(.pwd-panel__confirm) {
  border-radius: 999rpx !important;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important;
}
</style>
