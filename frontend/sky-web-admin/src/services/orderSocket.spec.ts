import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { OrderSocketConnectionState } from '@/types/orders'
import {
  buildOrderSocketUrl,
  OrderSocketClient,
  parseOrderSocketMessage,
  type SocketLike,
} from './orderSocket'

class FakeSocket implements SocketLike {
  readyState = 0
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null

  close() { this.readyState = 3 }
  open() { this.readyState = 1; this.onopen?.(new Event('open')) }
  message(data: string) { this.onmessage?.({ data } as MessageEvent) }
  serverClose(code: number) { this.readyState = 3; this.onclose?.({ code } as CloseEvent) }
}

function createHarness(options: Record<string, unknown> = {}) {
  const sockets: FakeSocket[] = []
  const states: OrderSocketConnectionState[] = []
  const received = vi.fn()
  const createSocket = vi.fn(() => {
    const socket = new FakeSocket()
    sockets.push(socket)
    return socket
  })
  const client = new OrderSocketClient({
    sid: 8,
    token: 'test-token',
    origin: 'http://localhost:5174',
    onMessage: received,
    onStateChange: (state) => states.push(state),
    createSocket,
    baseRetryDelay: 100,
    maxRetryDelay: 400,
    degradedRetryDelay: 400,
    ...options,
  })
  return { client, sockets, states, received, createSocket }
}

describe('order WebSocket client', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('URL-encodes identity and token query values', () => {
    const url = buildOrderSocketUrl('https://admin.example.test/', '8/9', 'a+b /?=')
    expect(url).toBe('wss://admin.example.test/ws/8%2F9?token=a%2Bb%20%2F%3F%3D')
  })

  it('parses only the real new-order and reminder message shapes', () => {
    expect(parseOrderSocketMessage('{"type":1,"orderId":8,"content":"订单号：A8"}')).toEqual({ type: 1, orderId: 8, content: '订单号：A8' })
    expect(parseOrderSocketMessage('{"type":2,"orderId":8,"status":3}')).toEqual({ type: 2, orderId: 8, status: 3 })
    expect(parseOrderSocketMessage('{"event":"new"}')).toBeNull()
    expect(parseOrderSocketMessage('not-json')).toBeNull()
  })

  it('emits connecting for the first connection', () => {
    const { client, sockets, states } = createHarness()

    expect(client.start()).toBe(true)

    expect(sockets).toHaveLength(1)
    expect(states.at(-1)).toMatchObject({ status: 'connecting', retryCount: 0, waitingForRetry: false })
  })

  it('emits connected after a successful open', () => {
    const { client, sockets, states } = createHarness()
    client.start()

    sockets[0]!.open()

    expect(states.at(-1)).toMatchObject({ status: 'connected', retryCount: 0, waitingForRetry: false })
  })

  it('shows the first abnormal close as the first reconnect attempt', () => {
    const { client, sockets, states } = createHarness({ now: () => 12_000 })
    client.start()
    sockets[0]!.open()

    sockets[0]!.serverClose(1006)

    expect(states.at(-1)).toMatchObject({
      status: 'reconnecting', retryCount: 1, lastDisconnectedAt: 12_000,
      lastCloseCode: 1006, waitingForRetry: true,
    })
  })

  it('enters degraded after four consecutive abnormal closes', () => {
    const { client, sockets, states } = createHarness()
    client.start()

    for (let attempt = 0; attempt < 4; attempt += 1) {
      sockets[attempt]!.serverClose(1006)
      if (attempt < 3) vi.advanceTimersByTime(100 * 2 ** attempt)
    }

    expect(states.at(-1)).toMatchObject({ status: 'degraded', retryCount: 4, waitingForRetry: true })
  })

  it('enters degraded when an outage lasts thirty seconds', () => {
    const { client, sockets, states } = createHarness({
      baseRetryDelay: 60_000, maxRetryDelay: 60_000, degradedAfterMs: 30_000,
    })
    client.start()
    sockets[0]!.serverClose(1006)

    vi.advanceTimersByTime(30_000)

    expect(states.at(-1)?.status).toBe('degraded')
  })

  it('returns to connected and resets retry count after recovery', () => {
    const { client, sockets, states } = createHarness()
    client.start()
    sockets[0]!.serverClose(1006)
    vi.advanceTimersByTime(100)

    sockets[1]!.open()

    expect(states.at(-1)).toMatchObject({ status: 'connected', retryCount: 0, outageStartedAt: null })
  })

  it('locks manual retry while one connection attempt is active', () => {
    const { client, sockets, createSocket } = createHarness()
    client.start()
    sockets[0]!.serverClose(1006)

    expect(client.retryNow()).toBe(true)
    expect(client.retryNow()).toBe(false)
    expect(createSocket).toHaveBeenCalledTimes(2)
  })

  it('allows a manual retry to replace a hanging connection after degradation', () => {
    const { client, sockets, createSocket } = createHarness({
      baseRetryDelay: 60_000, maxRetryDelay: 60_000, degradedAfterMs: 30_000,
    })
    client.start()
    sockets[0]!.serverClose(1006)
    vi.advanceTimersByTime(30_000)
    vi.advanceTimersByTime(30_000)
    expect(createSocket).toHaveBeenCalledTimes(2)

    expect(client.retryNow()).toBe(true)
    expect(createSocket).toHaveBeenCalledTimes(3)
  })

  it('coalesces visibility and online resume into one connection', () => {
    const { client, sockets, createSocket } = createHarness()
    client.start()
    sockets[0]!.serverClose(1006)

    expect(client.resume()).toBe(true)
    expect(client.resume()).toBe(false)
    expect(createSocket).toHaveBeenCalledTimes(2)
  })

  it.each([1008, 4001])('stops retrying after authentication close code %s', (code) => {
    const authFailure = vi.fn()
    const { client, sockets, states, createSocket } = createHarness({ onAuthFailure: authFailure })
    client.start()

    sockets[0]!.serverClose(code)
    vi.runAllTimers()

    expect(states.at(-1)).toMatchObject({ status: 'auth-failed', lastCloseCode: code, waitingForRetry: false })
    expect(client.resume()).toBe(false)
    expect(createSocket).toHaveBeenCalledTimes(1)
    expect(authFailure).toHaveBeenCalledTimes(1)
  })

  it('treats 1011 as a degraded service failure', () => {
    const { client, sockets, states } = createHarness()
    client.start()

    sockets[0]!.serverClose(1011)

    expect(states.at(-1)).toMatchObject({ status: 'degraded', lastCloseCode: 1011, waitingForRetry: true })
  })

  it('treats 1000 as a normal closed state without reconnecting', () => {
    const { client, sockets, states, createSocket } = createHarness()
    client.start()

    sockets[0]!.serverClose(1000)
    vi.runAllTimers()

    expect(states.at(-1)).toMatchObject({ status: 'closed', lastCloseCode: 1000, waitingForRetry: false })
    expect(createSocket).toHaveBeenCalledTimes(1)
  })

  it('caps exponential retry delay at the configured maximum', () => {
    const { client, sockets, createSocket } = createHarness({ degradedAfterAttempts: 99 })
    client.start()
    sockets[0]!.serverClose(1006)
    vi.advanceTimersByTime(100)
    sockets[1]!.serverClose(1006)
    vi.advanceTimersByTime(200)
    sockets[2]!.serverClose(1006)
    vi.advanceTimersByTime(400)
    sockets[3]!.serverClose(1006)

    vi.advanceTimersByTime(399)
    expect(createSocket).toHaveBeenCalledTimes(4)
    vi.advanceTimersByTime(1)
    expect(createSocket).toHaveBeenCalledTimes(5)
  })

  it('deduplicates repeated messages and allows them again after the TTL', () => {
    let now = 1_000
    const { client, sockets, received } = createHarness({ dedupTtl: 1_000, now: () => now })
    client.start()
    sockets[0]!.open()
    sockets[0]!.message('{"type":2,"orderId":8,"status":3}')
    sockets[0]!.message('{"type":2,"orderId":8,"status":3}')
    expect(received).toHaveBeenCalledTimes(1)
    now += 1_001
    sockets[0]!.message('{"type":2,"orderId":8,"status":3}')
    expect(received).toHaveBeenCalledTimes(2)
  })
})
