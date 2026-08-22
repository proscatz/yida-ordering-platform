<template>
  <div class="content-page">
    <header class="page-heading">
      <div><span class="eyebrow">商品中心</span><h1>菜品管理</h1><p>维护菜品信息、价格、图片、规格与售卖状态。</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增菜品</el-button>
    </header>

    <FilterToolbar>
      <el-form inline @submit.prevent="search">
        <el-form-item label="菜品名称"><el-input v-model.trim="query.name" placeholder="输入名称查询" clearable maxlength="30" @keyup.enter="search" /></el-form-item>
        <el-form-item label="菜品分类"><el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 160px"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select></el-form-item>
        <el-form-item label="售卖状态"><el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px"><el-option label="在售" :value="1" /><el-option label="停售" :value="0" /></el-select></el-form-item>
      </el-form>
      <template #actions><el-button :icon="Refresh" @click="resetFilters">重置</el-button><el-button type="primary" :icon="Search" @click="search">查询</el-button></template>
    </FilterToolbar>

    <PageErrorAlert v-if="error" :message="error" @retry="loadDishes" />
    <section class="surface-card data-card">
      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column label="菜品" min-width="220"><template #default="{ row }: { row: Dish }"><div class="product-cell"><span class="product-cell__image"><ProductThumbnail :src="row.image" :name="row.name" /></span><div><strong>{{ row.name }}</strong><small>{{ row.categoryName }}</small></div></div></template></el-table-column>
        <el-table-column label="价格" width="120"><template #default="{ row }: { row: Dish }"><strong class="money-cell">{{ formatMoney(row.price) }}</strong></template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="210" show-overflow-tooltip />
        <el-table-column label="状态" width="100"><template #default="{ row }: { row: Dish }"><StatusTag :enabled="row.status === 1" enabled-text="在售" disabled-text="停售" /></template></el-table-column>
        <el-table-column label="更新时间" min-width="165"><template #default="{ row }: { row: Dish }">{{ formatDateTime(row.updateTime) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right"><template #default="{ row }: { row: Dish }"><div class="table-actions"><el-button link type="primary" :disabled="rowActionId === row.id" @click="openEdit(row)">编辑</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" :disabled="rowActionId === row.id" @click="prepareAction('status', row)">{{ row.status === 1 ? '停售' : '启售' }}</el-button><el-button link type="danger" :disabled="rowActionId === row.id" @click="prepareAction('delete', row)">删除</el-button></div></template></el-table-column>
        <template #empty><EmptyState title="未找到菜品" description="调整筛选条件，或新增一道菜品。" /></template>
      </el-table>
      <DataPagination v-model:page="query.page" v-model:page-size="query.pageSize" :total="total" @change="loadDishes" />
    </section>

    <EntityFormDrawer
      v-model="drawerOpen"
      size="min(720px, 100vw)"
      :title="editingId ? '编辑菜品' : '新增菜品'"
      :submitting="submitting"
      :submit-text="editingId ? '保存修改' : '创建菜品'"
      :before-close="confirmDiscard"
      @submit="submitDish"
    >
      <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submitDish">
        <div class="form-grid">
          <el-form-item label="菜品名称" prop="name"><el-input v-model.trim="form.name" maxlength="30" placeholder="请输入菜品名称" /></el-form-item>
          <el-form-item label="菜品分类" prop="categoryId"><el-select v-model="form.categoryId" placeholder="请选择菜品分类" style="width:100%"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select></el-form-item>
          <el-form-item label="菜品价格" prop="price"><el-input-number v-model="form.price" :min="0.01" :max="99999" :precision="2" :step="1" controls-position="right" style="width:100%" /></el-form-item>
          <el-form-item class="form-grid__full" label="菜品描述" prop="description"><el-input v-model.trim="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入菜品描述" /></el-form-item>
          <el-form-item class="form-grid__full" label="菜品图片" prop="image"><ImageUploader v-model="form.image" /></el-form-item>
          <el-form-item class="form-grid__full"><DishFlavorEditor v-model="flavorDrafts" /></el-form-item>
        </div>
      </el-form>
    </EntityFormDrawer>

    <ConfirmActionDialog v-model="actionDialogOpen" :title="actionTitle" :description="actionDescription" :confirm-text="actionKind === 'delete' ? '确认删除' : actionTarget?.status === 1 ? '确认停售' : '确认启售'" :tone="actionKind === 'delete' || actionTarget?.status === 1 ? 'danger' : 'primary'" :loading="actionSubmitting" @confirm="executeAction" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import ConfirmActionDialog from '@/components/ConfirmActionDialog.vue'
import DataPagination from '@/components/DataPagination.vue'
import DishFlavorEditor from '@/components/DishFlavorEditor.vue'
import EmptyState from '@/components/EmptyState.vue'
import EntityFormDrawer from '@/components/EntityFormDrawer.vue'
import FilterToolbar from '@/components/FilterToolbar.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import ProductThumbnail from '@/components/ProductThumbnail.vue'
import StatusTag from '@/components/StatusTag.vue'
import { categoryApi } from '@/api/categories'
import { dishApi } from '@/api/dishes'
import { userFacingError } from '@/api/errors'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'
import { parseFlavorRecords, serializeFlavorDrafts, validateFlavorDrafts } from '@/utils/catalog'
import { formatDateTime, formatMoney } from '@/utils/format'
import { withWriteLock } from '@/utils/writeLock'
import type { Dish, DishPageQuery, DishPayload, FlavorDraft } from '@/types/catalog'
import type { Category, EnableStatus } from '@/types/management'

type ActionKind = 'delete' | 'status'
const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const actionSubmitting = ref(false)
const error = ref('')
const records = ref<Dish[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const drawerOpen = ref(false)
const editingId = ref<number>()
const rowActionId = ref<number>()
const actionDialogOpen = ref(false)
const actionKind = ref<ActionKind>('status')
const actionTarget = ref<Dish>()
const formRef = ref<FormInstance>()
const query = reactive<DishPageQuery>({ page: 1, pageSize: 10, name: '', categoryId: undefined, status: undefined })
const form = reactive<DishPayload>({ name: '', categoryId: null, price: 0.01, image: '', description: '', status: 0, flavors: [] })
const flavorDrafts = ref<FlavorDraft[]>([])
const cleanSnapshot = ref('')
const currentSnapshot = computed(() => JSON.stringify({ form, flavors: flavorDrafts.value }))
const isDirty = computed(() => drawerOpen.value && !detailLoading.value && currentSnapshot.value !== cleanSnapshot.value)
const { confirmDiscard } = useUnsavedGuard(isDirty)
const rules: FormRules<DishPayload> = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择菜品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入菜品价格', trigger: 'blur' }],
  description: [{ required: true, message: '请输入菜品描述', trigger: 'blur' }],
}
const actionTitle = computed(() => actionKind.value === 'delete' ? '确认删除菜品？' : actionTarget.value?.status === 1 ? '确认停售菜品？' : '确认启售菜品？')
const actionDescription = computed(() => {
  const name = actionTarget.value?.name || '该菜品'
  if (actionKind.value === 'delete') return `即将删除“${name}”。在售或已被套餐关联的菜品将由服务端拒绝删除。`
  return actionTarget.value?.status === 1 ? `停售“${name}”后，包含它的套餐也可能同步停售。` : `启售后，“${name}”将在用户端对应分类中可见。`
})

function setCleanSnapshot() { cleanSnapshot.value = currentSnapshot.value }
function resetForm() {
  Object.assign(form, { id: undefined, name: '', categoryId: null, price: 0.01, image: '', description: '', status: 0, flavors: [] })
  flavorDrafts.value = []
  formRef.value?.clearValidate()
  setCleanSnapshot()
}

async function loadCategories() {
  try { categories.value = await categoryApi.list(1) } catch (categoryError) { ElMessage.error(userFacingError(categoryError, '菜品分类加载失败')) }
}

async function loadDishes() {
  loading.value = true
  error.value = ''
  try {
    const result = await dishApi.page({ ...query, name: query.name || undefined })
    records.value = result.records
    total.value = result.total
  } catch (loadError) { error.value = userFacingError(loadError, '菜品列表加载失败') }
  finally { loading.value = false }
}

function search() { query.page = 1; void loadDishes() }
function resetFilters() { Object.assign(query, { page: 1, name: '', categoryId: undefined, status: undefined }); void loadDishes() }
function openCreate() { editingId.value = undefined; resetForm(); drawerOpen.value = true; setCleanSnapshot() }

async function openEdit(row: Dish) {
  editingId.value = row.id
  resetForm()
  drawerOpen.value = true
  detailLoading.value = true
  rowActionId.value = row.id
  try {
    const detail = await dishApi.detail(row.id)
    Object.assign(form, { id: detail.id, name: detail.name, categoryId: detail.categoryId, price: detail.price, image: detail.image ?? '', description: detail.description ?? '', status: detail.status, flavors: [] })
    flavorDrafts.value = parseFlavorRecords(detail.flavors)
    setCleanSnapshot()
  } catch (detailError) {
    drawerOpen.value = false
    ElMessage.error(userFacingError(detailError, '菜品详情加载失败'))
  } finally { detailLoading.value = false; rowActionId.value = undefined }
}

async function submitDish() {
  if (submitting.value || !(await formRef.value?.validate().catch(() => false))) return
  const flavorError = validateFlavorDrafts(flavorDrafts.value)
  if (flavorError) { ElMessage.error(flavorError); return }
  if (!editingId.value && !form.image) { ElMessage.error('请上传菜品图片'); return }
  submitting.value = true
  const payload: DishPayload = { ...form, flavors: serializeFlavorDrafts(flavorDrafts.value) }
  try {
    await withWriteLock(`dish:save:${editingId.value ?? 'new'}`, () => editingId.value ? dishApi.update(payload) : dishApi.create(payload))
    setCleanSnapshot()
    drawerOpen.value = false
    if (!editingId.value) query.page = 1
    ElMessage.success(editingId.value ? '菜品已更新' : '菜品创建成功')
    await loadDishes()
  } catch (submitError) { ElMessage.error(userFacingError(submitError, '菜品保存失败')) }
  finally { submitting.value = false }
}

function prepareAction(kind: ActionKind, row: Dish) { actionKind.value = kind; actionTarget.value = row; actionDialogOpen.value = true }
async function executeAction() {
  const target = actionTarget.value
  if (!target) return
  actionSubmitting.value = true
  rowActionId.value = target.id
  try {
    if (actionKind.value === 'delete') {
      await withWriteLock(`dish:delete:${target.id}`, () => dishApi.remove([target.id]))
      ElMessage.success('菜品已删除')
    } else {
      const status: EnableStatus = target.status === 1 ? 0 : 1
      await withWriteLock(`dish:status:${target.id}`, () => dishApi.setStatus(target.id, status))
      ElMessage.success(status === 1 ? '菜品已启售' : '菜品已停售')
    }
    actionDialogOpen.value = false
    await loadDishes()
  } catch (actionError) { ElMessage.error(userFacingError(actionError, actionKind.value === 'delete' ? '菜品删除失败' : '菜品状态更新失败')) }
  finally { actionSubmitting.value = false; rowActionId.value = undefined }
}

onMounted(async () => { await Promise.all([loadCategories(), loadDishes()]) })
</script>

<style scoped>
.data-card { padding: 8px 20px 0; overflow: hidden; }
.product-cell { display: flex; align-items: center; gap: 11px; }
.product-cell__image { width: 48px; height: 48px; flex: 0 0 48px; overflow:hidden; border-radius:13px; }
.product-cell__image :deep(img) { width:100%; height:100%; object-fit:cover; }
.product-cell strong,.product-cell small { display: block; }
.product-cell small { margin-top: 3px; color: var(--muted); font-size: 10px; }
.money-cell { color: #d26631; }
.table-actions { display: flex; align-items: center; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 15px; }
.form-grid__full { grid-column: 1 / -1; }
@media (max-width: 720px) { .page-heading { align-items: stretch; flex-direction: column; } .page-heading :deep(.el-button) { width: 100%; } .data-card { padding-inline: 12px; } .form-grid { grid-template-columns: 1fr; } .form-grid__full { grid-column: auto; } }
</style>
