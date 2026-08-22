<template>
  <div class="page page--sub address-page">
    <PageHeader title="我的地址" action="新增" @action="router.push('/addresses/edit')" />
    <EmptyState v-if="!loading && !addresses.length" title="还没有常用地址" description="添加地址后即可确认订单" icon="location-o" />
    <div v-else class="card-list">
      <article v-for="address in addresses" :key="String(address.id)" class="address-card page-card" @click="selectAddress(address)">
        <div class="address-card__top"><strong>{{ address.consignee }}</strong><span>{{ address.phone }}</span><em v-if="address.isDefault === 1">默认</em></div>
        <p>{{ fullAddress(address) }}</p>
        <div class="address-card__foot">
          <button v-if="address.isDefault !== 1" type="button" @click.stop="makeDefault(address)"><van-icon name="passed" /> 设为默认</button>
          <span v-else class="default-text"><van-icon name="passed" /> 默认地址</span>
          <div><button type="button" @click.stop="router.push(`/addresses/edit/${address.id}`)">编辑</button><button type="button" class="danger" @click.stop="remove(address)">删除</button></div>
        </div>
      </article>
    </div>
    <van-loading v-if="loading" class="loading" vertical>加载地址中</van-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon as VanIcon, Loading as VanLoading, showConfirmDialog, showToast } from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { addressApi } from '@/api/modules'
import { errorMessage } from '@/api/http'
import { fullAddress } from '@/utils/format'
import type { Address } from '@/types'

const addresses = ref<Address[]>([])
const loading = ref(false)
const router = useRouter()
const route = useRoute()

onMounted(load)
async function load() {
  loading.value = true
  try { addresses.value = await addressApi.list() }
  catch (error) { showToast(errorMessage(error)) }
  finally { loading.value = false }
}
function selectAddress(address: Address) {
  if (route.query.select === '1') void router.replace({ path: '/checkout', query: { addressId: String(address.id) } })
}
async function makeDefault(address: Address) {
  if (!address.id) return
  try { await addressApi.setDefault(address.id); await load(); showToast('已设为默认地址') }
  catch (error) { showToast(errorMessage(error)) }
}
async function remove(address: Address) {
  if (!address.id) return
  try { await showConfirmDialog({ title: '删除地址', message: `确定删除 ${address.consignee} 的地址吗？` }); await addressApi.remove(address.id); await load(); showToast('地址已删除') }
  catch (error) { if (error !== 'cancel') showToast(errorMessage(error)) }
}
</script>

<style scoped>
.address-page { padding-bottom: 32px; }
.address-card { padding: 18px; cursor: pointer; }
.address-card__top { display: flex; align-items: center; gap: 10px; }
.address-card__top strong { font-size: 17px; }
.address-card__top span { color: var(--muted); }
.address-card__top em { padding: 3px 7px; border-radius: 7px; color: var(--brand); background: var(--brand-soft); font-size: 10px; font-style: normal; }
.address-card > p { margin: 12px 0 16px; line-height: 1.6; }
.address-card__foot { padding-top: 12px; display: flex; justify-content: space-between; border-top: 1px solid var(--line); }
.address-card button { border: 0; color: var(--muted); background: transparent; }
.address-card__foot > div { display: flex; gap: 12px; }
.address-card button.danger { color: #d65c4a; }
.default-text { color: var(--brand); font-size: 13px; }
.loading { padding: 60px 0; }
</style>