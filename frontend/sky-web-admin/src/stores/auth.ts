import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginRequest, logout as logoutRequest } from '@/api/auth'
import { isAppError } from '@/api/errors'
import { markAuthenticatedSession } from '@/api/unauthorized'
import { clearSession, readSession, writeSession } from '@/utils/session'
import { withWriteLock } from '@/utils/writeLock'
import type { LoginPayload } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const session = ref(readSession())
  const loggingOut = ref(false)
  const isAuthenticated = computed(() => Boolean(session.value?.token))
  const displayName = computed(() => session.value?.name || session.value?.userName || '管理员')
  const username = computed(() => session.value?.userName || '')
  const isAdmin = computed(() => session.value?.role === 'ADMIN')
  const roleLabel = computed(() => isAdmin.value ? '管理员' : '普通员工')

  async function login(payload: LoginPayload) {
    const result = await withWriteLock('auth:login', () => loginRequest(payload))
    session.value = result
    writeSession(result)
    markAuthenticatedSession()
  }

  function clearLocalSession() {
    session.value = null
    clearSession()
  }

  function restoreSession() {
    session.value = readSession()
    if (session.value) markAuthenticatedSession()
  }

  async function logout(): Promise<boolean> {
    if (loggingOut.value) return false
    loggingOut.value = true
    let remoteConfirmed = true

    try {
      if (session.value?.token) {
        await withWriteLock('auth:logout', logoutRequest)
      }
    } catch (error) {
      remoteConfirmed = isAppError(error) && error.kind === 'auth'
    } finally {
      clearLocalSession()
      loggingOut.value = false
    }

    return remoteConfirmed
  }

  return {
    session,
    loggingOut,
    isAuthenticated,
    displayName,
    username,
    isAdmin,
    roleLabel,
    login,
    logout,
    clearLocalSession,
    restoreSession,
  }
})
