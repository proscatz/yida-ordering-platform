<template>
  <div class="page page--sub checkout-page">
    <PageHeader title="确认订单" />
    <button type="button" class="address-select page-card" @click="router.push('/addresses?select=1')">
      <span class="address-select__icon"><van-icon name="location" /></span>
      <span v-if="address" class="address-select__content"><strong>{{ address.consignee }} · {{ address.phone }}</strong><small>{{ fullAddress(address) }}</small></span>
      <span v-else class="address-select__content"><strong>选择配送地址</strong><small>下单前请先添加一个地址</small></span>
      <van-icon name="arrow" />
    </button>

    <section class="checkout-section page-card">
      <h2>本次餐品</h2>
      <div v-for="item in cart.items" :key="String(item.id)" class="checkout-item">
        <div class="checkout-item__image"><ProductImage :src="item.image" :alt="item.name" /></div>
        <div><strong>{{ item.name }}</strong><small>{{ item.dishFlavor || '标准规格' }}</small></div>
        <span>×{{ item.number }}</span><b class="price">{{ money(Number(item.amount) * item.number) }}</b>
      </div>
    </section>

    <section class="checkout-section page-card options">
      <van-cell title="履约方式" value="尽快送达" />
      <van-field v-model="remark" label="订单备注" placeholder="口味、门牌等特殊需求" maxlength="80" />
      <van-cell title="餐具">
        <template #value><van-stepper v-model="tablewareNumber" min="0" max="20" /></template>
      </van-cell>
      <van-cell title="支付方式" value="在线支付（默认 Mock）" />
    </section>

    <div class="amount-card page-card">
      <div><span>餐品小计</span><b>¥{{ money(cart.total) }}</b></div>
      <div><span>打包费</span><b>以服务端核算为准</b></div>
      <div class="amount-card__total"><span>预计合计</span><strong class="price">{{ money(cart.total) }}</strong></div>
    </div>

    <div class="sticky-submit"><div class="sticky-submit__inner"><div class="sticky-submit__main"><small class="muted">服务端将按当前价格复核</small><strong class="price">{{ money(cart.total) }}</strong></div><van-button type="primary" class="brand-button" :loading="submitting" :disabled="!cart.count" @click="submit">提交订单</van-button></div></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button as VanButton, Cell as VanCell, Field as VanField, Icon as VanIcon, Stepper as VanStepper, showToast } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import ProductImage from '@/components/ProductImage.vue'
import { addressApi, orderApi } from '@/api/modules'
import { errorMessage } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import { createRequestId, fullAddress, money } from '@/utils/format'
import type { Address } from '@/types'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const address = ref<Address>()
const remark = ref('')
const tablewareNumber = ref(0)
const submitting = ref(false)
const requestId = createRequestId()

onMounted(async () => {
  try {
    await cart.load()
    if (!cart.count) { showToast('购物车为空'); return void router.replace('/') }
    if (typeof route.query.addressId === 'string') address.value = await addressApi.detail(route.query.addressId)
    else {
      try { address.value = await addressApi.getDefault() }
      catch { address.value = (await addressApi.list())[0] }
    }
  } catch (error) { showToast(errorMessage(error)) }
})

async function submit() {
  if (submitting.value) return
  if (!address.value?.id) return showToast('请先选择配送地址')
  submitting.value = true
  try {
    const result = await orderApi.submit({
      requestId,
      addressBookId: address.value.id,
      payMethod: 1,
      remark: remark.value,
      deliveryStatus: 1,
      tablewareNumber: tablewareNumber.value,
      tablewareStatus: 0,
      packAmount: 0,
    })
    cart.reset()
    await router.replace(`/payment/${result.id}`)
  } catch (error) { showToast(errorMessage(error)) }
  finally { submitting.value = false }
}
</script>

<style scoped>
.checkout-page { padding-bottom: 120px; }
.address-select { width: 100%; padding: 18px; display: grid; grid-template-columns: 42px 1fr 18px; gap: 12px; align-items: center; border: 0; color: var(--ink); text-align: left; }
.address-select__icon { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 14px; color: var(--brand); background: var(--brand-soft); font-size: 21px; }
.address-select__content { min-width: 0; }
.address-select__content strong, .address-select__content small { display: block; }
.address-select__content small { margin-top: 5px; overflow: hidden; color: var(--muted); text-overflow: ellipsis; white-space: nowrap; }
.checkout-section, .amount-card { margin-top: 14px; padding: 17px; }
.checkout-section h2 { margin: 0 0 12px; font-size: 17px; }
.checkout-item { padding: 10px 0; display: grid; grid-template-columns: 52px 1fr auto auto; gap: 10px; align-items: center; }
.checkout-item__image { width: 52px; height: 52px; overflow: hidden; border-radius: 12px; }
.checkout-item div:nth-child(2) { min-width: 0; }
.checkout-item strong, .checkout-item small { display: block; }
.checkout-item small { margin-top: 4px; color: var(--muted); font-size: 10px; }
.checkout-item > span { color: var(--muted); }
.options { padding: 4px 0; overflow: hidden; }
.options :deep(.van-cell) { padding: 15px 17px; }
.amount-card > div { padding: 8px 0; display: flex; justify-content: space-between; color: var(--muted); font-size: 13px; }
.amount-card__total { margin-top: 8px; padding-top: 15px !important; border-top: 1px solid var(--line); color: var(--ink) !important; }
.amount-card__total .price { font-size: 21px; }
.sticky-submit__main strong { display: block; font-size: 21px; }
.sticky-submit .van-button { min-width: 138px; }
</style>