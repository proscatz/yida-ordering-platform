<template>
  <div class="brand-logo">
    <img src="/logo.svg" alt="驿达点餐 Logo" />
    <span>
      <span class="brand-logo__name">驿达点餐</span>
      <span v-if="showTagline" class="brand-logo__tagline">预约点餐 · 准时履约</span>
    </span>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ showTagline?: boolean }>(), { showTagline: true })
</script>