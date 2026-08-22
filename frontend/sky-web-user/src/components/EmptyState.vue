<template>
  <div class="empty-state">
    <div class="empty-state__icon"><van-icon :name="icon" /></div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </div>
</template>

<script setup lang="ts">
import { Icon as VanIcon } from 'vant'
withDefaults(defineProps<{ title: string; description: string; icon?: string }>(), { icon: 'notes-o' })
</script>