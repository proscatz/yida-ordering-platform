<template>
  <div class="content-page">
    <header class="page-heading">
      <div><span class="eyebrow">商品中心</span><h1>分类管理</h1><p>维护菜品与套餐的展示分类、排序和启停状态。</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
    </header>

    <FilterToolbar>
      <el-form inline @submit.prevent="search">
        <el-form-item label="分类名称"><el-input v-model.trim="query.name" placeholder="输入名称查询" clearable maxlength="30" @keyup.enter="search" /></el-form-item>
        <el-form-item label="分类类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="菜品分类" :value="1" /><el-option label="套餐分类" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </template>
    </FilterToolbar>

    <PageErrorAlert v-if="error" :message="error" @retry="loadCategories" />

    <section class="surface-card data-card">
      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column label="分类名称" min-width="190">
          <template #default="{ row }: { row: Category }"><div class="category-name"><span><Collection /></span><strong>{{ row.name }}</strong></div></template>
        </el-table-column>
        <el-table-column label="类型" min-width="110"><template #default="{ row }: { row: Category }"><span class="type-chip">{{ typeLabel(row.type) }}</span></template></el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="100"><template #default="{ row }: { row: Category }"><StatusTag :enabled="row.status === 1" /></template></el-table-column>
        <el-table-column label="更新时间" min-width="170"><template #default="{ row }: { row: Category }">{{ formatDateTime(row.updateTime) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }: { row: Category }">
            <div class="table-actions">
              <el-button link type="primary" :disabled="rowActionId === row.id" @click="openEdit(row)">编辑</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" :disabled="rowActionId === row.id" @click="prepareAction('status', row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
              <el-button link type="danger" :disabled="rowActionId === row.id" @click="prepareAction('delete', row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty><EmptyState title="未找到分类" description="调整筛选条件，或新增一个分类。" /></template>
      </el-table>
      <DataPagination v-model:page="query.page" v-model:page-size="query.pageSize" :total="total" @change="loadCategories" />
    </section>

    <EntityFormDrawer
      v-model="drawerOpen"
      :title="editingId ? '编辑分类' : '新增分类'"
      :submitting="submitting"
      :submit-text="editingId ? '保存修改' : '创建分类'"
      @submit="submitCategory"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submitCategory">
        <el-form-item label="分类类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择分类类型" style="width: 100%"><el-option label="菜品分类" :value="1" /><el-option label="套餐分类" :value="2" /></el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name"><el-input v-model.trim="form.name" maxlength="30" show-word-limit placeholder="请输入分类名称" /></el-form-item>
        <el-form-item label="显示排序" prop="sort"><el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" /></el-form-item>
        <el-alert title="数字越小越靠前；新分类创建后默认为禁用状态。" type="info" :closable="false" show-icon />
      </el-form>
    </EntityFormDrawer>

    <ConfirmActionDialog
      v-model="actionDialogOpen"
      :title="actionDialogTitle"
      :description="actionDialogDescription"
      :confirm-text="actionKind === 'delete' ? '确认删除' : actionTarget?.status === 1 ? '确认禁用' : '确认启用'"
      :tone="actionKind === 'delete' || actionTarget?.status === 1 ? 'danger' : 'primary'"
      :loading="actionSubmitting"
      @confirm="executeAction"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Collection, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import ConfirmActionDialog from '@/components/ConfirmActionDialog.vue'
import DataPagination from '@/components/DataPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import EntityFormDrawer from '@/components/EntityFormDrawer.vue'
import FilterToolbar from '@/components/FilterToolbar.vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import { categoryApi } from '@/api/categories'
import { userFacingError } from '@/api/errors'
import { formatDateTime } from '@/utils/format'
import { withWriteLock } from '@/utils/writeLock'
import type { Category, CategoryPageQuery, CategoryPayload, CategoryType, EnableStatus } from '@/types/management'

type ActionKind = 'delete' | 'status'

const loading = ref(false)
const submitting = ref(false)
const actionSubmitting = ref(false)
const error = ref('')
const records = ref<Category[]>([])
const total = ref(0)
const drawerOpen = ref(false)
const editingId = ref<number>()
const rowActionId = ref<number>()
const actionDialogOpen = ref(false)
const actionKind = ref<ActionKind>('status')
const actionTarget = ref<Category>()
const formRef = ref<FormInstance>()
const query = reactive<CategoryPageQuery>({ name: '', type: undefined, page: 1, pageSize: 10 })
const form = reactive<CategoryPayload>({ type: 1, name: '', sort: 0 })
const rules: FormRules<CategoryPayload> = {
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }],
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 30, message: '分类名称应为2至30个字符', trigger: 'blur' },
  ],
  sort: [{ required: true, message: '请输入显示排序', trigger: 'blur' }],
}

const actionDialogTitle = computed(() => {
  if (actionKind.value === 'delete') return '确认删除分类？'
  return actionTarget.value?.status === 1 ? '确认禁用分类？' : '确认启用分类？'
})
const actionDialogDescription = computed(() => {
  const name = actionTarget.value?.name || '该分类'
  if (actionKind.value === 'delete') return `即将删除“${name}”。如果分类已关联菜品或套餐，服务端会拒绝本次操作。`
  return actionTarget.value?.status === 1 ? `禁用后，“${name}”将不再作为可用分类。` : `启用后，“${name}”将恢复为可用分类。`
})

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    const result = await categoryApi.page({ page: query.page, pageSize: query.pageSize, name: query.name || undefined, type: query.type })
    records.value = result.records
    total.value = result.total
    if (!records.value.length && query.page > 1 && total.value > 0) {
      query.page -= 1
      await loadCategories()
    }
  } catch (loadError) {
    error.value = userFacingError(loadError, '分类列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; void loadCategories() }
function resetFilters() { query.name = ''; query.type = undefined; query.page = 1; void loadCategories() }
function resetForm() { Object.assign(form, { id: undefined, type: 1, name: '', sort: 0 }); formRef.value?.clearValidate() }
function openCreate() { editingId.value = undefined; resetForm(); drawerOpen.value = true }
function openEdit(row: Category) { editingId.value = row.id; Object.assign(form, { id: row.id, type: row.type, name: row.name, sort: row.sort }); drawerOpen.value = true }

async function submitCategory() {
  if (submitting.value || !(await formRef.value?.validate().catch(() => false))) return
  submitting.value = true
  const payload = { ...form }
  try {
    await withWriteLock(`category:save:${editingId.value ?? 'new'}`, () => editingId.value ? categoryApi.update(payload) : categoryApi.create(payload))
    ElMessage.success(editingId.value ? '分类已更新' : '分类创建成功')
    drawerOpen.value = false
    if (!editingId.value) query.page = 1
    await loadCategories()
  } catch (submitError) {
    ElMessage.error(userFacingError(submitError, '分类保存失败'))
  } finally {
    submitting.value = false
  }
}

function prepareAction(kind: ActionKind, row: Category) { actionKind.value = kind; actionTarget.value = row; actionDialogOpen.value = true }

async function executeAction() {
  const target = actionTarget.value
  if (!target) return
  actionSubmitting.value = true
  rowActionId.value = target.id
  try {
    if (actionKind.value === 'delete') {
      await withWriteLock(`category:delete:${target.id}`, () => categoryApi.remove(target.id))
      ElMessage.success('分类已删除')
    } else {
      const nextStatus: EnableStatus = target.status === 1 ? 0 : 1
      await withWriteLock(`category:status:${target.id}`, () => categoryApi.setStatus(target.id, nextStatus))
      ElMessage.success(nextStatus === 1 ? '分类已启用' : '分类已禁用')
    }
    actionDialogOpen.value = false
    await loadCategories()
  } catch (actionError) {
    ElMessage.error(userFacingError(actionError, actionKind.value === 'delete' ? '分类删除失败' : '分类状态更新失败'))
  } finally {
    actionSubmitting.value = false
    rowActionId.value = undefined
  }
}

function typeLabel(type: CategoryType) { return type === 1 ? '菜品分类' : '套餐分类' }

onMounted(loadCategories)
</script>

<style scoped>
.data-card { padding: 8px 20px 0; overflow: hidden; }
.category-name { display: flex; align-items: center; gap: 11px; }
.category-name span { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 12px; color: var(--brand); background: var(--brand-soft); }
.category-name svg { width: 18px; }
.category-name strong { font-size: 13px; }
.type-chip { padding: 5px 9px; border-radius: 8px; color: #5f716e; background: #f0f4f2; font-size: 11px; }
.table-actions { display: flex; align-items: center; }
@media (max-width: 720px) {
  .page-heading { align-items: stretch; flex-direction: column; }
  .page-heading :deep(.el-button) { width: 100%; }
  .data-card { padding-inline: 12px; }
}
</style>
