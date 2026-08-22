<template>
  <main class="login-page">
    <section class="login-showcase">
      <BrandLogo />
      <div class="login-showcase__content">
        <span class="eyebrow eyebrow--light">高效协作 · 准时履约</span>
        <h1>让每一份预约<br />都有序抵达</h1>
        <p>从商品维护到订单履约，用清晰的数据与流程连接门店运营的每一个环节。</p>
      </div>
      <div class="login-showcase__features">
        <div v-for="feature in features" :key="feature.title" class="feature-chip">
          <component :is="feature.icon" />
          <span><strong>{{ feature.title }}</strong><small>{{ feature.caption }}</small></span>
        </div>
      </div>
      <span class="login-showcase__orb login-showcase__orb--one" />
      <span class="login-showcase__orb login-showcase__orb--two" />
    </section>

    <section class="login-panel">
      <div class="login-panel__mobile-brand"><BrandLogo /></div>
      <div class="login-card">
        <div class="login-card__heading">
          <span class="eyebrow">管理端入口</span>
          <h2>欢迎回来</h2>
          <p>登录后继续管理预约与履约服务</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" autocomplete="username" placeholder="请输入登录账号" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              autocomplete="current-password"
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              show-password
              type="password"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button class="brand-submit" type="primary" native-type="submit" :loading="submitting">
            {{ submitting ? '正在登录…' : '进入管理中心' }}
          </el-button>
        </el-form>

        <p class="login-card__notice"><Lock class="login-card__notice-icon" />请使用门店管理员分配的账号登录</p>
      </div>
      <p class="login-panel__footer">驿达点餐 · 预约点餐与履约管理平台</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, Lock, Timer, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { userFacingError } from '@/api/errors'
import BrandLogo from '@/components/BrandLogo.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
}
const features = [
  { title: '履约可视', caption: '状态清晰流转', icon: Timer },
  { title: '经营有数', caption: '关键指标聚合', icon: DataLine },
]

async function submit() {
  if (submitting.value || !(await formRef.value?.validate().catch(() => false))) return
  submitting.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/workspace'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(userFacingError(error, '登录失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}
</script>
