import type { DateTimeString, Money, PaginationQuery } from './api'

export type OrderStatus = 1 | 2 | 3 | 4 | 5 | 6
export type OrderPayStatus = 0 | 1 | 2
export type OrderAction = 'confirm' | 'reject' | 'cancel' | 'deliver' | 'complete'

export interface OrderDetailItem {
  id: number
  name: string
  orderId: number
  dishId: number | null
  setmealId: number | null
  dishFlavor: string | null
  number: number
  amount: Money
  image: string | null
}

export interface Order {
  id: number
  number: string
  requestId: string | null
  status: OrderStatus
  userId: number
  addressBookId: number
  orderTime: DateTimeString
  checkoutTime: DateTimeString | null
  payMethod: number
  payStatus: OrderPayStatus
  amount: Money
  remark: string | null
  userName: string | null
  phone: string
  address: string
  consignee: string
  cancelReason: string | null
  rejectionReason: string | null
  cancelTime: DateTimeString | null
  estimatedDeliveryTime: DateTimeString | null
  deliveryStatus: number
  deliveryTime: DateTimeString | null
  packAmount: number
  tablewareNumber: number
  tablewareStatus: number
  orderDishes?: string | null
  orderDetailList?: OrderDetailItem[]
}

export interface OrderPageQuery extends PaginationQuery {
  number?: string
  phone?: string
  status?: OrderStatus
  beginTime?: string
  endTime?: string
}

export interface OrderStatistics {
  toBeConfirmed: number
  confirmed: number
  deliveryInProgress: number
}

export interface OrderConfirmPayload {
  id: number
  status: 3
}

export interface OrderRejectionPayload {
  id: number
  rejectionReason: string
}

export interface OrderCancelPayload {
  id: number
  cancelReason: string
}

export interface OrderSocketMessage {
  type: 1 | 2
  orderId: number
  content?: string
  status?: OrderStatus
}

export type OrderSocketStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'degraded' | 'auth-failed' | 'closed'

export interface OrderSocketConnectionState {
  status: OrderSocketStatus
  retryCount: number
  lastDisconnectedAt: number | null
  outageStartedAt: number | null
  lastCloseCode: number | null
  waitingForRetry: boolean
}
