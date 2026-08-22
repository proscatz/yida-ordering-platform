<template>
  <div class="empty-state">
    <div class="empty-state__icon"><Box /></div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'

withDefaults(defineProps<{ title?: string; description?: string }>(), {
  title: '这里还没有内容',
  description: '相关功能将在后续阶段逐步完善。',
})
</script>
