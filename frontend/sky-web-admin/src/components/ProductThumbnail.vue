<template>
  <img v-if="src && !failed" :src="src" :alt="name" @error="failed = true" />
  <span v-else class="product-thumbnail-placeholder" role="img" :aria-label="`${name}暂无图片`">{{ name.slice(0, 1) }}</span>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{ src?: string | null; name: string }>()
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>

<style scoped>
.product-thumbnail-placeholder { width: 100%; height: 100%; display: grid; place-items: center; color: var(--brand); background: linear-gradient(145deg,var(--brand-soft),var(--accent-soft)); font-size: 17px; font-weight: 850; }
</style>
