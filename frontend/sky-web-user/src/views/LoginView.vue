<template>
  <main class="login-page">
    <section class="login-intro">
      <BrandLogo />
      <div class="login-copy">
        <span class="eyebrow">WEB USER PORTAL</span>
        <h1>好餐不必等，<br /><em>提前约，准时取。</em></h1>
        <p>面向园区、门店与校园场景的预约点餐与履约管理平台。</p>
      </div>
      <div class="feature-row">
        <span><van-icon name="clock-o" /> 预约下单</span>
        <span><van-icon name="passed" /> 进度可追踪</span>
        <span><van-icon name="location-o" /> 灵活履约</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-form-wrap">
        <h2>欢迎回来</h2>
        <p class="muted">使用用户名或手机号登录网页用户端</p>
        <van-form class="login-form" @submit="submit">
          <van-field v-model.trim="identifier" name="identifier" label="账号" placeholder="用户名或手机号"
            left-icon="contact-o" :rules="[{ required: true, message: '请输入用户名或手机号' }]" />
          <van-field v-model="password" name="password" label="密码" type="password" placeholder="请输入密码"
            left-icon="closed-eye" :rules="[{ required: true, message: '请输入密码' }]" />
          <van-button class="brand-button" block type="primary" native-type="submit" :loading="loading" loading-text="正在登录">
            登录驿达点餐
          </van-button>
        </van-form>
        <div class="login-note"><van-icon name="shield-o" /> 本端不依赖微信授权 code，账号密码经安全接口验证</div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button as VanButton, Field as VanField, Form as VanForm, Icon as VanIcon, showToast } from 'vant'
import BrandLogo from '@/components/BrandLogo.vue'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/api/http'

const identifier = ref('')
const password = ref('')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function submit() {
  if (loading.value) return
  loading.value = true
  try {
    const account = identifier.value.trim()
    const isPhone = /^1\d{10}$/.test(account)
    await auth.login({ ...(isPhone ? { phone: account } : { username: account }), password: password.value })
    showToast('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    showToast(errorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; background: #f6f7f3; }
.login-intro { min-height: 43vh; padding: 24px; display: flex; flex-direction: column; justify-content: space-between; color: white; background: linear-gradient(145deg,rgba(9,82,77,.96),rgba(15,118,110,.9)), radial-gradient(circle at 80% 20%,#ff9b62,transparent 30%); border-radius: 0 0 34px 34px; }
.login-intro :deep(.brand-logo__name), .login-intro :deep(.brand-logo__tagline) { color: white; }
.login-copy { margin: 46px 0 36px; }
.eyebrow { font-size: 11px; letter-spacing: .18em; color: #bfe8df; font-weight: 700; }
h1 { margin: 14px 0; font-size: clamp(34px,10vw,54px); line-height: 1.08; letter-spacing: -.06em; }
h1 em { color: #ffbd91; font-style: normal; }
.login-copy p { max-width: 500px; color: #d8efea; line-height: 1.8; }
.feature-row { display: flex; flex-wrap: wrap; gap: 10px; }
.feature-row span { padding: 8px 11px; border: 1px solid rgba(255,255,255,.18); border-radius: 12px; background: rgba(255,255,255,.08); font-size: 12px; }
.login-panel { padding: 28px 20px 44px; display: grid; place-items: center; }
.login-form-wrap { width: min(100%,430px); }
h2 { margin: 0 0 8px; font-size: 27px; letter-spacing: -.04em; }
.login-form { margin-top: 28px; }
.login-form :deep(.van-cell) { margin-bottom: 14px; padding: 15px 16px; border: 1px solid var(--line); border-radius: 15px; }
.login-form :deep(.van-cell::after) { display: none; }
.login-form .van-button { margin-top: 10px; }
.login-note { margin-top: 18px; color: var(--muted); font-size: 12px; text-align: center; }
@media (min-width: 860px) {
  .login-page { grid-template-columns: minmax(440px,1.15fr) minmax(420px,.85fr); padding: 20px; gap: 20px; }
  .login-intro { min-height: calc(100vh - 40px); border-radius: 30px; padding: 38px 44px; }
  .login-panel { min-height: calc(100vh - 40px); }
}
</style>