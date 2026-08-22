import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cartApi } from '@/api/modules'
import type { CartItem, Dish, Setmeal } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)
  const count = computed(() => items.value.reduce((sum, item) => sum + item.number, 0))
  const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.amount) * item.number, 0))

  async function load(): Promise<void> {
    loading.value = true
    try { items.value = await cartApi.list() } finally { loading.value = false }
  }

  async function addDish(dish: Dish, dishFlavor?: string): Promise<void> {
    await cartApi.addDish(dish.id, dishFlavor)
    await load()
  }

  async function addSetmeal(setmeal: Setmeal): Promise<void> {
    await cartApi.addSetmeal(setmeal.id)
    await load()
  }

  async function increase(item: CartItem): Promise<void> {
    if (item.dishId) await cartApi.addDish(item.dishId, item.dishFlavor)
    else if (item.setmealId) await cartApi.addSetmeal(item.setmealId)
    await load()
  }

  async function decrease(item: CartItem): Promise<void> {
    await cartApi.sub({ dishId: item.dishId, setmealId: item.setmealId, dishFlavor: item.dishFlavor })
    await load()
  }

  async function clean(): Promise<void> {
    await cartApi.clean()
    items.value = []
  }

  function reset(): void { items.value = [] }

  return { items, loading, count, total, load, addDish, addSetmeal, increase, decrease, clean, reset }
})