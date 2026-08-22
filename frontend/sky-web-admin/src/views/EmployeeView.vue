<template>
  <div class="content-page">
    <header class="page-heading">
      <div><span class="eyebrow">组织管理</span><h1>员工管理</h1><p>维护门店员工账号、联系方式与在岗状态。</p></div>
      <el-button v-if="authStore.isAdmin" type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
    </header>

    <FilterToolbar>
      <el-form inline @submit.prevent="search">
        <el-form-item label="员工姓名">
          <el-input v-model.trim="query.name" placeholder="输入姓名查询" clearable maxlength="20" @keyup.enter="search" />
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </template>
    </FilterToolbar>

    <PageErrorAlert v-if="error" :message="error" @retry="loadEmployees" />

    <section class="surface-card data-card">
      <el-table v-loading="loading" :data="records" row-key="id" class="responsive-table">
        <el-table-column prop="name" label="员工" min-width="150">
          <template #default="{ row }: { row: Employee }">
            <div class="identity-cell">
              <span>{{ row.name.slice(0, 1) }}</span>
              <div>
                <strong>{{ row.name }}</strong>
                <small>@{{ row.username }}</small>
                <div class="identity-badges">
                  <em v-if="row.id === authStore.session?.id">当前账号</em>
                  <em v-if="row.role === 'ADMIN'" class="identity-badges__admin">管理员</em>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="性别" width="82"><template #default="{ row }: { row: Employee }">{{ sexLabel(row.sex) }}</template></el-table-column>
        <el-table-column label="角色" width="100"><template #default="{ row }: { row: Employee }">{{ row.role === 'ADMIN' ? '管理员' : '普通员工' }}</template></el-table-column>
        <el-table-column label="身份证号" min-width="160"><template #default="{ row }: { row: Employee }">{{ maskIdNumber(row.idNumber) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }: { row: Employee }"><StatusTag :enabled="row.status === 1" /></template></el-table-column>
        <el-table-column label="更新时间" min-width="165"><template #default="{ row }: { row: Employee }">{{ formatDateTime(row.updateTime) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }: { row: Employee }">
            <div v-if="authStore.isAdmin" class="table-actions">
              <el-button link type="primary" :title="employeeEditBlockReason(row)" :disabled="rowActionId === row.id || Boolean(employeeEditBlockReason(row))" @click="openEdit(row)">编辑</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" :title="employeeStatusBlockReason(row, authStore.session?.id)" :disabled="rowActionId === row.id || Boolean(employeeStatusBlockReason(row, authStore.session?.id))" @click="prepareStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty><EmptyState title="未找到员工" description="调整筛选条件，或新增一名员工。" /></template>
      </el-table>
      <DataPagination v-model:page="query.page" v-model:page-size="query.pageSize" :total="total" @change="loadEmployees" />
    </section>

    <EntityFormDrawer
      v-model="drawerOpen"
      :title="editingId ? '编辑员工' : '新增员工'"
      :submitting="submitting"
      :submit-text="editingId ? '保存修改' : '创建员工'"
      @submit="submitEmployee"
    >
      <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submitEmployee">
        <div class="form-grid">
          <el-form-item label="登录账号" prop="username"><el-input v-model.trim="form.username" maxlength="20" placeholder="请输入登录账号" /></el-form-item>
          <el-form-item label="员工姓名" prop="name"><el-input v-model.trim="form.name" maxlength="20" placeholder="请输入员工姓名" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" maxlength="11" placeholder="请输入手机号" /></el-form-item>
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="form.sex"><el-radio value="1">男</el-radio><el-radio value="2">女</el-radio></el-radio-group>
          </el-form-item>
          <el-form-item class="form-grid__full" label="身份证号" prop="idNumber"><el-input v-model.trim="form.idNumber" maxlength="18" placeholder="请输入18位身份证号" /></el-form-item>
        </div>
        <el-alert v-if="!editingId" title="新员工将使用服务端设置的初始密码，请提醒员工首次登录后妥善保管。" type="info" :closable="false" show-icon />
      </el-form>
    </EntityFormDrawer>

    <ConfirmActionDialog
      v-model="statusDialogOpen"
      :title="statusTarget?.status === 1 ? '确认禁用员工？' : '确认启用员工？'"
      :description="statusTarget?.status === 1 ? `禁用后，${statusTarget.name} 将无法继续登录管理端。` : `启用后，${statusTarget?.name || ''} 将恢复管理端访问。`"
      :confirm-text="statusTarget?.status === 1 ? '确认禁用' : '确认启用'"
      :tone="statusTarget?.status === 1 ? 'danger' : 'primary'"
      :loading="statusChanging"
      @confirm="changeStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import ConfirmActionDialog from '@/components/ConfirmActionDialog.vue'
import DataPagination from '@/components/DataPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import EntityFormDrawer from '@/components/EntityFormDrawer.vue'
import FilterToolbar from '@/components/FilterToolbar.vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import { employeeApi } from '@/api/employees'
import { userFacingError } from '@/api/errors'
import { formatDateTime } from '@/utils/format'
import { withWriteLock } from '@/utils/writeLock'
import { employeeEditBlockReason, employeeStatusBlockReason } from '@/utils/employeeAccess'
import { useAuthStore } from '@/stores/auth'
import type { Employee, EmployeePayload, EnableStatus } from '@/types/management'

const loading = ref(false)
const authStore = useAuthStore()
const detailLoading = ref(false)
const submitting = ref(false)
const statusChanging = ref(false)
const error = ref('')
const records = ref<Employee[]>([])
const total = ref(0)
const drawerOpen = ref(false)
const editingId = ref<number>()
const rowActionId = ref<number>()
const statusDialogOpen = ref(false)
const statusTarget = ref<Employee>()
const formRef = ref<FormInstance>()
const query = reactive({ name: '', page: 1, pageSize: 10 })
const form = reactive<EmployeePayload>({ username: '', name: '', phone: '', sex: '1', idNumber: '' })
const rules: FormRules<EmployeePayload> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,20}$/, message: '账号应为3至20位字母、数字或下划线', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' },
  ],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idNumber: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入正确的18位身份证号', trigger: 'blur' },
  ],
}

function resetForm() {
  Object.assign(form, { id: undefined, username: '', name: '', phone: '', sex: '1', idNumber: '' })
  formRef.value?.clearValidate()
}

async function loadEmployees() {
  loading.value = true
  error.value = ''
  try {
    const result = await employeeApi.page({ page: query.page, pageSize: query.pageSize, name: query.name || undefined })
    records.value = result.records
    total.value = result.total
    if (!records.value.length && query.page > 1 && total.value > 0) {
      query.page -= 1
      await loadEmployees()
    }
  } catch (loadError) {
    error.value = userFacingError(loadError, '员工列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; void loadEmployees() }
function resetFilters() { query.name = ''; query.page = 1; void loadEmployees() }
function openCreate() { editingId.value = undefined; resetForm(); drawerOpen.value = true }

async function openEdit(row: Employee) {
  if (employeeEditBlockReason(row)) return
  editingId.value = row.id
  resetForm()
  drawerOpen.value = true
  detailLoading.value = true
  rowActionId.value = row.id
  try {
    const detail = await employeeApi.detail(row.id)
    Object.assign(form, {
      id: detail.id,
      username: detail.username,
      name: detail.name,
      phone: detail.phone,
      sex: detail.sex,
      idNumber: detail.idNumber,
    })
  } catch (detailError) {
    drawerOpen.value = false
    ElMessage.error(userFacingError(detailError, '员工详情加载失败'))
  } finally {
    detailLoading.value = false
    rowActionId.value = undefined
  }
}

async function submitEmployee() {
  if (submitting.value || !(await formRef.value?.validate().catch(() => false))) return
  submitting.value = true
  const payload = { ...form }
  const actionKey = `employee:save:${editingId.value ?? 'new'}`
  try {
    await withWriteLock(actionKey, () => editingId.value ? employeeApi.update(payload) : employeeApi.create(payload))
    ElMessage.success(editingId.value ? '员工信息已更新' : '员工创建成功')
    drawerOpen.value = false
    if (!editingId.value) query.page = 1
    await loadEmployees()
  } catch (submitError) {
    ElMessage.error(userFacingError(submitError, '员工保存失败'))
  } finally {
    submitting.value = false
  }
}

function prepareStatus(row: Employee) {
  if (employeeStatusBlockReason(row, authStore.session?.id)) return
  statusTarget.value = row
  statusDialogOpen.value = true
}

async function changeStatus() {
  const target = statusTarget.value
  if (!target) return
  const nextStatus: EnableStatus = target.status === 1 ? 0 : 1
  statusChanging.value = true
  rowActionId.value = target.id
  try {
    await withWriteLock(`employee:status:${target.id}`, () => employeeApi.setStatus(target.id, nextStatus))
    statusDialogOpen.value = false
    ElMessage.success(nextStatus === 1 ? '员工已启用' : '员工已禁用')
    await loadEmployees()
  } catch (statusError) {
    ElMessage.error(userFacingError(statusError, '员工状态更新失败'))
  } finally {
    statusChanging.value = false
    rowActionId.value = undefined
  }
}

function sexLabel(sex: string) { return sex === '1' ? '男' : sex === '2' ? '女' : '未知' }
function maskIdNumber(value: string) { return value?.length >= 8 ? `${value.slice(0, 4)}**********${value.slice(-4)}` : '--' }

onMounted(loadEmployees)
</script>

<style scoped>
.data-card { padding: 8px 20px 0; overflow: hidden; }
.identity-cell { display: flex; align-items: center; gap: 10px; }
.identity-cell > span { width: 36px; height: 36px; flex: 0 0 36px; display: grid; place-items: center; border-radius: 12px; color: var(--brand); background: var(--brand-soft); font-weight: 800; }
.identity-cell strong,.identity-cell small { display: block; }
.identity-cell strong { font-size: 13px; }
.identity-cell small { margin-top: 2px; color: var(--muted); font-size: 10px; }
.identity-badges { margin-top: 5px; display: flex; gap: 5px; }
.identity-badges em { padding: 2px 6px; border-radius: 999px; color: var(--brand); background: var(--brand-soft); font-size: 9px; font-style: normal; font-weight: 800; }
.identity-badges .identity-badges__admin { color: #b45309; background: #fff1d6; }
.table-actions { display: flex; align-items: center; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.form-grid__full { grid-column: 1 / -1; }
@media (max-width: 720px) {
  .page-heading { align-items: stretch; flex-direction: column; }
  .page-heading :deep(.el-button) { width: 100%; }
  .data-card { padding-inline: 12px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-grid__full { grid-column: auto; }
}
</style>
