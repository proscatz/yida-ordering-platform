<template>
  <section class="filter-toolbar">
    <div class="filter-toolbar__fields"><slot /></div>
    <div class="filter-toolbar__actions"><slot name="actions" /></div>
  </section>
</template>

<style scoped>
.filter-toolbar { margin-bottom: 18px; padding: 17px 18px; display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; border: 1px solid rgba(255,255,255,.9); border-radius: var(--radius); background: var(--surface); box-shadow: 0 8px 26px rgba(23,56,54,.05); }
.filter-toolbar__fields { flex: 1; display: flex; align-items: flex-end; flex-wrap: wrap; gap: 12px; }
.filter-toolbar__actions { display: flex; align-items: center; gap: 10px; }
:deep(.el-form-item) { margin: 0; }
@media (max-width: 720px) {
  .filter-toolbar { align-items: stretch; flex-direction: column; }
  .filter-toolbar__fields, .filter-toolbar__actions { width: 100%; }
  .filter-toolbar__actions :deep(.el-button) { flex: 1; }
  :deep(.el-form), :deep(.el-form-item), :deep(.el-input), :deep(.el-select) { width: 100%; }
}
</style>
