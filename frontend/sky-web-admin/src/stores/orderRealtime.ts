import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import type { OrderSocketConnectionState, OrderSocketMessage } from '@/types/orders'

const initialConnectionState = (): OrderSocketConnectionState => ({
  status: 'idle', retryCount: 0, lastDisconnectedAt: null,
  outageStartedAt: null, lastCloseCode: null, waitingForRetry: false,
})

export const useOrderRealtimeStore = defineStore('order-realtime', () => {
  const revision = ref(0)
  const lastMessage = ref<OrderSocketMessage | null>(null)
  const connection = reactive<OrderSocketConnectionState>(initialConnectionState())
  const manualRetrying = ref(false)
  const connectionStatus = computed(() => connection.status)
  let retryHandler: (() => boolean) | null = null

  function accept(message: OrderSocketMessage) {
    lastMessage.value = message
    revision.value += 1
  }

  function setConnectionState(state: OrderSocketConnectionState) {
    Object.assign(connection, state)
    if (state.status === 'connected' || state.status === 'auth-failed' || state.status === 'closed' || state.waitingForRetry) {
      manualRetrying.value = false
    }
  }

  function registerRetryHandler(handler: (() => boolean) | null) {
    retryHandler = handler
  }

  function retryNow() {
    if (manualRetrying.value || !retryHandler) return false
    manualRetrying.value = true
    const started = retryHandler()
    if (!started) manualRetrying.value = false
    return started
  }

  function resetConnection() {
    Object.assign(connection, initialConnectionState())
    manualRetrying.value = false
  }

  return {
    revision, lastMessage, connection, connectionStatus, manualRetrying,
    accept, setConnectionState, registerRetryHandler, retryNow, resetConnection,
  }
})
