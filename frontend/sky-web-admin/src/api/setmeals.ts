import { request } from './http'
import type { PageResult } from '@/types/api'
import type { Setmeal, SetmealPageQuery, SetmealPayload } from '@/types/catalog'
import type { EnableStatus } from '@/types/management'

export const setmealApi = {
  page: (params: SetmealPageQuery) => request<PageResult<Setmeal>>({ method: 'GET', url: '/setmeal/page', params }),
  detail: (id: number) => request<Setmeal>({ method: 'GET', url: `/setmeal/${id}` }),
  create: (data: SetmealPayload) => request<void>({ method: 'POST', url: '/setmeal', data }),
  update: (data: SetmealPayload) => request<void>({ method: 'PUT', url: '/setmeal', data }),
  remove: (ids: number[]) => request<void>({ method: 'DELETE', url: '/setmeal', params: { ids: ids.join(',') } }),
  setStatus: (id: number, status: EnableStatus) => request<void>({ method: 'POST', url: `/setmeal/status/${status}`, params: { id } }),
}
