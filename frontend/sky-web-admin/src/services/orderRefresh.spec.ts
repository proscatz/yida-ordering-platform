import { describe, expect, it, vi } from 'vitest'
import { createOrderDataRefresher } from './orderRefresh'

describe('order data refresh action', () => {
  it('refreshes both the order list and statistics', async () => {
    const loadOrders = vi.fn().mockResolvedValue(undefined)
    const loadStatistics = vi.fn().mockResolvedValue(undefined)
    const refresh = createOrderDataRefresher(loadOrders, loadStatistics)

    await refresh()

    expect(loadOrders).toHaveBeenCalledTimes(1)
    expect(loadStatistics).toHaveBeenCalledTimes(1)
  })

  it('shares one in-flight refresh when the button is clicked repeatedly', async () => {
    let finish!: () => void
    const loadOrders = vi.fn(() => new Promise<void>((resolve) => { finish = resolve }))
    const loadStatistics = vi.fn().mockResolvedValue(undefined)
    const refresh = createOrderDataRefresher(loadOrders, loadStatistics)

    const first = refresh()
    const second = refresh()

    expect(loadOrders).toHaveBeenCalledTimes(1)
    expect(loadStatistics).toHaveBeenCalledTimes(1)
    finish()
    await Promise.all([first, second])
  })
})
