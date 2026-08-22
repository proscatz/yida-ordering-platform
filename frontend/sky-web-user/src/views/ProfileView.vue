<template>
  <div class="page profile-page">
    <section class="profile-hero">
      <BrandLogo :show-tagline="false" />
      <div class="profile-avatar">驿</div>
      <h1>驿达用户</h1><p>用户编号 {{ auth.userId || '--' }}</p>
    </section>
    <section class="quick-grid">
      <RouterLink to="/orders" class="page-card"><van-icon name="orders-o" /><strong>我的订单</strong><span>查看履约进度</span></RouterLink>
      <RouterLink to="/addresses" class="page-card"><van-icon name="location-o" /><strong>地址管理</strong><span>维护常用地址</span></RouterLink>
    </section>
    <section class="profile-menu page-card">
      <div><span><van-icon name="shield-o" /> 账号安全</span><small>BCrypt 密码验证</small></div>
      <div><span><van-icon name="service-o" /> 服务说明</span><small>预约点餐与履约管理</small></div>
      <button type="button" :disabled="loggingOut" @click="logout"><span><van-icon name="revoke" /> 退出登录</span><van-icon name="arrow" /></button>
    </section>
    <p class="version">驿达点餐 Web · 1.0.0</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { Icon as VanIcon, showConfirmDialog, showToast } from 'vant'
import BrandLogo from '@/components/BrandLogo.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { errorMessage } from '@/api/http'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const loggingOut = ref(false)
async function logout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定退出当前账号吗？' })
    loggingOut.value = true
    await auth.logout(); cart.reset(); await router.replace('/login')
  } catch (error) { if (error !== 'cancel') showToast(errorMessage(error)) }
  finally { loggingOut.value = false }
}
</script>

<style scoped>
.profile-hero { min-height: 260px; padding: 20px 20px 26px; border-radius: 0 0 30px 30px; color: white; background: linear-gradient(145deg,#0b5b56,#11877d); text-align: center; }
.profile-hero :deep(.brand-logo) { display: none; }
.profile-avatar { width: 76px; height: 76px; margin: 20px auto 12px; display: grid; place-items: center; border: 2px solid rgba(255,255,255,.5); border-radius: 26px; color: #0b5b56; background: #ffd0b2; font-size: 34px; font-weight: 900; }
.profile-hero h1 { margin: 0; font-size: 24px; }
.profile-hero p { color: #d8efea; }
.quick-grid { margin-top: -38px; padding: 0 4px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.quick-grid a { padding: 18px; display: grid; gap: 6px; }
.quick-grid .van-icon { color: var(--brand); font-size: 25px; }
.quick-grid span { color: var(--muted); font-size: 11px; }
.profile-menu { margin-top: 18px; overflow: hidden; }
.profile-menu > div, .profile-menu button { width: 100%; padding: 17px 18px; display: flex; align-items: center; justify-content: space-between; border: 0; border-bottom: 1px solid var(--line); background: white; text-align: left; }
.profile-menu span { display: flex; align-items: center; gap: 9px; }
.profile-menu span .van-icon { color: var(--brand); font-size: 19px; }
.profile-menu small { color: var(--muted); }
.version { margin-top: 24px; color: #9aa6a3; font-size: 11px; text-align: center; }
@media (min-width: 900px) { .profile-hero { border-radius: 26px; } .profile-hero :deep(.brand-logo) { display: inline-flex; float: left; } }
</style>