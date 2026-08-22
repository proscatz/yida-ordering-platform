import type { OrderSocketConnectionState, OrderSocketMessage, OrderSocketStatus } from '@/types/orders'

const SOCKET_CONNECTING = 0
const SOCKET_OPEN = 1
const NORMAL_CLOSE_CODE = 1000
const SERVER_ERROR_CODE = 1011
const AUTH_FAILURE_CODES = new Set([1008, 4001])
const DEFAULT_DEDUP_TTL = 5 * 60_000
const DEFAULT_DEDUP_LIMIT = 200
const DEFAULT_DEGRADED_AFTER_MS = 30_000
const DEFAULT_DEGRADED_AFTER_ATTEMPTS = 4

export interface SocketLike {
  readyState: number
  onopen: ((event: Event) => void) | null
  onmessage: ((event: MessageEvent) => void) | null
  onclose: ((event: CloseEvent) => void) | null
  onerror: ((event: Event) => void) | null
  close(code?: number, reason?: string): void
}

interface OrderSocketClientOptions {
  sid: number | string
  token: string
  origin?: string
  createSocket?: (url: string) => SocketLike
  onMessage: (message: OrderSocketMessage) => void
  onStateChange?: (state: OrderSocketConnectionState) => void
  onAuthFailure?: () => void | Promise<void>
  baseRetryDelay?: number
  maxRetryDelay?: number
  degradedRetryDelay?: number
  degradedAfterMs?: number
  degradedAfterAttempts?: number
  dedupTtl?: number
  now?: () => number
}

const initialState = (): OrderSocketConnectionState => ({
  status: 'idle',
  retryCount: 0,
  lastDisconnectedAt: null,
  outageStartedAt: null,
  lastCloseCode: null,
  waitingForRetry: false,
})

export function buildOrderSocketUrl(origin: string, sid: number | string, token: string) {
  const socketOrigin = origin.replace(/^http:/i, 'ws:').replace(/^https:/i, 'wss:').replace(/\/$/, '')
  return `${socketOrigin}/ws/${encodeURIComponent(String(sid))}?token=${encodeURIComponent(token)}`
}

export function parseOrderSocketMessage(raw: unknown): OrderSocketMessage | null {
  try {
    const value = typeof raw === 'string' ? JSON.parse(raw) as Record<string, unknown> : raw as Record<string, unknown>
    if (!value || (value.type !== 1 && value.type !== 2) || typeof value.orderId !== 'number') return null
    const message: OrderSocketMessage = { type: value.type, orderId: value.orderId }
    if (typeof value.content === 'string') message.content = value.content
    if (typeof value.status === 'number' && value.status >= 1 && value.status <= 6) {
      message.status = value.status as OrderSocketMessage['status']
    }
    return message
  } catch {
    return null
  }
}

export function orderSocketFingerprint(message: OrderSocketMessage) {
  return [message.type, message.orderId, message.status ?? '', message.content ?? ''].join(':')
}

export class OrderSocketClient {
  private socket: SocketLike | null = null
  private retryTimer: ReturnType<typeof setTimeout> | null = null
  private degradedTimer: ReturnType<typeof setTimeout> | null = null
  private stopped = true
  private authFailed = false
  private state = initialState()
  private readonly seen = new Map<string, number>()

  constructor(private readonly options: OrderSocketClientOptions) {}

  start() {
    if (!this.options.token || this.options.sid === '') return false
    this.stopped = false
    this.authFailed = false
    this.clearTimers()
    this.state = initialState()
    this.open(false)
    return true
  }

  resume() {
    return this.connectNow()
  }

  retryNow() {
    if (this.stopped || this.authFailed) return false
    if (this.isActive()) {
      if (this.state.status !== 'degraded') return false
      const stale = this.socket
      this.socket = null
      stale?.close(NORMAL_CLOSE_CODE, 'manual retry')
    }
    return this.connectNow()
  }

  stop() {
    this.stopped = true
    this.clearTimers()
    const current = this.socket
    this.socket = null
    if (current && (current.readyState === SOCKET_CONNECTING || current.readyState === SOCKET_OPEN)) {
      current.close(NORMAL_CLOSE_CODE, 'client closed')
    }
    this.emit('closed', { waitingForRetry: false, lastCloseCode: NORMAL_CLOSE_CODE })
  }

  getState(): OrderSocketConnectionState {
    return { ...this.state }
  }

  private connectNow() {
    if (this.stopped || this.authFailed || this.isActive()) return false
    this.clearRetryTimer()
    this.open(this.state.retryCount > 0 || this.state.status === 'degraded')
    return true
  }

  private open(reconnecting: boolean) {
    if (this.stopped || this.authFailed || this.isActive()) return
    const status: OrderSocketStatus = this.state.status === 'degraded'
      ? 'degraded'
      : reconnecting ? 'reconnecting' : 'connecting'
    this.emit(status, { waitingForRetry: false })

    try {
      const origin = this.options.origin ?? window.location.origin
      const target = buildOrderSocketUrl(origin, this.options.sid, this.options.token)
      const socket = (this.options.createSocket ?? ((url) => new WebSocket(url)))(target)
      this.socket = socket

      socket.onopen = () => {
        if (this.socket !== socket || this.stopped) return
        this.clearTimers()
        this.emit('connected', { retryCount: 0, outageStartedAt: null, waitingForRetry: false })
      }
      socket.onmessage = (event) => {
        if (this.socket !== socket || this.stopped) return
        const message = parseOrderSocketMessage(event.data)
        if (!message || this.isDuplicate(message)) return
        this.options.onMessage(message)
      }
      socket.onerror = () => {
        if (this.socket === socket && !this.stopped && socket.readyState < 2) socket.close()
      }
      socket.onclose = (event) => {
        if (this.socket !== socket) return
        this.socket = null
        if (this.stopped) return
        this.handleClose(event.code)
      }
    } catch {
      this.socket = null
      this.handleClose(SERVER_ERROR_CODE)
    }
  }

  private handleClose(closeCode: number) {
    const disconnectedAt = this.currentTime()
    if (AUTH_FAILURE_CODES.has(closeCode)) {
      this.authFailed = true
      this.clearTimers()
      this.emit('auth-failed', {
        lastDisconnectedAt: disconnectedAt,
        outageStartedAt: this.state.outageStartedAt ?? disconnectedAt,
        lastCloseCode: closeCode,
        waitingForRetry: false,
      })
      void this.options.onAuthFailure?.()
      return
    }

    if (closeCode === NORMAL_CLOSE_CODE) {
      this.stopped = true
      this.clearTimers()
      this.emit('closed', {
        lastDisconnectedAt: disconnectedAt,
        lastCloseCode: closeCode,
        waitingForRetry: false,
      })
      return
    }

    const retryCount = this.state.retryCount + 1
    const outageStartedAt = this.state.outageStartedAt ?? disconnectedAt
    const degraded = closeCode === SERVER_ERROR_CODE
      || retryCount >= (this.options.degradedAfterAttempts ?? DEFAULT_DEGRADED_AFTER_ATTEMPTS)
      || disconnectedAt - outageStartedAt >= (this.options.degradedAfterMs ?? DEFAULT_DEGRADED_AFTER_MS)

    this.emit(degraded ? 'degraded' : 'reconnecting', {
      retryCount,
      lastDisconnectedAt: disconnectedAt,
      outageStartedAt,
      lastCloseCode: closeCode,
      waitingForRetry: true,
    })
    this.ensureDegradedTimer()
    this.scheduleReconnect(degraded)
  }

  private scheduleReconnect(degraded: boolean) {
    if (this.stopped || this.authFailed || this.retryTimer) return
    const base = this.options.baseRetryDelay ?? 1_000
    const max = this.options.maxRetryDelay ?? 30_000
    const delay = degraded
      ? (this.options.degradedRetryDelay ?? max)
      : Math.min(max, base * 2 ** Math.max(0, this.state.retryCount - 1))
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null
      this.emit(this.state.status, { waitingForRetry: false })
      this.open(true)
    }, delay)
  }

  private ensureDegradedTimer() {
    if (this.degradedTimer || this.state.status === 'degraded' || this.state.outageStartedAt === null) return
    const threshold = this.options.degradedAfterMs ?? DEFAULT_DEGRADED_AFTER_MS
    const remaining = Math.max(0, threshold - (this.currentTime() - this.state.outageStartedAt))
    this.degradedTimer = setTimeout(() => {
      this.degradedTimer = null
      if (this.stopped || this.authFailed || this.state.status === 'connected') return
      this.emit('degraded')
    }, remaining)
  }

  private isDuplicate(message: OrderSocketMessage) {
    const now = this.currentTime()
    const ttl = this.options.dedupTtl ?? DEFAULT_DEDUP_TTL
    const fingerprint = orderSocketFingerprint(message)
    const previous = this.seen.get(fingerprint)
    if (previous !== undefined && now - previous <= ttl) return true

    this.seen.set(fingerprint, now)
    for (const [key, timestamp] of this.seen) {
      if (now - timestamp > ttl || this.seen.size > DEFAULT_DEDUP_LIMIT) this.seen.delete(key)
    }
    return false
  }

  private isActive() {
    return this.socket?.readyState === SOCKET_CONNECTING || this.socket?.readyState === SOCKET_OPEN
  }

  private emit(status: OrderSocketStatus, changes: Partial<OrderSocketConnectionState> = {}) {
    this.state = { ...this.state, ...changes, status }
    this.options.onStateChange?.({ ...this.state })
  }

  private clearRetryTimer() {
    if (this.retryTimer) clearTimeout(this.retryTimer)
    this.retryTimer = null
  }

  private clearTimers() {
    this.clearRetryTimer()
    if (this.degradedTimer) clearTimeout(this.degradedTimer)
    this.degradedTimer = null
  }

  private currentTime() {
    return (this.options.now ?? Date.now)()
  }
}
