<template>
  <div class="content-page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">运营总览</span>
        <h1>工作台</h1>
        <p>{{ greeting }}，{{ authStore.displayName }}。今天也一起把每一份预约稳稳送达。</p>
      </div>
      <div class="workspace-actions">
        <div class="date-badge"><Calendar />{{ today }}</div>
        <button class="shop-control" :class="{ 'shop-control--open': shopStore.isOpen }" :disabled="loading || shopStore.loading" @click="prepareShopChange">
          <span><i />{{ shopStore.isOpen ? '营业中' : '已打烊' }}</span>
          <small>{{ shopStore.isOpen ? '点击暂停营业' : '点击开始营业' }}</small>
        </button>
      </div>
    </header>

    <PageErrorAlert v-if="error" :message="error" @retry="loadWorkspace" />

    <section v-loading="loading" class="workspace-body">
      <section class="metric-grid" aria-label="今日经营数据">
        <article v-for="metric in metrics" :key="metric.label" class="metric-card">
          <span class="metric-card__icon"><component :is="metric.icon" /></span>
          <div><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.caption }}</small></div>
        </article>
      </section>

      <section class="workspace-grid">
        <article class="surface-card order-overview">
          <div class="card-heading">
            <div><span class="eyebrow">今日履约</span><h2>订单概况</h2></div>
            <RouterLink to="/orders" class="card-link">查看订单<ArrowRight /></RouterLink>
          </div>
          <div class="order-total">
            <span>全部订单</span><strong>{{ orders.allOrders }}</strong><small>笔</small>
          </div>
          <div class="order-status-grid">
            <div><span class="status-orb status-orb--accent"><Bell /></span><strong>{{ orders.waitingOrders }}</strong><small>待接单</small></div>
            <div><span class="status-orb"><Van /></span><strong>{{ orders.deliveredOrders }}</strong><small>待派送</small></div>
            <div><span class="status-orb status-orb--success"><CircleCheck /></span><strong>{{ orders.completedOrders }}</strong><small>已完成</small></div>
            <div><span class="status-orb status-orb--muted"><CircleClose /></span><strong>{{ orders.cancelledOrders }}</strong><small>已取消</small></div>
          </div>
        </article>

        <article class="surface-card completion-card">
          <div class="card-heading"><div><span class="eyebrow">服务质量</span><h2>订单完成率</h2></div></div>
          <el-progress type="dashboard" :percentage="completionPercentage" :width="150" :stroke-width="13" color="#0f766e">
            <template #default><strong class="completion-value">{{ formatPercent(business.orderCompletionRate) }}</strong><small>今日完成率</small></template>
          </el-progress>
          <p>有效订单 {{ business.validOrderCount }} 笔，平均客单价 {{ formatMoney(business.unitPrice) }}</p>
        </article>
      </section>

      <section class="product-grid">
        <article class="surface-card product-overview">
          <div class="product-overview__heading"><span class="product-overview__icon"><Dish /></span><div><span>菜品总览</span><strong>{{ dishes.sold + dishes.discontinued }}</strong></div><RouterLink to="/dishes">管理菜品</RouterLink></div>
          <div class="product-overview__stats"><span><i class="enabled-dot" />在售 {{ dishes.sold }}</span><span><i />停售 {{ dishes.discontinued }}</span></div>
        </article>
        <article class="surface-card product-overview">
          <div class="product-overview__heading"><span class="product-overview__icon product-overview__icon--accent"><Wallet /></span><div><span>套餐总览</span><strong>{{ setmeals.sold + setmeals.discontinued }}</strong></div><RouterLink to="/setmeals">管理套餐</RouterLink></div>
          <div class="product-overview__stats"><span><i class="enabled-dot" />在售 {{ setmeals.sold }}</span><span><i />停售 {{ setmeals.discontinued }}</span></div>
        </article>
      </section>
    </section>

    <ConfirmActionDialog
      v-model="shopDialogOpen"
      :title="shopTargetStatus === 1 ? '确认开始营业？' : '确认暂停营业？'"
      :description="shopTargetStatus === 1 ? '开启后，用户端将恢复展示门店营业状态。' : '暂停后，用户端将看到门店休息状态，请确认当前订单已妥善处理。'"
      :confirm-text="shopTargetStatus === 1 ? '开始营业' : '暂停营业'"
      :tone="shopTargetStatus === 1 ? 'primary' : 'danger'"
      :loading="shopStore.changing"
      @confirm="changeShopStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Bell, Calendar, CircleCheck, CircleClose, Dish, TrendCharts, UserFilled, Van, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ConfirmActionDialog from '@/components/ConfirmActionDialog.vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import { userFacingError } from '@/api/errors'
import { workspaceApi } from '@/api/workspace'
import { useAuthStore } from '@/stores/auth'
import { useShopStore } from '@/stores/shop'
import { formatMoney, formatPercent } from '@/utils/format'
import type { BusinessData, EnableStatus, OrderOverview, ProductOverview } from '@/types/management'

const authStore = useAuthStore()
const shopStore = useShopStore()
const loading = ref(false)
const error = ref('')
const shopDialogOpen = ref(false)
const shopTargetStatus = ref<EnableStatus>(1)
const business = ref<BusinessData>({ turnover: 0, validOrderCount: 0, orderCompletionRate: 0, unitPrice: 0, newUsers: 0 })
const orders = ref<OrderOverview>({ waitingOrders: 0, deliveredOrders: 0, completedOrders: 0, cancelledOrders: 0, allOrders: 0 })
const dishes = ref<ProductOverview>({ sold: 0, discontinued: 0 })
const setmeals = ref<ProductOverview>({ sold: 0, discontinued: 0 })

const now = new Date()
const hour = now.getHours()
const greeting = hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const today = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(now)
const completionPercentage = computed(() => Math.min(100, Math.max(0, Math.round((business.value.orderCompletionRate || 0) * 100))))
const metrics = computed(() => [
  { label: '今日营业额', value: formatMoney(business.value.turnover), caption: '已完成订单收入', icon: TrendCharts },
  { label: '有效订单', value: business.value.validOrderCount, caption: '今日有效订单数', icon: CircleCheck },
  { label: '平均客单价', value: formatMoney(business.value.unitPrice), caption: '有效订单平均金额', icon: Wallet },
  { label: '新增用户', value: business.value.newUsers, caption: '今日新增用户数', icon: UserFilled },
])

async function loadWorkspace() {
  loading.value = true
  error.value = ''
  try {
    const [businessData, orderData, dishData, setmealData] = await Promise.all([
      workspaceApi.businessData(),
      workspaceApi.orderOverview(),
      workspaceApi.dishOverview(),
      workspaceApi.setmealOverview(),
      shopStore.refreshStatus(),
    ])
    business.value = businessData
    orders.value = orderData
    dishes.value = dishData
    setmeals.value = setmealData
  } catch (loadError) {
    error.value = userFacingError(loadError, '工作台加载失败')
  } finally {
    loading.value = false
  }
}

function prepareShopChange() {
  shopTargetStatus.value = shopStore.isOpen ? 0 : 1
  shopDialogOpen.value = true
}

async function changeShopStatus() {
  try {
    await shopStore.changeStatus(shopTargetStatus.value)
    shopDialogOpen.value = false
    ElMessage.success(shopStore.isOpen ? '店铺已开始营业' : '店铺已暂停营业')
  } catch (changeError) {
    ElMessage.error(userFacingError(changeError, '店铺状态更新失败'))
  }
}

onMounted(loadWorkspace)
</script>

<style scoped>
.workspace-actions { display: flex; align-items: stretch; gap: 10px; }
.shop-control { min-width: 150px; padding: 8px 13px; display: grid; gap: 2px; border: 1px solid var(--line); border-radius: 13px; background: white; cursor: pointer; text-align: left; box-shadow: 0 5px 18px rgba(23,56,54,.04); }
.shop-control:disabled { cursor: wait; opacity: .65; }
.shop-control span { display: flex; align-items: center; gap: 7px; color: #6d7c79; font-size: 12px; font-weight: 800; }
.shop-control span i { width: 7px; height: 7px; border-radius: 50%; background: #9aa5a2; }
.shop-control small { padding-left: 14px; color: #99a4a2; font-size: 9px; }
.shop-control--open span { color: var(--brand); }
.shop-control--open span i { background: #18a28f; box-shadow: 0 0 0 4px #dcf3ed; }
.workspace-body { min-height: 420px; }
.order-overview { min-height: 315px; }
.card-link { display: inline-flex; align-items: center; gap: 5px; color: var(--brand); font-size: 12px; font-weight: 750; }
.card-link svg { width: 13px; }
.order-total { margin: 24px 0 18px; padding: 17px 20px; border-radius: 16px; background: linear-gradient(110deg,#eef8f5,#f9fbfa); }
.order-total span { color: var(--muted); font-size: 12px; }
.order-total strong { margin-left: 15px; font-size: 30px; }
.order-total small { margin-left: 4px; color: var(--muted); }
.order-status-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 12px; }
.order-status-grid > div { min-width: 0; display: grid; grid-template-columns: 38px 1fr; grid-template-rows: 1fr 1fr; column-gap: 10px; align-items: center; }
.status-orb { grid-row: span 2; width: 38px; height: 38px; display: grid; place-items: center; border-radius: 12px; color: var(--brand); background: var(--brand-soft); }
.status-orb svg { width: 18px; }
.status-orb--accent { color: #dc682f; background: var(--accent-soft); }
.status-orb--success { color: #2e8b58; background: #e4f5e9; }
.status-orb--muted { color: #87928f; background: #eef1f0; }
.order-status-grid strong { align-self: end; font-size: 18px; }
.order-status-grid small { align-self: start; color: var(--muted); font-size: 10px; }
.completion-card { min-height: 315px; display: flex; align-items: center; flex-direction: column; }
.completion-card .card-heading { width: 100%; }
.completion-card :deep(.el-progress) { margin: 15px 0 2px; }
.completion-value { display: block; font-size: 24px; }
.completion-value + small { display: block; margin-top: 4px; color: var(--muted); font-size: 10px; }
.completion-card p { margin: 8px 0 0; color: var(--muted); font-size: 11px; text-align: center; }
.product-grid { margin-top: 20px; display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 20px; }
.product-overview__heading { display: grid; grid-template-columns: 48px 1fr auto; gap: 13px; align-items: center; }
.product-overview__icon { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 15px; color: var(--brand); background: var(--brand-soft); }
.product-overview__icon--accent { color: #d46832; background: var(--accent-soft); }
.product-overview__icon svg { width: 24px; }
.product-overview__heading div span,.product-overview__heading div strong { display: block; }
.product-overview__heading div span { color: var(--muted); font-size: 11px; }
.product-overview__heading div strong { margin-top: 2px; font-size: 22px; }
.product-overview__heading a { color: var(--brand); font-size: 11px; font-weight: 750; }
.product-overview__stats { margin-top: 17px; padding-top: 15px; display: flex; gap: 22px; border-top: 1px solid var(--line); color: var(--muted); font-size: 11px; }
.product-overview__stats span { display: flex; align-items: center; gap: 7px; }
.product-overview__stats i { width: 7px; height: 7px; border-radius: 50%; background: #a9b1af; }
.product-overview__stats .enabled-dot { background: #18a28f; }
@media (max-width: 900px) { .order-status-grid { grid-template-columns: repeat(2,1fr); row-gap: 20px; } }
@media (max-width: 720px) {
  .page-heading { display: block; }
  .workspace-actions { margin-top: 16px; }
  .shop-control { flex: 1; }
  .product-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .workspace-actions .date-badge { display: none; }
  .order-status-grid { grid-template-columns: 1fr 1fr; }
}
</style>
