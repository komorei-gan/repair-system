/**
 * 认证状态：token + 当前用户。
 * token 持久化到 localStorage（axios 拦截器自动读取）。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { UserInfo } from '@/types/api'
import { clearToken, getCachedUser, getToken, setCachedUser, setToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(getToken())
  const user = ref<UserInfo | null>(getCachedUser<UserInfo>())

  const isLoggedIn = computed(() => token.value !== '')

  /** 登录：调后端，成功即持久化 token 与用户信息 */
  async function login(username: string, password: string): Promise<void> {
    const res = await authApi.login(username, password)
    token.value = res.token
    user.value = res.user
    setToken(res.token)
    setCachedUser(res.user)
  }

  /** 登出：清空本地凭证 */
  function logout(): void {
    token.value = ''
    user.value = null
    clearToken()
  }

  /** 昵称：优先真实姓名，回退用户名 */
  const displayName = computed(() => user.value?.realName || user.value?.username || '管理员')
  /** 头像首字 */
  const avatarChar = computed(() => displayName.value.charAt(0))

  return { token, user, isLoggedIn, login, logout, displayName, avatarChar }
})
