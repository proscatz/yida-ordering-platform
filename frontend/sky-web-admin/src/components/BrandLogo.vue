<template>
  <div class="brand-logo" :class="{ 'brand-logo--compact': compact }">
    <img class="brand-logo__mark" src="/logo.svg" alt="驿达点餐 Logo" />
    <span v-if="!compact" class="brand-logo__copy">
      <strong class="brand-logo__name">驿达点餐</strong>
      <small v-if="showTagline" class="brand-logo__tagline">预约点餐与履约管理平台</small>
    </span>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ compact?: boolean; showTagline?: boolean }>(), {
  compact: false,
  showTagline: true,
})
</script>
