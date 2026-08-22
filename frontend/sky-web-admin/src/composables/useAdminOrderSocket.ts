import { onBeforeUnmount, onMounted, watch } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { handleUnauthorized } from '@/api/unauthorized'
import { OrderSocketClient } from '@/services/orderSocket'
import { useAuthStore } from '@/stores/auth'
import { useOrderRealtimeStore } from '@/stores/orderRealtime'
import type { OrderSocketMessage, OrderSocketStatus } from '@/types/orders'

export function useAdminOrderSocket() {
  const authStore = useAuthStore()
  const realtimeStore = useOrderRealtimeStore()
  let client: OrderSocketClient | null = null
  let userActivated = false
  let previousStatus: OrderSocketStatus = 'idle'

  function markUserActivated() {
    userActivated = true
  }

  function playNotificationTone() {
    if (!userActivated || typeof AudioContext === 'undefined') return
    try {
      const context = new AudioContext()
      const oscillator = context.createOscillator()
      const gain = context.createGain()
      oscillator.frequency.value = 740
      gain.gain.setValueAtTime(0.0001, context.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.08, context.currentTime + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, context.currentTime + 0.18)
      oscillator.connect(gain).connect(context.destination)
      oscillator.start()
      oscillator.stop(context.currentTime + 0.2)
      oscillator.addEventListener('ended', () => void context.close(), { once: true })
    } catch {
      // 浏览器拒绝自动播放时保留站内通知，不输出连接或认证信息。
    }
  }

  function showNotice(message: OrderSocketMessage) {
    const isNewOrder = message.type === 1
    ElNotification({
      title: isNewOrder ? '收到新订单' : '收到订单催单',
      message: message.content || `订单 ID：${message.orderId}`,
      type: isNewOrder ? 'success' : 'warning',
      duration: 6_000,
    })
    playNotificationTone()
    realtimeStore.accept(message)
  }

  function stop() {
    client?.stop()
    client = null
    realtimeStore.registerRetryHandler(null)
  }

  function start() {
    stop()
    const session = authStore.session
    if (!session?.token || session.id == null) return
    const socketOrigin = import.meta.env.VITE_WS_ORIGIN
      || (import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin)
    client = new OrderSocketClient({
      sid: session.id,
      token: session.token,
      origin: socketOrigin,
      onMessage: showNotice,
      onStateChange: (state) => {
        const recovered = state.status === 'connected'
          && (previousStatus === 'reconnecting' || previousStatus === 'degraded')
        previousStatus = state.status
        realtimeStore.setConnectionState(state)
        if (recovered) ElMessage.success('实时订单提醒已恢复')
      },
      onAuthFailure: handleUnauthorized,
    })
    realtimeStore.registerRetryHandler(() => client?.retryNow() ?? false)
    client.start()
  }

  function resumeWhenVisible() {
    if (document.visibilityState === 'visible') client?.resume()
  }

  onMounted(() => {
    window.addEventListener('pointerdown', markUserActivated, { once: true })
    window.addEventListener('keydown', markUserActivated, { once: true })
    window.addEventListener('online', resumeWhenVisible)
    document.addEventListener('visibilitychange', resumeWhenVisible)
    start()
  })

  watch(() => `${authStore.session?.id ?? ''}:${authStore.session?.token ?? ''}`, start)

  onBeforeUnmount(() => {
    window.removeEventListener('pointerdown', markUserActivated)
    window.removeEventListener('keydown', markUserActivated)
    window.removeEventListener('online', resumeWhenVisible)
    document.removeEventListener('visibilitychange', resumeWhenVisible)
    stop()
  })

  return { reconnect: () => client?.resume() ?? false }
}
