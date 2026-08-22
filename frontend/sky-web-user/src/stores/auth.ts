import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/modules'
import type { LoginPayload } from '@/types'

const TOKEN_KEY = 'yida-user-token'
const USER_KEY = 'yida-user-id'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userId = ref(localStorage.getItem(USER_KEY) || '')
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(payload: LoginPayload): Promise<void> {
    const result = await authApi.login(payload)
    token.value = result.token
    userId.value = String(result.id)
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, String(result.id))
  }

  function clear(): void {
    token.value = ''
    userId.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) await authApi.logout()
    } finally {
      clear()
    }
  }

  return { token, userId, isAuthenticated, login, logout, clear }
})