<template>
  <div class="page page--sub payment-page">
    <PageHeader title="订单支付" />
    <section v-if="order" class="payment-card page-card">
      <div class="payment-visual" :class="{ success: order.payStatus === 1 }"><van-icon :name="order.payStatus === 1 ? 'passed' : 'balance-pay'" /></div>
      <span class="payment-label">{{ order.payStatus === 1 ? '支付完成' : '订单待支付' }}</span>
      <strong class="price payment-amount">{{ money(order.amount) }}</strong>
      <p>订单号 {{ order.number }}</p>
      <div class="provider"><span>支付通道</span><b>{{ providerName }}</b></div>
      <van-button v-if="order.payStatus !== 1" type="primary" block class="brand-button" :loading="paying" @click="pay">立即支付</van-button>
      <van-button v-else block class="soft-button" @click="router.replace(`/orders/${order.id}`)">查看订单进度</van-button>
      <button v-if="order.payStatus !== 1" class="later" type="button" @click="router.replace('/orders')">稍后支付</button>
    </section>
    <van-loading v-else class="loading" vertical>加载订单中</van-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button as VanButton, Icon as VanIcon, Loading as VanLoading, showToast } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import { orderApi } from '@/api/modules'
import { errorMessage } from '@/api/http'
import { paymentAdapter } from '@/payments'
import { money } from '@/utils/format'
import type { Order } from '@/types'

const route = useRoute()
const router = useRouter()
const order = ref<Order>()
const paying = ref(false)
const adapter = paymentAdapter()
const providerName = computed(() => adapter.provider === 'mock' ? '本地 Mock 支付' : '真实支付适配器')

onMounted(load)
async function load() {
  try { order.value = await orderApi.detail(String(route.params.id)) }
  catch (error) { showToast(errorMessage(error)) }
}
async function pay() {
  if (!order.value || paying.value) return
  paying.value = true
  try { await adapter.pay(order.value); await load(); showToast('支付成功，订单已进入履约流程') }
  catch (error) { showToast(errorMessage(error)) }
  finally { paying.value = false }
}
</script>

<style scoped>
.payment-card { padding: 28px 22px; text-align: center; }
.payment-visual { width: 78px; height: 78px; margin: 4px auto 18px; display: grid; place-items: center; border-radius: 26px; color: #d86126; background: var(--accent-soft); font-size: 40px; }
.payment-visual.success { color: var(--brand); background: var(--brand-soft); }
.payment-label { display: block; color: var(--muted); }
.payment-amount { display: block; margin: 7px 0; font-size: 38px; }
.payment-card p { color: var(--muted); font-size: 12px; }
.provider { margin: 24px 0; padding: 15px; display: flex; justify-content: space-between; border-radius: 14px; background: #f4f6f3; }
.provider span { color: var(--muted); }
.later { margin-top: 18px; border: 0; color: var(--muted); background: transparent; }
.loading { padding-top: 80px; }
</style>