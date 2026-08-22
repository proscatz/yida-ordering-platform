import type { AdminSession } from '@/types/api'

const SESSION_KEY = 'yida-admin-session'

function jwtExpiresAt(token: string): number | null {
  const payload = token.split('.')[1]
  if (!payload) return null

  try {
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = JSON.parse(atob(normalized)) as { exp?: unknown }
    return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null
  } catch {
    return null
  }
}

export function isSessionExpired(session: AdminSession, now = Date.now()) {
  const expiresAt = jwtExpiresAt(session.token)
  return expiresAt !== null && expiresAt <= now
}

export function readSession(): AdminSession | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null

  try {
    const value = JSON.parse(raw) as Partial<AdminSession>
    if (!value.token || !value.id || !value.userName || !value.name
      || (value.role !== 'ADMIN' && value.role !== 'EMPLOYEE')) {
      localStorage.removeItem(SESSION_KEY)
      return null
    }
    const session = value as AdminSession
    if (isSessionExpired(session)) {
      localStorage.removeItem(SESSION_KEY)
      return null
    }
    return session
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function writeSession(session: AdminSession) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export const sessionStorageKey = SESSION_KEY
