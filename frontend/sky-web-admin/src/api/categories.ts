import { request } from './http'
import type { PageResult } from '@/types/api'
import type { Category, CategoryPageQuery, CategoryPayload, CategoryType, EnableStatus } from '@/types/management'

export const categoryApi = {
  page: (params: CategoryPageQuery) => request<PageResult<Category>>({ method: 'GET', url: '/category/page', params }),
  list: (type?: CategoryType) => request<Category[]>({ method: 'GET', url: '/category/list', params: { type } }),
  create: (data: CategoryPayload) => request<void>({ method: 'POST', url: '/category', data }),
  update: (data: CategoryPayload) => request<void>({ method: 'PUT', url: '/category', data }),
  remove: (id: number) => request<void>({ method: 'DELETE', url: '/category', params: { id } }),
  setStatus: (id: number, status: EnableStatus) => request<void>({
    method: 'POST',
    url: `/category/status/${status}`,
    params: { id },
  }),
}
