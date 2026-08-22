<template>
  <img v-if="src && !failed" class="product-image" :src="src" :alt="alt" loading="lazy" @error="failed = true" />
  <div v-else class="product-placeholder" :aria-label="alt">{{ alt.slice(0, 1) || '驿' }}</div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
const props = defineProps<{ src?: string; alt: string }>()
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>