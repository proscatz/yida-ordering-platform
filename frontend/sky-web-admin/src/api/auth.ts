import { request } from './http'
import type { AdminSession, LoginPayload } from '@/types/api'

export function login(payload: LoginPayload): Promise<AdminSession> {
  return request<AdminSession>({ method: 'POST', url: '/employee/login', data: payload })
}

export function logout(): Promise<void> {
  return request<void>({ method: 'POST', url: '/employee/logout' })
}
