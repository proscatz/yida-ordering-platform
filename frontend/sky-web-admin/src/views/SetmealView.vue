<template>
  <div class="content-page">
    <header class="page-heading">
      <div><span class="eyebrow">商品中心</span><h1>套餐管理</h1><p>组合可售菜品，配置套餐价格、图片和售卖状态。</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增套餐</el-button>
    </header>

    <FilterToolbar>
      <el-form inline @submit.prevent="search">
        <el-form-item label="套餐名称"><el-input v-model.trim="query.name" placeholder="输入名称查询" clearable maxlength="30" @keyup.enter="search" /></el-form-item>
        <el-form-item label="套餐分类"><el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width:160px"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select></el-form-item>
        <el-form-item label="售卖状态"><el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px"><el-option label="在售" :value="1" /><el-option label="停售" :value="0" /></el-select></el-form-item>
      </el-form>
      <template #actions><el-button :icon="Refresh" @click="resetFilters">重置</el-button><el-button type="primary" :icon="Search" @click="search">查询</el-button></template>
    </FilterToolbar>

    <PageErrorAlert v-if="error" :message="error" @retry="loadSetmeals" />
    <section class="surface-card data-card">
      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column label="套餐" min-width="220"><template #default="{ row }: { row: Setmeal }"><div class="product-cell"><span class="product-cell__image"><ProductThumbnail :src="row.image" :name="row.name" /></span><div><strong>{{ row.name }}</strong><small>{{ row.categoryName }}</small></div></div></template></el-table-column>
        <el-table-column label="套餐价" width="120"><template #default="{ row }: { row: Setmeal }"><strong class="money-cell">{{ formatMoney(row.price) }}</strong></template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="210" show-overflow-tooltip />
        <el-table-column label="状态" width="100"><template #default="{ row }: { row: Setmeal }"><StatusTag :enabled="row.status === 1" enabled-text="在售" disabled-text="停售" /></template></el-table-column>
        <el-table-column label="更新时间" min-width="165"><template #default="{ row }: { row: Setmeal }">{{ formatDateTime(row.updateTime) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right"><template #default="{ row }: { row: Setmeal }"><div class="table-actions"><el-button link type="primary" :disabled="rowActionId === row.id" @click="openEdit(row)">编辑</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" :disabled="rowActionId === row.id" @click="prepareAction('status', row)">{{ row.status === 1 ? '停售' : '启售' }}</el-button><el-button link type="danger" :disabled="rowActionId === row.id" @click="prepareAction('delete', row)">删除</el-button></div></template></el-table-column>
        <template #empty><EmptyState title="未找到套餐" description="调整筛选条件，或新增一个套餐。" /></template>
      </el-table>
      <DataPagination v-model:page="query.page" v-model:page-size="query.pageSize" :total="total" @change="loadSetmeals" />
    </section>

    <EntityFormDrawer v-model="drawerOpen" size="min(760px, 100vw)" :title="editingId ? '编辑套餐' : '新增套餐'" :submitting="submitting" :submit-text="editingId ? '保存修改' : '创建套餐'" :before-close="confirmDiscard" @submit="submitSetmeal">
      <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submitSetmeal">
        <div class="form-grid">
          <el-form-item label="套餐名称" prop="name"><el-input v-model.trim="form.name" maxlength="30" placeholder="请输入套餐名称" /></el-form-item>
          <el-form-item label="套餐分类" prop="categoryId"><el-select v-model="form.categoryId" placeholder="请选择套餐分类" style="width:100%"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select></el-form-item>
          <el-form-item label="套餐价格" prop="price"><el-input-number v-model="form.price" :min="0.01" :max="99999" :precision="2" controls-position="right" style="width:100%" /></el-form-item>
          <el-form-item class="form-grid__full" label="套餐描述" prop="description"><el-input v-model.trim="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入套餐描述" /></el-form-item>
          <el-form-item class="form-grid__full" label="套餐图片" prop="image"><ImageUploader v-model="form.image" /></el-form-item>
          <el-form-item class="form-grid__full">
            <div class="selected-dishes">
              <div class="selected-dishes__heading"><div><strong>套餐菜品</strong><span>菜品原价仅用于套餐配置参考，不参与订单结算计算</span></div><el-button :icon="Plus" plain @click="pickerOpen = true">选择菜品</el-button></div>
              <div v-if="!form.setmealDishes.length" class="selected-dishes__empty">尚未选择菜品，请至少添加一道在售菜品。</div>
              <div v-else class="selected-dishes__list">
                <article v-for="(dish, index) in form.setmealDishes" :key="dish.dishId">
                  <div><strong>{{ dish.name }}</strong><small>{{ formatMoney(dish.price) }} / 份</small></div>
                  <el-input-number v-model="dish.copies" :min="1" :max="99" size="small" aria-label="菜品份数" />
                  <strong>{{ formatMoney(dish.price * dish.copies) }}</strong>
                  <el-button circle plain type="danger" :icon="Delete" aria-label="移除菜品" @click="removeDish(index)" />
                </article>
              </div>
              <div class="selected-dishes__total"><span>共 {{ form.setmealDishes.length }} 道菜品</span><strong>菜品原价合计 {{ formatMoney(dishOriginalTotal) }}</strong></div>
            </div>
          </el-form-item>
        </div>
      </el-form>
    </EntityFormDrawer>

    <DishPickerDialog v-model="pickerOpen" :selected-ids="selectedDishIds" @select="addDish" />
    <ConfirmActionDialog v-model="actionDialogOpen" :title="actionTitle" :description="actionDescription" :confirm-text="actionKind === 'delete' ? '确认删除' : actionTarget?.status === 1 ? '确认停售' : '确认启售'" :tone="actionKind === 'delete' || actionTarget?.status === 1 ? 'danger' : 'primary'" :loading="actionSubmitting" @confirm="executeAction" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import ConfirmActionDialog from '@/components/ConfirmActionDialog.vue'
import DataPagination from '@/components/DataPagination.vue'
import DishPickerDialog from '@/components/DishPickerDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import EntityFormDrawer from '@/components/EntityFormDrawer.vue'
import FilterToolbar from '@/components/FilterToolbar.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import ProductThumbnail from '@/components/ProductThumbnail.vue'
import StatusTag from '@/components/StatusTag.vue'
import { categoryApi } from '@/api/categories'
import { userFacingError } from '@/api/errors'
import { setmealApi } from '@/api/setmeals'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'
import { normalizeSetmealDishes, validateSetmealDishes } from '@/utils/catalog'
import { formatDateTime, formatMoney } from '@/utils/format'
import { withWriteLock } from '@/utils/writeLock'
import type { Dish, Setmeal, SetmealPageQuery, SetmealPayload } from '@/types/catalog'
import type { Category, EnableStatus } from '@/types/management'

type ActionKind = 'delete' | 'status'
const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const actionSubmitting = ref(false)
const error = ref('')
const records = ref<Setmeal[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const drawerOpen = ref(false)
const pickerOpen = ref(false)
const editingId = ref<number>()
const rowActionId = ref<number>()
const actionDialogOpen = ref(false)
const actionKind = ref<ActionKind>('status')
const actionTarget = ref<Setmeal>()
const formRef = ref<FormInstance>()
const query = reactive<SetmealPageQuery>({ page: 1, pageSize: 10, name: '', categoryId: undefined, status: undefined })
const form = reactive<SetmealPayload>({ name: '', categoryId: null, price: 0.01, status: 0, description: '', image: '', setmealDishes: [] })
const cleanSnapshot = ref('')
const currentSnapshot = computed(() => JSON.stringify(form))
const isDirty = computed(() => drawerOpen.value && !detailLoading.value && currentSnapshot.value !== cleanSnapshot.value)
const { confirmDiscard } = useUnsavedGuard(isDirty)
const selectedDishIds = computed(() => form.setmealDishes.map((dish) => dish.dishId))
const dishOriginalTotal = computed(() => form.setmealDishes.reduce((totalValue, dish) => totalValue + Number(dish.price) * dish.copies, 0))
const rules: FormRules<SetmealPayload> = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择套餐分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }],
  description: [{ required: true, message: '请输入套餐描述', trigger: 'blur' }],
}
const actionTitle = computed(() => actionKind.value === 'delete' ? '确认删除套餐？' : actionTarget.value?.status === 1 ? '确认停售套餐？' : '确认启售套餐？')
const actionDescription = computed(() => {
  const name = actionTarget.value?.name || '该套餐'
  if (actionKind.value === 'delete') return `即将删除“${name}”。在售套餐将由服务端拒绝删除。`
  return actionTarget.value?.status === 1 ? `停售后，“${name}”将从用户端可售套餐中移除。` : `启售前服务端会检查套餐内菜品是否全部在售。`
})

function setCleanSnapshot() { cleanSnapshot.value = currentSnapshot.value }
function resetForm() { Object.assign(form, { id: undefined, name: '', categoryId: null, price: 0.01, status: 0, description: '', image: '', setmealDishes: [] }); formRef.value?.clearValidate(); setCleanSnapshot() }
async function loadCategories() { try { categories.value = await categoryApi.list(2) } catch (categoryError) { ElMessage.error(userFacingError(categoryError, '套餐分类加载失败')) } }
async function loadSetmeals() {
  loading.value = true
  error.value = ''
  try { const result = await setmealApi.page({ ...query, name: query.name || undefined }); records.value = result.records; total.value = result.total }
  catch (loadError) { error.value = userFacingError(loadError, '套餐列表加载失败') }
  finally { loading.value = false }
}
function search() { query.page = 1; void loadSetmeals() }
function resetFilters() { Object.assign(query, { page: 1, name: '', categoryId: undefined, status: undefined }); void loadSetmeals() }
function openCreate() { editingId.value = undefined; resetForm(); drawerOpen.value = true; setCleanSnapshot() }
async function openEdit(row: Setmeal) {
  editingId.value = row.id
  resetForm()
  drawerOpen.value = true
  detailLoading.value = true
  rowActionId.value = row.id
  try {
    const detail = await setmealApi.detail(row.id)
    Object.assign(form, {
      id: detail.id,
      name: detail.name,
      categoryId: detail.categoryId,
      price: detail.price,
      status: detail.status,
      description: detail.description ?? '',
      image: detail.image ?? '',
      setmealDishes: normalizeSetmealDishes(detail.setmealDishes),
    })
    setCleanSnapshot()
  } catch (detailError) { drawerOpen.value = false; ElMessage.error(userFacingError(detailError, '套餐详情加载失败')) }
  finally { detailLoading.value = false; rowActionId.value = undefined }
}
function addDish(dish: Dish) {
  if (selectedDishIds.value.includes(dish.id)) { ElMessage.warning('该菜品已经在套餐中'); return }
  form.setmealDishes.push({ dishId: dish.id, name: dish.name, price: dish.price, copies: 1 })
}
function removeDish(index: number) { form.setmealDishes.splice(index, 1) }
async function submitSetmeal() {
  if (submitting.value || !(await formRef.value?.validate().catch(() => false))) return
  const dishError = validateSetmealDishes(form.setmealDishes)
  if (dishError) { ElMessage.error(dishError); return }
  if (!editingId.value && !form.image) { ElMessage.error('请上传套餐图片'); return }
  submitting.value = true
  const payload: SetmealPayload = { ...form, setmealDishes: normalizeSetmealDishes(form.setmealDishes) }
  try {
    await withWriteLock(`setmeal:save:${editingId.value ?? 'new'}`, () => editingId.value ? setmealApi.update(payload) : setmealApi.create(payload))
    setCleanSnapshot()
    drawerOpen.value = false
    if (!editingId.value) query.page = 1
    ElMessage.success(editingId.value ? '套餐已更新' : '套餐创建成功')
    await loadSetmeals()
  } catch (submitError) { ElMessage.error(userFacingError(submitError, '套餐保存失败')) }
  finally { submitting.value = false }
}
function prepareAction(kind: ActionKind, row: Setmeal) { actionKind.value = kind; actionTarget.value = row; actionDialogOpen.value = true }
async function executeAction() {
  const target = actionTarget.value
  if (!target) return
  actionSubmitting.value = true
  rowActionId.value = target.id
  try {
    if (actionKind.value === 'delete') { await withWriteLock(`setmeal:delete:${target.id}`, () => setmealApi.remove([target.id])); ElMessage.success('套餐已删除') }
    else { const status: EnableStatus = target.status === 1 ? 0 : 1; await withWriteLock(`setmeal:status:${target.id}`, () => setmealApi.setStatus(target.id, status)); ElMessage.success(status === 1 ? '套餐已启售' : '套餐已停售') }
    actionDialogOpen.value = false
    await loadSetmeals()
  } catch (actionError) { ElMessage.error(userFacingError(actionError, actionKind.value === 'delete' ? '套餐删除失败' : '套餐状态更新失败')) }
  finally { actionSubmitting.value = false; rowActionId.value = undefined }
}

onMounted(async () => { await Promise.all([loadCategories(), loadSetmeals()]) })
</script>

<style scoped>
.data-card { padding: 8px 20px 0; overflow: hidden; }
.product-cell { display: flex; align-items: center; gap: 11px; }
.product-cell__image { width:48px; height:48px; flex:0 0 48px; overflow:hidden; border-radius:13px; }
.product-cell__image :deep(img) { width:100%; height:100%; object-fit:cover; }
.product-cell strong,.product-cell small { display: block; }
.product-cell small { margin-top: 3px; color: var(--muted); font-size: 10px; }
.money-cell { color: #d26631; }
.table-actions { display: flex; align-items: center; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 15px; }
.form-grid__full { grid-column: 1 / -1; }
.selected-dishes { width: 100%; }
.selected-dishes__heading { margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.selected-dishes__heading strong,.selected-dishes__heading span { display: block; }
.selected-dishes__heading span { margin-top: 3px; color: var(--muted); font-size: 10px; }
.selected-dishes__empty { padding: 28px 18px; border: 1px dashed #d8e2de; border-radius: 14px; color: var(--muted); background: #f8faf9; font-size: 11px; text-align:center; }
.selected-dishes__list { display: grid; gap: 8px; }
.selected-dishes__list article { padding: 11px 12px; display: grid; grid-template-columns: 1fr 130px 100px 34px; align-items:center; gap: 10px; border: 1px solid var(--line); border-radius: 13px; background: #fafcfb; }
.selected-dishes__list article div strong,.selected-dishes__list article div small { display:block; }
.selected-dishes__list article div small { margin-top:2px; color:var(--muted); font-size:10px; }
.selected-dishes__list article > strong { color:#d26631; font-size:12px; text-align:right; }
.selected-dishes__total { margin-top:10px; padding:10px 12px; display:flex; justify-content:space-between; gap:12px; border-radius:12px; color:var(--muted); background:var(--brand-soft); font-size:11px; }
.selected-dishes__total strong { color:var(--brand-deep); }
@media (max-width:720px) { .page-heading { align-items:stretch; flex-direction:column; } .page-heading :deep(.el-button) { width:100%; } .data-card { padding-inline:12px; } .form-grid { grid-template-columns:1fr; } .form-grid__full { grid-column:auto; } .selected-dishes__list article { grid-template-columns:1fr 110px 34px; } .selected-dishes__list article > strong { display:none; } .selected-dishes__total { flex-direction:column; } }
</style>
