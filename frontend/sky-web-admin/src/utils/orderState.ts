import type { OrderAction, OrderStatus } from '@/types/orders'

export const ORDER_STATUS_META: Record<OrderStatus, { label: string; tone: 'info' | 'warning' | 'primary' | 'success' | 'danger' }> = {
  1: { label: '待付款', tone: 'info' },
  2: { label: '待接单', tone: 'warning' },
  3: { label: '已接单', tone: 'primary' },
  4: { label: '派送中', tone: 'primary' },
  5: { label: '已完成', tone: 'success' },
  6: { label: '已取消', tone: 'danger' },
}

export const ORDER_ACTION_META: Record<OrderAction, { label: string; target: OrderStatus; tone: 'primary' | 'danger' | 'warning' | 'success' }> = {
  confirm: { label: '接单', target: 3, tone: 'primary' },
  reject: { label: '拒单', target: 6, tone: 'danger' },
  cancel: { label: '取消', target: 6, tone: 'danger' },
  deliver: { label: '派送', target: 4, tone: 'warning' },
  complete: { label: '完成', target: 5, tone: 'success' },
}

const ACTIONS_BY_STATUS: Record<OrderStatus, readonly OrderAction[]> = {
  1: ['cancel'],
  2: ['confirm', 'reject'],
  3: ['deliver', 'cancel'],
  4: ['complete', 'cancel'],
  5: [],
  6: [],
}

export function allowedOrderActions(status: OrderStatus): readonly OrderAction[] {
  return ACTIONS_BY_STATUS[status] ?? []
}

export function canOrderAction(status: OrderStatus, action: OrderAction) {
  return allowedOrderActions(status).includes(action)
}

export function orderStatusLabel(status: OrderStatus) {
  return ORDER_STATUS_META[status]?.label ?? `未知状态（${status}）`
}
