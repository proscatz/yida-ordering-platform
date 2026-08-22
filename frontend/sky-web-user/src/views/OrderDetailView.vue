<template>
  <div class="page page--sub detail-page">
    <PageHeader title="订单详情" />
    <template v-if="order">
      <section class="status-card page-card">
        <div class="status-icon"><van-icon :name="statusIcon" /></div>
        <div><span>{{ orderStatusMap[order.status] }}</span><p>{{ statusDescription }}</p></div>
      </section>
      <section class="timeline page-card">
        <van-steps :active="activeStep" active-color="#0f766e">
          <van-step>已下单</van-step><van-step>已支付</van-step><van-step>制作中</van-step><van-step>配送中</van-step><van-step>完成</van-step>
        </van-steps>
      </section>
      <section class="detail-section page-card">
        <h2>餐品明细</h2>
        <div v-for="item in order.orderDetailList" :key="String(item.id)" class="detail-item">
          <div class="detail-item__image"><ProductImage :src="item.image" :alt="item.name" /></div>
          <div><strong>{{ item.name }}</strong><small>{{ item.dishFlavor || '标准规格' }}</small></div><span>×{{ item.number }}</span><b class="price">{{ money(Number(item.amount) * item.number) }}</b>
        </div>
        <div class="detail-total"><span>订单金额</span><strong class="price">{{ money(order.amount) }}</strong></div>
      </section>
      <section class="detail-section page-card info-list">
        <h2>履约信息</h2>
        <div><span>订单号</span><b>{{ order.number }}</b></div><div><span>下单时间</span><b>{{ formatTime(order.orderTime) }}</b></div>
        <div><span>联系人</span><b>{{ order.consignee }} {{ order.phone }}</b></div><div><span>配送地址</span><b>{{ order.address }}</b></div>
        <div v-if="order.remark"><span>订单备注</span><b>{{ order.remark }}</b></div>
        <div v-if="order.cancelReason"><span>取消原因</span><b>{{ order.cancelReason }}</b></div>
      </section>
      <div class="sticky-submit"><div class="sticky-submit__inner actions"><van-button v-if="order.status === 1 || order.status === 2" plain @click="cancel">取消订单</van-button><van-button v-if="[2,3,4].includes(order.status)" plain @click="remind">催一下</van-button><van-button v-if="order.status === 1" type="primary" class="brand-button" @click="router.push(`/payment/${order.id}`)">去支付</van-button><van-button v-if="order.status === 5 || order.status === 6" type="primary" class="brand-button" @click="repeat">再来一单</van-button></div></div>
    </template>
    <van-loading v-else class="loading" vertical>加载详情中</van-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button as VanButton, Icon as VanIcon, Loading as VanLoading, Step as VanStep, Steps as VanSteps, showConfirmDialog, showToast } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import ProductImage from '@/components/ProductImage.vue'
import { orderApi } from '@/api/modules'
import { errorMessage } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import { formatTime, money, orderStatusMap } from '@/utils/format'
import type { Order } from '@/types'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const order = ref<Order>()
const activeStep = computed(() => order.value ? ({ 1: 0, 2: 1, 3: 2, 4: 3, 5: 4, 6: 0 }[order.value.status] ?? 0) : 0)
const statusIcon = computed(() => order.value?.status === 5 ? 'passed' : order.value?.status === 6 ? 'close' : order.value?.status === 4 ? 'logistics' : 'clock-o')
const statusDescription = computed(() => ({ 1: '请在订单关闭前完成支付', 2: '订单已支付，等待门店确认', 3: '门店正在为你准备餐品', 4: '餐品正在送往约定地址', 5: '本次履约已完成，期待再次见面', 6: '订单已取消' }[order.value?.status || 0] || '订单状态更新中'))

onMounted(load)
async function load() { try { order.value = await orderApi.detail(String(route.params.id)) } catch (error) { showToast(errorMessage(error)) } }
async function cancel() { if (!order.value) return; try { await showConfirmDialog({ title: '取消订单', message: '确定取消当前订单吗？' }); await orderApi.cancel(order.value.id); await load(); showToast('订单已取消') } catch (error) { if (error !== 'cancel') showToast(errorMessage(error)) } }
async function remind() { if (!order.value) return; try { await orderApi.remind(order.value.id); showToast('已提醒门店') } catch (error) { showToast(errorMessage(error)) } }
async function repeat() { if (!order.value) return; try { await orderApi.repeat(order.value.id); await cart.load(); showToast('餐品已加入购物车'); await router.push('/') } catch (error) { showToast(errorMessage(error)) } }
</script>

<style scoped>
.detail-page { padding-bottom: 110px; }
.status-card { padding: 22px; display: flex; align-items: center; gap: 15px; color: white; background: linear-gradient(125deg,#0b5b56,#11877d); }
.status-icon { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 17px; color: var(--brand); background: #e7f7f2; font-size: 27px; }
.status-card span { font-size: 22px; font-weight: 850; }
.status-card p { margin: 5px 0 0; color: #d7efea; font-size: 12px; }
.timeline, .detail-section { margin-top: 14px; padding: 17px; }
.detail-section h2 { margin: 0 0 13px; font-size: 17px; }
.detail-item { padding: 9px 0; display: grid; grid-template-columns: 50px 1fr auto auto; gap: 9px; align-items: center; }
.detail-item__image { width: 50px; height: 50px; overflow: hidden; border-radius: 11px; }
.detail-item strong, .detail-item small { display: block; }
.detail-item small { margin-top: 4px; color: var(--muted); font-size: 10px; }
.detail-item > span { color: var(--muted); }
.detail-total { margin-top: 10px; padding-top: 15px; display: flex; justify-content: space-between; border-top: 1px solid var(--line); }
.detail-total .price { font-size: 21px; }
.info-list > div { padding: 8px 0; display: flex; justify-content: space-between; gap: 22px; font-size: 13px; }
.info-list span { flex: 0 0 64px; color: var(--muted); }
.info-list b { text-align: right; font-weight: 500; }
.actions { justify-content: flex-end; }
.actions .van-button { min-width: 98px; border-radius: 14px; }
.loading { padding-top: 80px; }
</style>