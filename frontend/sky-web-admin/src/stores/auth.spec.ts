import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { AppError } from '@/api/errors'
import { clearSession, readSession, sessionStorageKey } from '@/utils/session'
import { resetWriteLocksForTests } from '@/utils/writeLock'

vi.mock('@/api/auth', () => ({ login: vi.fn(), logout: vi.fn() }))

import { login as loginRequest, logout as logoutRequest } from '@/api/auth'
import { useAuthStore } from './auth'

const session = { id: 7, userName: 'manager', name: '值班经理', role: 'ADMIN' as const, token: 'jwt-token' }

describe('authentication store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearSession()
    resetWriteLocksForTests()
    vi.mocked(loginRequest).mockReset()
    vi.mocked(logoutRequest).mockReset()
  })

  it('persists a successful login session', async () => {
    vi.mocked(loginRequest).mockResolvedValue(session)
    const store = useAuthStore()

    await store.login({ username: 'manager', password: 'test-only' })

    expect(store.isAuthenticated).toBe(true)
    expect(store.displayName).toBe('值班经理')
    expect(store.username).toBe('manager')
    expect(store.isAdmin).toBe(true)
    expect(store.roleLabel).toBe('管理员')
    expect(readSession()).toEqual(session)
  })

  it('restores an employee role without granting administrator access', async () => {
    const employeeSession = { ...session, role: 'EMPLOYEE' as const }
    vi.mocked(loginRequest).mockResolvedValue(employeeSession)
    const store = useAuthStore()

    await store.login({ username: 'manager', password: 'test-only' })

    expect(store.isAdmin).toBe(false)
    expect(store.roleLabel).toBe('普通员工')
  })

  it('clears local authentication even when remote logout fails', async () => {
    vi.mocked(loginRequest).mockResolvedValue(session)
    vi.mocked(logoutRequest).mockRejectedValue(new AppError('network', 'network unavailable'))
    const store = useAuthStore()
    await store.login({ username: 'manager', password: 'test-only' })

    await expect(store.logout()).resolves.toBe(false)
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem(sessionStorageKey)).toBeNull()
  })
})
