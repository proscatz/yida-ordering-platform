import { request } from './http'
import type { PageResult } from '@/types/api'
import type { Dish, DishPageQuery, DishPayload } from '@/types/catalog'
import type { EnableStatus } from '@/types/management'

export const dishApi = {
  page: (params: DishPageQuery) => request<PageResult<Dish>>({ method: 'GET', url: '/dish/page', params }),
  detail: (id: number) => request<Dish>({ method: 'GET', url: `/dish/${id}` }),
  create: (data: DishPayload) => request<void>({ method: 'POST', url: '/dish', data }),
  update: (data: DishPayload) => request<void>({ method: 'PUT', url: '/dish', data }),
  remove: (ids: number[]) => request<void>({ method: 'DELETE', url: '/dish', params: { ids: ids.join(',') } }),
  setStatus: (id: number, status: EnableStatus) => request<void>({ method: 'POST', url: `/dish/status/${status}`, params: { id } }),
}
