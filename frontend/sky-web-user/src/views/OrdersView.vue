<template>
  <div class="page orders-page">
    <header class="orders-header"><div><span>履约中心</span><h1>我的订单</h1></div><button type="button" @click="load"><van-icon name="replay" /></button></header>
    <van-tabs v-model:active="activeTab" sticky offset-top="0" shrink @change="load">
      <van-tab v-for="tab in tabs" :key="tab.label" :title="tab.label" />
    </van-tabs>
    <van-pull-refresh v-model="refreshing" @refresh="load">
      <div v-if="loading" class="loading"><van-loading vertical>加载订单中</van-loading></div>
      <EmptyState v-else-if="!orders.length" title="这里还没有订单" description="去挑选今天想吃的餐品吧" icon="orders-o" />
      <div v-else class="order-list">
        <article v-for="order in orders" :key="String(order.id)" class="order-card page-card" @click="router.push(`/orders/${order.id}`)">
          <div class="order-card__head"><div><span>订单 {{ order.number }}</span><small>{{ formatTime(order.orderTime) }}</small></div><strong>{{ orderStatusMap[order.status] }}</strong></div>
          <div class="order-goods">
            <div v-for="detail in (order.orderDetailList || []).slice(0, 3)" :key="String(detail.id)" class="order-goods__image"><ProductImage :src="detail.image" :alt="detail.name" /></div>
            <div v-if="!order.orderDetailList?.length" class="order-summary">{{ order.orderDishes || '餐品详情' }}</div>
          </div>
          <div class="order-card__amount"><span>共 {{ itemCount(order) }} 件</span><span>实付 <b class="price">{{ money(order.amount) }}</b></span></div>
          <div class="order-actions" @click.stop>
            <van-button v-if="order.status === 1 || order.status === 2" size="small" plain @click="cancel(order)">取消订单</van-button>
            <van-button v-if="[2,3,4].includes(order.status)" size="small" plain @click="remind(order)">催一下</van-button>
            <van-button v-if="order.status === 1" size="small" type="primary" @click="router.push(`/payment/${order.id}`)">去支付</van-button>
            <van-button v-if="order.status === 5 || order.status === 6" size="small" type="primary" @click="repeat(order)">再来一单</van-button>
          </div>
        </article>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button as VanButton, Icon as VanIcon, Loading as VanLoading, PullRefresh as VanPullRefresh, Tab as VanTab, Tabs as VanTabs, showConfirmDialog, showToast } from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import ProductImage from '@/components/ProductImage.vue'
import { orderApi } from '@/api/modules'
import { errorMessage } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import { formatTime, money, orderStatusMap } from '@/utils/format'
import type { Order } from '@/types'

const tabs: { label: string; status?: number }[] = [
  { label: '全部' }, { label: '待支付', status: 1 }, { label: '待接单', status: 2 },
  { label: '制作中', status: 3 }, { label: '配送中', status: 4 }, { label: '已完成', status: 5 }, { label: '已取消', status: 6 },
]
const activeTab = ref(0)
const orders = ref<Order[]>([])
const loading = ref(false)
const refreshing = ref(false)
const router = useRouter()
const cart = useCartStore()

onMounted(load)
async function load() {
  loading.value = !refreshing.value
  try { orders.value = (await orderApi.list(1, 30, tabs[activeTab.value]?.status)).records }
  catch (error) { showToast(errorMessage(error)) }
  finally { loading.value = false; refreshing.value = false }
}
function itemCount(order: Order) { return (order.orderDetailList || []).reduce((sum, item) => sum + item.number, 0) }
async function cancel(order: Order) {
  try { await showConfirmDialog({ title: '取消订单', message: '确定取消当前订单吗？' }); await orderApi.cancel(order.id); showToast('订单已取消'); await load() }
  catch (error) { if (error !== 'cancel') showToast(errorMessage(error)) }
}
async function remind(order: Order) {
  try { await orderApi.remind(order.id); showToast('已提醒门店，请耐心等待') }
  catch (error) { showToast(errorMessage(error)) }
}
async function repeat(order: Order) {
  try { await orderApi.repeat(order.id); await cart.load(); showToast('餐品已加入购物车'); await router.push('/') }
  catch (error) { showToast(errorMessage(error)) }
}
</script>

<style scoped>
.orders-header { padding: 10px 2px 18px; display: flex; align-items: end; justify-content: space-between; }
.orders-header span { color: var(--brand); font-size: 11px; font-weight: 750; letter-spacing: .12em; }
.orders-header h1 { margin: 4px 0 0; font-size: 30px; letter-spacing: -.05em; }
.orders-header button { width: 40px; height: 40px; border: 0; border-radius: 14px; color: var(--brand); background: var(--brand-soft); font-size: 18px; }
.orders-page :deep(.van-tabs__wrap) { margin: 0 -16px 14px; background: transparent; }
.order-list { display: grid; gap: 14px; }
.order-card { padding: 17px; cursor: pointer; }
.order-card__head { display: flex; justify-content: space-between; gap: 12px; }
.order-card__head span, .order-card__head small { display: block; }
.order-card__head span { font-size: 13px; font-weight: 700; }
.order-card__head small { margin-top: 5px; color: var(--muted); }
.order-card__head > strong { color: var(--brand); font-size: 14px; }
.order-goods { min-height: 62px; margin: 16px 0; display: flex; gap: 8px; }
.order-goods__image { width: 62px; height: 62px; overflow: hidden; border-radius: 12px; }
.order-summary { color: var(--muted); font-size: 13px; }
.order-card__amount { padding-bottom: 13px; display: flex; justify-content: flex-end; gap: 13px; color: var(--muted); font-size: 12px; }
.order-card__amount .price { color: var(--ink); font-size: 16px; }
.order-actions { padding-top: 13px; display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid var(--line); }
.order-actions .van-button { border-radius: 10px; }
.loading { padding: 80px 0; }
@media (min-width: 700px) { .order-list { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media (min-width: 900px) { .orders-page :deep(.van-tabs__wrap) { margin: 0 0 18px; border-radius: 16px; } }
</style>