import { request } from './http'
import type { PageResult } from '@/types/api'
import type {
  Order,
  OrderCancelPayload,
  OrderConfirmPayload,
  OrderPageQuery,
  OrderRejectionPayload,
  OrderStatistics,
} from '@/types/orders'

export const orderApi = {
  page: (params: OrderPageQuery) => request<PageResult<Order>>({
    method: 'GET',
    url: '/order/conditionSearch',
    params,
  }),
  statistics: () => request<OrderStatistics>({ method: 'GET', url: '/order/statistics' }),
  detail: (id: number) => request<Order>({ method: 'GET', url: `/order/details/${id}` }),
  confirm: (data: OrderConfirmPayload) => request<void>({ method: 'PUT', url: '/order/confirm', data }),
  reject: (data: OrderRejectionPayload) => request<void>({ method: 'PUT', url: '/order/rejection', data }),
  cancel: (data: OrderCancelPayload) => request<void>({ method: 'PUT', url: '/order/cancel', data }),
  deliver: (id: number) => request<void>({ method: 'PUT', url: `/order/delivery/${id}` }),
  complete: (id: number) => request<void>({ method: 'PUT', url: `/order/complete/${id}` }),
}
