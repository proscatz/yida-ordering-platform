<template>
  <div class="content-page">
    <header class="page-heading">
      <div><span class="eyebrow">履约中心</span><h1>订单管理</h1><p>按服务端状态处理接单、配送和订单异常。</p></div>
      <div
        class="socket-state"
        :class="`socket-state--${realtimeStore.connectionStatus}`"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        <i class="socket-state__dot" />
        <span>{{ socketStatusLabel }}</span>
      </div>
    </header>

    <section
      v-if="realtimeStore.connectionStatus === 'degraded'"
      class="realtime-warning"
      role="alert"
      aria-live="assertive"
    >
      <WarningFilled class="realtime-warning__icon" />
      <div class="realtime-warning__copy">
        <strong>实时订单提醒暂不可用</strong>
        <p>订单查询和状态操作仍可正常使用，请及时手动刷新。</p>
        <small>{{ socketStatusDetail }}</small>
      </div>
      <div class="realtime-warning__actions">
        <el-button
          type="danger"
          plain
          :loading="realtimeStore.manualRetrying"
          :disabled="realtimeStore.manualRetrying"
          @click="retryRealtime"
        >立即重试</el-button>
        <el-button :icon="Refresh" :loading="manualRefreshLoading" @click="manualRefreshOrders">刷新订单</el-button>
      </div>
    </section>

    <section class="order-statistics">
      <button type="button" @click="filterByStatus(2)"><span>待接单</span><strong>{{ statistics.toBeConfirmed }}</strong></button>
      <button type="button" @click="filterByStatus(3)"><span>待派送</span><strong>{{ statistics.confirmed }}</strong></button>
      <button type="button" @click="filterByStatus(4)"><span>派送中</span><strong>{{ statistics.deliveryInProgress }}</strong></button>
    </section>

    <FilterToolbar>
      <el-form inline @submit.prevent="search">
        <el-form-item label="订单号"><el-input v-model.trim="query.number" placeholder="输入订单号" clearable maxlength="40" @keyup.enter="search" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model.trim="query.phone" placeholder="输入手机号" clearable maxlength="20" @keyup.enter="search" /></el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="(meta, status) in ORDER_STATUS_META" :key="status" :label="meta.label" :value="Number(status)" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
            style="width: 350px"
          />
        </el-form-item>
      </el-form>
      <template #actions>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </template>
    </FilterToolbar>

    <PageErrorAlert v-if="error" :message="error" @retry="refreshAll" />

    <section class="surface-card data-card">
      <el-table v-loading="loading" :data="records" row-key="id" class="responsive-table">
        <el-table-column label="订单" min-width="210">
          <template #default="{ row }: { row: Order }">
            <div class="order-identity"><strong>{{ row.number }}</strong><small>{{ row.orderDishes || '暂无商品摘要' }}</small></div>
          </template>
        </el-table-column>
        <el-table-column label="顾客" min-width="145">
          <template #default="{ row }: { row: Order }"><div class="customer-cell"><strong>{{ row.consignee || row.userName || '--' }}</strong><small>{{ row.phone || '--' }}</small></div></template>
        </el-table-column>
        <el-table-column label="金额" width="110"><template #default="{ row }: { row: Order }"><strong>{{ formatMoney(row.amount) }}</strong></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }: { row: Order }"><OrderStatusTag :status="row.status" /></template></el-table-column>
        <el-table-column label="下单时间" min-width="165"><template #default="{ row }: { row: Order }">{{ formatDateTime(row.orderTime) }}</template></el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }: { row: Order }">
            <div class="table-actions">
              <el-button link type="primary" :disabled="rowActionId === row.id" @click="openDetail(row.id)">详情</el-button>
              <el-button
                v-for="action in allowedOrderActions(row.status)"
                :key="action"
                link
                :type="ORDER_ACTION_META[action].tone"
                :disabled="rowActionId === row.id"
                @click="prepareAction(row, action)"
              >{{ ORDER_ACTION_META[action].label }}</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty><EmptyState title="未找到订单" description="调整订单号、状态或时间范围后重试。" /></template>
      </el-table>
      <DataPagination v-model:page="query.page" v-model:page-size="query.pageSize" :total="total" @change="loadOrders" />
    </section>

    <el-drawer v-model="detailOpen" title="订单详情" size="min(680px, 100vw)" destroy-on-close>
      <div v-loading="detailLoading" class="order-detail">
        <template v-if="detail">
          <section class="detail-hero">
            <div><span>订单号</span><strong>{{ detail.number }}</strong></div>
            <OrderStatusTag :status="detail.status" />
          </section>
          <section class="detail-grid">
            <div><span>下单时间</span><strong>{{ formatDateTime(detail.orderTime) }}</strong></div>
            <div><span>订单金额</span><strong>{{ formatMoney(detail.amount) }}</strong></div>
            <div><span>收货人</span><strong>{{ detail.consignee || '--' }}</strong></div>
            <div><span>联系电话</span><strong>{{ detail.phone || '--' }}</strong></div>
            <div class="detail-grid__full"><span>履约地址</span><strong>{{ detail.address || '--' }}</strong></div>
            <div class="detail-grid__full"><span>备注</span><strong>{{ detail.remark || '无备注' }}</strong></div>
            <div v-if="detail.cancelReason" class="detail-grid__full"><span>取消原因</span><strong>{{ detail.cancelReason }}</strong></div>
            <div v-if="detail.rejectionReason" class="detail-grid__full"><span>拒单原因</span><strong>{{ detail.rejectionReason }}</strong></div>
          </section>
          <section class="detail-products">
            <h3>商品明细</h3>
            <article v-for="item in detail.orderDetailList || []" :key="item.id">
              <ProductThumbnail :src="item.image" :name="item.name" />
              <div><strong>{{ item.name }}</strong><small>{{ item.dishFlavor || '标准规格' }}</small></div>
              <span>× {{ item.number }}</span>
              <strong>{{ formatMoney(item.amount * item.number) }}</strong>
            </article>
            <EmptyState v-if="!detail.orderDetailList?.length" title="暂无商品明细" description="服务端未返回该订单的商品行。" />
          </section>
          <div v-if="allowedOrderActions(detail.status).length" class="detail-actions">
            <el-button
              v-for="action in allowedOrderActions(detail.status)"
              :key="action"
              :type="ORDER_ACTION_META[action].tone"
              :loading="actionLoading && rowActionId === detail.id"
              @click="prepareAction(detail, action)"
            >{{ ORDER_ACTION_META[action].label }}</el-button>
          </div>
        </template>
      </div>
    </el-drawer>

    <ConfirmActionDialog
      v-model="confirmDialogOpen"
      :title="`${currentActionLabel}订单？`"
      :description="`将订单 ${actionTarget?.number || ''} 更新为“${actionTarget ? orderStatusLabel(ORDER_ACTION_META[actionTarget.action].target) : ''}”，提交后以服务端结果为准。`"
      :confirm-text="`确认${currentActionLabel}`"
      :tone="actionTarget?.action === 'complete' ? 'primary' : 'warning'"
      :loading="actionLoading"
      @confirm="runPreparedAction()"
    />

    <OrderReasonDialog
      v-model="reasonDialogOpen"
      :action="reasonAction"
      :order-number="actionTarget?.number || ''"
      :loading="actionLoading"
      @confirm="runPreparedAction"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Refresh, Search, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ConfirmActionDialog from '@/components/ConfirmActionDialog.vue'
import DataPagination from '@/components/DataPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import FilterToolbar from '@/components/FilterToolbar.vue'
import OrderReasonDialog from '@/components/OrderReasonDialog.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import ProductThumbnail from '@/components/ProductThumbnail.vue'
import { orderApi } from '@/api/orders'
import { userFacingError } from '@/api/errors'
import { executeOrderAction, orderActionErrorMessage } from '@/services/orderActions'
import { createOrderDataRefresher } from '@/services/orderRefresh'
import { useOrderRealtimeStore } from '@/stores/orderRealtime'
import { formatDateTime, formatMoney } from '@/utils/format'
import { allowedOrderActions, ORDER_ACTION_META, ORDER_STATUS_META, orderStatusLabel } from '@/utils/orderState'
import type { Order, OrderAction, OrderPageQuery, OrderStatistics, OrderStatus } from '@/types/orders'

type ActionTarget = Pick<Order, 'id' | 'number' | 'status'> & { action: OrderAction }

const realtimeStore = useOrderRealtimeStore()
const loading = ref(false)
const detailLoading = ref(false)
const actionLoading = ref(false)
const manualRefreshLoading = ref(false)
const connectionClock = ref(Date.now())
const error = ref('')
const records = ref<Order[]>([])
const total = ref(0)
const detail = ref<Order | null>(null)
const detailOpen = ref(false)
const selectedId = ref<number>()
const rowActionId = ref<number>()
const confirmDialogOpen = ref(false)
const reasonDialogOpen = ref(false)
const actionTarget = ref<ActionTarget>()
const dateRange = ref<[string, string] | []>([])
const statistics = reactive<OrderStatistics>({ toBeConfirmed: 0, confirmed: 0, deliveryInProgress: 0 })
const query = reactive<{ page: number; pageSize: number; number: string; phone: string; status?: OrderStatus }>({
  page: 1,
  pageSize: 10,
  number: '',
  phone: '',
  status: undefined,
})

const socketStatusLabel = computed(() => {
  const status = realtimeStore.connectionStatus
  if (status === 'idle') return '实时提醒尚未启动'
  if (status === 'connecting') return '正在连接实时提醒'
  if (status === 'connected') return '实时提醒已连接'
  if (status === 'reconnecting') return `实时提醒断开，正在第 ${realtimeStore.connection.retryCount} 次重连`
  if (status === 'degraded') return '实时提醒服务异常'
  if (status === 'auth-failed') return '登录状态失效'
  return '实时提醒已关闭'
})
const outageDurationText = computed(() => {
  const startedAt = realtimeStore.connection.outageStartedAt
  if (startedAt === null) return '0 秒'
  const seconds = Math.max(0, Math.floor((connectionClock.value - startedAt) / 1_000))
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  return `${minutes} 分 ${seconds % 60} 秒`
})
const socketStatusDetail = computed(() => {
  const state = realtimeStore.connection
  const parts = [`已持续 ${outageDurationText.value}`, `连续重试 ${state.retryCount} 次`]
  if (state.lastCloseCode !== null) parts.push(`最后关闭码 ${state.lastCloseCode}`)
  if (state.waitingForRetry) parts.push('正在等待下一次后台重连')
  return parts.join(' · ')
})
const currentActionLabel = computed(() => actionTarget.value ? ORDER_ACTION_META[actionTarget.value.action].label : '操作')
const reasonAction = computed<'reject' | 'cancel'>(() => actionTarget.value?.action === 'reject' ? 'reject' : 'cancel')

function pageParams(): OrderPageQuery {
  return {
    page: query.page,
    pageSize: query.pageSize,
    number: query.number || undefined,
    phone: query.phone || undefined,
    status: query.status,
    beginTime: dateRange.value[0] || undefined,
    endTime: dateRange.value[1] || undefined,
  }
}

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const result = await orderApi.page(pageParams())
    records.value = result.records
    total.value = result.total
    if (!records.value.length && query.page > 1 && total.value > 0) {
      query.page -= 1
      await loadOrders()
    }
  } catch (loadError) {
    error.value = userFacingError(loadError, '订单列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try { Object.assign(statistics, await orderApi.statistics()) }
  catch (loadError) { if (!error.value) error.value = userFacingError(loadError, '订单统计加载失败') }
}

const refreshOrderData = createOrderDataRefresher(loadOrders, loadStatistics)

async function refreshAll() {
  await refreshOrderData()
}

function retryRealtime() {
  realtimeStore.retryNow()
}

async function manualRefreshOrders() {
  if (manualRefreshLoading.value) return
  manualRefreshLoading.value = true
  try {
    await refreshAll()
    if (!error.value) ElMessage.success('订单列表和统计数据已刷新')
  } finally {
    manualRefreshLoading.value = false
  }
}

function search() { query.page = 1; void loadOrders() }
function resetFilters() { Object.assign(query, { page: 1, number: '', phone: '', status: undefined }); dateRange.value = []; void loadOrders() }
function filterByStatus(status: OrderStatus) { query.status = status; query.page = 1; void loadOrders() }

async function loadDetail(id: number) {
  detailLoading.value = true
  try { detail.value = await orderApi.detail(id) }
  catch (loadError) { detail.value = null; ElMessage.error(userFacingError(loadError, '订单详情加载失败')) }
  finally { detailLoading.value = false }
}

function openDetail(id: number) {
  selectedId.value = id
  detailOpen.value = true
  void loadDetail(id)
}

function prepareAction(order: Pick<Order, 'id' | 'number' | 'status'>, action: OrderAction) {
  actionTarget.value = { ...order, action }
  if (action === 'reject' || action === 'cancel') reasonDialogOpen.value = true
  else confirmDialogOpen.value = true
}

async function runPreparedAction(reason?: string) {
  const target = actionTarget.value
  if (!target || actionLoading.value) return
  actionLoading.value = true
  rowActionId.value = target.id
  try {
    await executeOrderAction(target.id, target.status, target.action, reason)
    ElMessage.success(`订单已${ORDER_ACTION_META[target.action].label}`)
    confirmDialogOpen.value = false
    reasonDialogOpen.value = false
    await refreshAll()
    if (detailOpen.value && selectedId.value === target.id) await loadDetail(target.id)
  } catch (actionError) {
    ElMessage.error(orderActionErrorMessage(actionError))
    await refreshAll()
    if (detailOpen.value && selectedId.value === target.id) await loadDetail(target.id)
  } finally {
    actionLoading.value = false
    rowActionId.value = undefined
  }
}

watch(() => realtimeStore.revision, () => {
  void refreshAll()
  if (detailOpen.value && selectedId.value) void loadDetail(selectedId.value)
})

let connectionClockTimer: number | undefined
onMounted(() => {
  connectionClockTimer = window.setInterval(() => { connectionClock.value = Date.now() }, 1_000)
  void refreshAll()
})
onBeforeUnmount(() => {
  if (connectionClockTimer !== undefined) window.clearInterval(connectionClockTimer)
})
</script>

<style scoped>
.page-heading { align-items: center; }
.socket-state { min-height: 34px; padding: 8px 13px; display: inline-flex; align-items: center; gap: 8px; border-radius: 999px; color: var(--muted); background: var(--surface); box-shadow: var(--shadow-sm); font-size: 13px; font-weight: 750; line-height: 1.35; }
.socket-state__dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #a7b0ae; }
.socket-state--connected { color: #087165; background: #e2f5ef; }
.socket-state--connected .socket-state__dot { background: #13a18f; box-shadow: 0 0 0 4px rgba(19,161,143,.13); }
.socket-state--connecting { color: #1769aa; background: #e8f3ff; }
.socket-state--connecting .socket-state__dot { background: #3189d6; }
.socket-state--reconnecting { color: #a65c21; background: #fff0e4; }
.socket-state--reconnecting .socket-state__dot { background: #ed8737; }
.socket-state--degraded,.socket-state--auth-failed { color: #b43d47; background: #ffeaec; }
.socket-state--degraded .socket-state__dot,.socket-state--auth-failed .socket-state__dot { background: #dc4c59; }
.socket-state--connecting .socket-state__dot,.socket-state--reconnecting .socket-state__dot { animation: socket-pulse 1.5s ease-in-out infinite; }
.realtime-warning { margin: 0 0 18px; padding: 18px 20px; display: grid; grid-template-columns: auto minmax(0,1fr) auto; align-items: center; gap: 15px; border: 1px solid #f2b9b4; border-left: 5px solid #d94b54; border-radius: 18px; color: #7f2931; background: #fff2f0; box-shadow: 0 10px 28px rgba(137,39,48,.08); }
.realtime-warning__icon { width: 26px; color: #d94b54; }
.realtime-warning__copy strong { display: block; margin-bottom: 4px; font-size: 16px; }
.realtime-warning__copy p { margin: 0 0 5px; color: #663a3d; font-size: 13px; }
.realtime-warning__copy small { color: #9b6266; font-size: 12px; }
.realtime-warning__actions { display: flex; align-items: center; gap: 8px; }
@keyframes socket-pulse { 0%,100% { opacity: .48; transform: scale(.86); } 50% { opacity: 1; transform: scale(1.14); } }
.order-statistics { margin-bottom: 18px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.order-statistics button { padding: 18px 20px; display: flex; align-items: center; justify-content: space-between; border: 1px solid rgba(255,255,255,.9); border-radius: var(--radius); color: var(--ink); background: var(--surface); box-shadow: 0 8px 26px rgba(23,56,54,.05); cursor: pointer; }
.order-statistics button:hover { border-color: var(--brand-soft); transform: translateY(-1px); }
.order-statistics span { color: var(--muted); font-size: 12px; }
.order-statistics strong { color: var(--brand); font-size: 26px; }
.data-card { padding: 8px 20px 0; overflow: hidden; }
.order-identity strong,.order-identity small,.customer-cell strong,.customer-cell small { display: block; }
.order-identity strong,.customer-cell strong { color: var(--ink); font-size: 13px; }
.order-identity small { max-width: 260px; margin-top: 5px; overflow: hidden; color: var(--muted); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.customer-cell small { margin-top: 3px; color: var(--muted); font-size: 11px; }
.table-actions { display: flex; align-items: center; flex-wrap: wrap; }
.order-detail { min-height: 260px; }
.detail-hero { margin-bottom: 18px; padding: 19px; display: flex; align-items: center; justify-content: space-between; gap: 14px; border-radius: 18px; background: var(--brand-soft); }
.detail-hero span,.detail-grid span { display: block; margin-bottom: 5px; color: var(--muted); font-size: 11px; }
.detail-hero strong { color: var(--ink); font-size: 17px; }
.detail-grid { margin-bottom: 22px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-grid > div { padding: 14px; border: 1px solid var(--line); border-radius: 14px; }
.detail-grid strong { color: var(--ink); font-size: 13px; line-height: 1.6; }
.detail-grid__full { grid-column: 1 / -1; }
.detail-products h3 { margin: 0 0 12px; color: var(--ink); font-size: 15px; }
.detail-products article { padding: 11px 0; display: grid; grid-template-columns: 46px minmax(0, 1fr) auto auto; align-items: center; gap: 12px; border-bottom: 1px solid var(--line); }
.detail-products article :deep(.product-thumbnail) { width: 46px; height: 46px; }
.detail-products article div strong,.detail-products article div small { display: block; }
.detail-products article div small { margin-top: 3px; color: var(--muted); font-size: 10px; }
.detail-products article > span { color: var(--muted); font-size: 12px; }
.detail-actions { position: sticky; bottom: 0; margin: 22px -20px -20px; padding: 16px 20px; display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid var(--line); background: rgba(255,255,255,.96); backdrop-filter: blur(12px); }
@media (max-width: 900px) {
  .order-statistics { grid-template-columns: 1fr; }
  :deep(.el-date-editor) { max-width: 100%; }
}
@media (max-width: 720px) {
  .page-heading { align-items: stretch; flex-direction: column; }
  .socket-state { align-self: flex-start; }
  .realtime-warning { grid-template-columns: auto 1fr; }
  .realtime-warning__actions { grid-column: 1 / -1; padding-left: 41px; flex-wrap: wrap; }
  .data-card { padding-inline: 12px; }
  .detail-grid { grid-template-columns: 1fr; }
  .detail-grid__full { grid-column: auto; }
  .detail-products article { grid-template-columns: 42px minmax(0, 1fr) auto; }
  .detail-products article > strong { grid-column: 2 / -1; text-align: right; }
  .detail-actions { flex-wrap: wrap; }
  .detail-actions :deep(.el-button) { flex: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .socket-state__dot { animation: none !important; }
}
</style>
