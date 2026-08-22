import { AppError, isAppError } from '@/api/errors'
import { orderApi } from '@/api/orders'
import { withWriteLock } from '@/utils/writeLock'
import { canOrderAction } from '@/utils/orderState'
import type { OrderAction, OrderStatus } from '@/types/orders'

export function isOrderStateConflict(error: unknown) {
  return isAppError(error)
    && error.kind === 'business'
    && /订单状态|状态错误|状态已/.test(error.message)
}

export function orderActionErrorMessage(error: unknown) {
  if (isOrderStateConflict(error)) return '订单状态已被其他操作修改，已刷新为服务端最新状态'
  if (isAppError(error)) return error.message
  return '订单操作失败，请稍后重试'
}

export function executeOrderAction(id: number, status: OrderStatus, action: OrderAction, reason?: string) {
  if (!canOrderAction(status, action)) {
    return Promise.reject(new AppError('business', '当前订单状态不允许执行此操作'))
  }

  const normalizedReason = reason?.trim()
  if ((action === 'reject' || action === 'cancel') && !normalizedReason) {
    return Promise.reject(new AppError('business', action === 'reject' ? '请填写拒单原因' : '请填写取消原因'))
  }

  return withWriteLock(`order:${action}:${id}`, () => {
    switch (action) {
      case 'confirm': return orderApi.confirm({ id, status: 3 })
      case 'reject': return orderApi.reject({ id, rejectionReason: normalizedReason! })
      case 'cancel': return orderApi.cancel({ id, cancelReason: normalizedReason! })
      case 'deliver': return orderApi.deliver(id)
      case 'complete': return orderApi.complete(id)
    }
  })
}
