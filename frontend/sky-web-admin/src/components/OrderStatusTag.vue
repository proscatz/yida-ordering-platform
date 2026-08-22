<template>
  <span class="order-status" :class="`order-status--${meta.tone}`"><i />{{ meta.label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ORDER_STATUS_META } from '@/utils/orderState'
import type { OrderStatus } from '@/types/orders'

const props = defineProps<{ status: OrderStatus }>()
const meta = computed(() => ORDER_STATUS_META[props.status])
</script>

<style scoped>
.order-status { width: fit-content; padding: 5px 10px; display: inline-flex; align-items: center; gap: 6px; border-radius: 999px; font-size: 11px; font-weight: 750; white-space: nowrap; }
.order-status i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.order-status--info { color: #667472; background: #eef1f0; }
.order-status--warning { color: #a65c21; background: #fff0e4; }
.order-status--primary { color: #087165; background: #e2f5ef; }
.order-status--success { color: #267443; background: #e8f6ec; }
.order-status--danger { color: #b43d47; background: #ffeaec; }
</style>
