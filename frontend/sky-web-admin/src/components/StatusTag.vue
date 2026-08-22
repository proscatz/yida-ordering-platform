<template>
  <span class="status-tag" :class="enabled ? 'status-tag--enabled' : 'status-tag--disabled'">
    <i />{{ enabled ? enabledText : disabledText }}
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ enabled: boolean; enabledText?: string; disabledText?: string }>(), {
  enabledText: '已启用',
  disabledText: '已禁用',
})
</script>

<style scoped>
.status-tag { width: fit-content; padding: 5px 10px; display: inline-flex; align-items: center; gap: 6px; border-radius: 999px; font-size: 11px; font-weight: 750; }
.status-tag i { width: 6px; height: 6px; border-radius: 50%; }
.status-tag--enabled { color: #087165; background: #e2f5ef; }
.status-tag--enabled i { background: #13a18f; }
.status-tag--disabled { color: #7c8986; background: #eef1f0; }
.status-tag--disabled i { background: #9ba6a3; }
</style>
