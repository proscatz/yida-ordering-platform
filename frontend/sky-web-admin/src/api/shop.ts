import { request } from './http'
import type { EnableStatus } from '@/types/management'

export const shopApi = {
  getStatus: () => request<EnableStatus>({ method: 'GET', url: '/shop/status' }),
  setStatus: (status: EnableStatus) => request<void>({ method: 'PUT', url: `/shop/${status}` }),
}
