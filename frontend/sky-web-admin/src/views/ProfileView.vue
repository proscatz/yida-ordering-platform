<template>
  <div class="content-page profile-page">
    <header class="page-heading">
      <div><span class="eyebrow">账号中心</span><h1>我的</h1><p>确认当前登录身份和账号状态。</p></div>
    </header>

    <PageErrorAlert v-if="error" :message="error" @retry="loadProfile" />
    <section v-loading="loading" class="surface-card profile-card">
      <template v-if="profile">
        <div class="profile-hero">
          <span class="profile-avatar">{{ profile.name.slice(0, 1).toUpperCase() }}</span>
          <div>
            <div class="profile-name"><h2>{{ profile.name }}</h2><span :class="['role-badge', `role-badge--${profile.role.toLowerCase()}`]">{{ roleLabel }}</span></div>
            <p>@{{ profile.username }} · 员工编号 {{ profile.id }}</p>
          </div>
          <StatusTag :enabled="profile.status === 1" />
        </div>
        <dl class="profile-details">
          <div><dt>登录账号</dt><dd>{{ profile.username }}</dd></div>
          <div><dt>员工姓名</dt><dd>{{ profile.name }}</dd></div>
          <div><dt>手机号</dt><dd>{{ profile.phone || '--' }}</dd></div>
          <div><dt>性别</dt><dd>{{ sexLabel }}</dd></div>
          <div><dt>账号角色</dt><dd>{{ roleLabel }}</dd></div>
          <div><dt>账号状态</dt><dd>{{ profile.status === 1 ? '正常启用' : '已禁用' }}</dd></div>
        </dl>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageErrorAlert from '@/components/PageErrorAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import { employeeApi } from '@/api/employees'
import { userFacingError } from '@/api/errors'
import type { EmployeeProfile } from '@/types/management'

const profile = ref<EmployeeProfile>()
const loading = ref(false)
const error = ref('')
const roleLabel = computed(() => profile.value?.role === 'ADMIN' ? '管理员' : '普通员工')
const sexLabel = computed(() => profile.value?.sex === '1' ? '男' : profile.value?.sex === '2' ? '女' : '未设置')

async function loadProfile() {
  loading.value = true
  error.value = ''
  try { profile.value = await employeeApi.me() }
  catch (loadError) { error.value = userFacingError(loadError, '个人资料加载失败') }
  finally { loading.value = false }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page { max-width: 980px; }
.profile-card { min-height: 360px; padding: 28px; }
.profile-hero { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 18px; padding-bottom: 26px; border-bottom: 1px solid var(--line); }
.profile-avatar { width: 72px; height: 72px; display: grid; place-items: center; border-radius: 22px; color: white; background: linear-gradient(145deg, var(--brand), var(--brand-deep)); font-size: 28px; font-weight: 850; box-shadow: 0 12px 28px rgba(15,118,110,.2); }
.profile-name { display: flex; align-items: center; gap: 10px; }
.profile-name h2 { margin: 0; color: var(--ink); font-size: 24px; }
.profile-hero p { margin: 7px 0 0; color: var(--muted); font-size: 13px; }
.role-badge { padding: 5px 9px; border-radius: 999px; font-size: 10px; font-weight: 800; }
.role-badge--admin { color: #b45309; background: #fff1d6; }
.role-badge--employee { color: var(--brand); background: var(--brand-soft); }
.profile-details { margin: 0; padding-top: 26px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.profile-details div { padding: 18px; border-radius: 15px; background: var(--surface-muted); }
.profile-details dt { color: var(--muted); font-size: 11px; font-weight: 700; }
.profile-details dd { margin: 7px 0 0; color: var(--ink); font-size: 14px; font-weight: 750; }
@media (max-width: 680px) {
  .profile-card { padding: 20px; }
  .profile-hero { grid-template-columns: auto 1fr; }
  .profile-hero :deep(.status-tag) { grid-column: 1 / -1; }
  .profile-details { grid-template-columns: 1fr; }
}
</style>
