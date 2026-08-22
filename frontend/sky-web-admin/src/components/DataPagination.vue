<template>
  <div v-if="total > 0" class="data-pagination">
    <span class="data-pagination__summary">共 {{ total }} 条记录</span>
    <el-pagination
      background
      :current-page="page"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="sizes, prev, pager, next"
      @update:current-page="changePage"
      @update:page-size="changeSize"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{ page: number; pageSize: number; total: number }>()
const emit = defineEmits<{
  'update:page': [value: number]
  'update:pageSize': [value: number]
  change: []
}>()

function changePage(value: number) {
  emit('update:page', value)
  emit('change')
}

function changeSize(value: number) {
  emit('update:pageSize', value)
  emit('update:page', 1)
  emit('change')
}
</script>

<style scoped>
.data-pagination { min-height: 68px; padding: 16px 4px 0; display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.data-pagination__summary { color: var(--muted); font-size: 12px; }
@media (max-width: 720px) {
  .data-pagination { align-items: flex-start; flex-direction: column; overflow-x: auto; }
  :deep(.el-pagination__sizes) { display: none; }
}
</style>
