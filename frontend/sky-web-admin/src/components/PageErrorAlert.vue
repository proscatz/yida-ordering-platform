<template>
  <el-alert class="page-error-alert" :title="message" type="error" show-icon :closable="false">
    <template #default><el-button link type="primary" @click="$emit('retry')">重新加载</el-button></template>
  </el-alert>
</template>

<script setup lang="ts">
defineProps<{ message: string }>()
defineEmits<{ retry: [] }>()
</script>

<style scoped>
.page-error-alert { margin-bottom: 16px; border-radius: 14px; }
</style>
