<script setup lang="ts">
/**
 * 修改密码 —— 系统设置右侧内容块。
 * 修改成功后清空凭证并跳转登录页，要求重新登录。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需在 6~32 位之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return // 校验失败
  }

  submitting.value = true
  try {
    await changePassword(form.oldPassword, form.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    // 清空凭证与状态，跳转登录页
    authStore.logout()
    appStore.reset()
    localStorage.removeItem('repair_token')
    localStorage.removeItem('repair_user')
    void router.replace('/login')
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="content-block">
    <div class="block-title">
      <h2>修改登录密码</h2>
      <p>修改成功后需要重新登录</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="password-form"
    >
      <el-form-item label="原密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          placeholder="请输入原密码"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="6~32 位"
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
        />
      </el-form-item>

      <el-button type="primary" :loading="submitting" @click="onSubmit">
        确认修改
      </el-button>
    </el-form>
  </div>
</template>

<style scoped>
.content-block {
  min-width: 0;
}
.block-title {
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}
.block-title h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-2);
}
.block-title p {
  font-size: 11.5px;
  color: var(--muted-2);
  margin-top: 4px;
}
.password-form {
  max-width: 360px;
}
</style>
