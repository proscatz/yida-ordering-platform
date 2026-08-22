<template>
  <el-dialog :model-value="modelValue" title="选择套餐菜品" width="min(780px, calc(100vw - 30px))" align-center @update:model-value="$emit('update:modelValue', $event)" @opened="load">
    <div class="dish-picker__filters">
      <el-input v-model.trim="query.name" placeholder="搜索菜品名称" clearable :prefix-icon="Search" @keyup.enter="search" />
      <el-select v-model="query.categoryId" placeholder="全部菜品分类" clearable><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <PageErrorAlert v-if="error" :message="error" @retry="loadDishes" />
    <el-table v-loading="loading" :data="records" height="360" row-key="id">
      <el-table-column label="菜品" min-width="220"><template #default="{ row }: { row: Dish }"><div class="picker-dish"><span class="picker-dish__image"><ProductThumbnail :src="row.image" :name="row.name" /></span><div><strong>{{ row.name }}</strong><small>{{ row.categoryName }}</small></div></div></template></el-table-column>
      <el-table-column label="价格" width="110"><template #default="{ row }: { row: Dish }">{{ formatMoney(row.price) }}</template></el-table-column>
      <el-table-column label="操作" width="100" align="right"><template #default="{ row }: { row: Dish }"><el-button link type="primary" :disabled="selectedIds.includes(row.id)" @click="$emit('select', row)">{{ selectedIds.includes(row.id) ? '已选择' : '选择' }}</el-button></template></el-table-column>
      <template #empty><EmptyState title="未找到可用菜品" description="请调整名称或分类条件。" /></template>
    </el-table>
    <DataPagination v-model:page="query.page" v-model:page-size="query.pageSize" :total="total" @change="loadDishes" />
    <template #footer><el-button @click="$emit('update:modelValue', false)">完成选择</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import DataPagination from './DataPagination.vue'
import EmptyState from './EmptyState.vue'
import PageErrorAlert from './PageErrorAlert.vue'
import ProductThumbnail from './ProductThumbnail.vue'
import { categoryApi } from '@/api/categories'
import { dishApi } from '@/api/dishes'
import { userFacingError } from '@/api/errors'
import { formatMoney } from '@/utils/format'
import type { Dish } from '@/types/catalog'
import type { Category } from '@/types/management'

defineProps<{ modelValue: boolean; selectedIds: number[] }>()
defineEmits<{ 'update:modelValue': [value: boolean]; select: [dish: Dish] }>()
const loading = ref(false)
const error = ref('')
const records = ref<Dish[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const query = reactive({ name: '', categoryId: undefined as number | undefined, page: 1, pageSize: 10 })

async function load() {
  if (!categories.value.length) {
    try { categories.value = await categoryApi.list(1) } catch { categories.value = [] }
  }
  await loadDishes()
}

async function loadDishes() {
  loading.value = true
  error.value = ''
  try {
    const result = await dishApi.page({ page: query.page, pageSize: query.pageSize, name: query.name || undefined, categoryId: query.categoryId, status: 1 })
    records.value = result.records
    total.value = result.total
  } catch (loadError) {
    error.value = userFacingError(loadError, '可用菜品加载失败')
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; void loadDishes() }
</script>

<style scoped>
.dish-picker__filters { margin-bottom: 15px; display: grid; grid-template-columns: 1fr 200px auto; gap: 10px; }
.picker-dish { display: flex; align-items: center; gap: 10px; }
.picker-dish__image { width:44px; height:44px; flex:0 0 44px; overflow:hidden; border-radius:11px; }
.picker-dish__image :deep(img) { width:100%; height:100%; object-fit:cover; }
.picker-dish strong,.picker-dish small { display: block; }
.picker-dish small { margin-top: 3px; color: var(--muted); font-size: 10px; }
@media (max-width: 620px) { .dish-picker__filters { grid-template-columns: 1fr; } }
</style>
