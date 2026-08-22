import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useOrderRealtimeStore } from './orderRealtime'

describe('order realtime store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('keeps connection diagnostics in memory only', () => {
    const store = useOrderRealtimeStore()
    store.setConnectionState({
      status: 'reconnecting', retryCount: 2, lastDisconnectedAt: 1_000,
      outageStartedAt: 500, lastCloseCode: 1006, waitingForRetry: true,
    })

    expect(store.connection).toMatchObject({ status: 'reconnecting', retryCount: 2, lastCloseCode: 1006 })
    expect(localStorage.length).toBe(0)
  })

  it('locks repeated manual retry requests until the attempt finishes', () => {
    const store = useOrderRealtimeStore()
    const retry = vi.fn(() => true)
    store.registerRetryHandler(retry)

    expect(store.retryNow()).toBe(true)
    expect(store.retryNow()).toBe(false)
    expect(retry).toHaveBeenCalledTimes(1)

    store.setConnectionState({
      status: 'connected', retryCount: 0, lastDisconnectedAt: 1_000,
      outageStartedAt: null, lastCloseCode: 1006, waitingForRetry: false,
    })
    expect(store.manualRetrying).toBe(false)
  })
})
