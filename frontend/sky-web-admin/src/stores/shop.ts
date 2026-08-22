import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { shopApi } from '@/api/shop'
import { withWriteLock } from '@/utils/writeLock'
import type { EnableStatus } from '@/types/management'

export const useShopStore = defineStore('shop', () => {
  const status = ref<EnableStatus | null>(null)
  const loading = ref(false)
  const changing = ref(false)
  const isOpen = computed(() => status.value === 1)

  async function refreshStatus() {
    loading.value = true
    try {
      status.value = await shopApi.getStatus()
      return status.value
    } finally {
      loading.value = false
    }
  }

  async function changeStatus(nextStatus: EnableStatus) {
    changing.value = true
    try {
      await withWriteLock('shop:status', () => shopApi.setStatus(nextStatus))
      return await refreshStatus()
    } finally {
      changing.value = false
    }
  }

  return { status, loading, changing, isOpen, refreshStatus, changeStatus }
})
