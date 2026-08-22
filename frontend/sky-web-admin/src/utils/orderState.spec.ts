import { describe, expect, it } from 'vitest'
import { allowedOrderActions, canOrderAction, orderStatusLabel } from './orderState'

describe('order state action matrix', () => {
  it('exposes only the operations allowed by the current server state', () => {
    expect(allowedOrderActions(1)).toEqual(['cancel'])
    expect(allowedOrderActions(2)).toEqual(['confirm', 'reject'])
    expect(allowedOrderActions(3)).toEqual(['deliver', 'cancel'])
    expect(allowedOrderActions(4)).toEqual(['complete', 'cancel'])
    expect(allowedOrderActions(5)).toEqual([])
    expect(allowedOrderActions(6)).toEqual([])
  })

  it('rejects illegal transitions instead of inferring actions from labels', () => {
    expect(canOrderAction(2, 'confirm')).toBe(true)
    expect(canOrderAction(2, 'deliver')).toBe(false)
    expect(canOrderAction(5, 'cancel')).toBe(false)
    expect(orderStatusLabel(4)).toBe('派送中')
  })
})
