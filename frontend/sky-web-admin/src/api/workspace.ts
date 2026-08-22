import { request } from './http'
import type { BusinessData, OrderOverview, ProductOverview } from '@/types/management'

export const workspaceApi = {
  businessData: () => request<BusinessData>({ method: 'GET', url: '/workspace/businessData' }),
  orderOverview: () => request<OrderOverview>({ method: 'GET', url: '/workspace/overviewOrders' }),
  dishOverview: () => request<ProductOverview>({ method: 'GET', url: '/workspace/overviewDishes' }),
  setmealOverview: () => request<ProductOverview>({ method: 'GET', url: '/workspace/overviewSetmeals' }),
}
