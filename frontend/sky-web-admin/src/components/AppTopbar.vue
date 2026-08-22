<template>
  <header class="topbar">
    <div class="topbar__leading">
      <button class="icon-button topbar__mobile-menu" aria-label="打开导航" @click="$emit('toggle-mobile')">
        <Menu />
      </button>
      <button class="icon-button topbar__collapse" :aria-label="collapsed ? '展开导航' : '收起导航'" @click="$emit('toggle-sidebar')">
        <Expand v-if="collapsed" />
        <Fold v-else />
      </button>
      <AppBreadcrumb />
    </div>

    <div class="topbar__actions">
      <div class="shop-status" :class="{ 'shop-status--open': shopStore.isOpen }" title="当前店铺营业状态">
        <span class="shop-status__dot" />
        <span class="shop-status__label">店铺状态</span>
        <strong>{{ shopStore.loading ? '同步中' : shopStore.status === null ? '未知' : shopStore.isOpen ? '营业中' : '已打烊' }}</strong>
      </div>

      <el-dropdown trigger="click" @command="handleCommand">
        <button class="account-button">
          <span class="account-button__avatar">{{ initial }}</span>
          <span class="account-button__copy">
            <strong>{{ authStore.displayName }}</strong>
            <small>@{{ authStore.username }} · {{ authStore.roleLabel }}</small>
          </span>
          <ArrowDown class="account-button__arrow" />
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile"><User />我的</el-dropdown-item>
            <el-dropdown-item command="logout" :disabled="authStore.loggingOut"><SwitchButton />退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Expand, Fold, Menu, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useShopStore } from '@/stores/shop'
import AppBreadcrumb from './AppBreadcrumb.vue'

defineProps<{ collapsed: boolean }>()
defineEmits<{ 'toggle-sidebar': []; 'toggle-mobile': [] }>()

const router = useRouter()
const authStore = useAuthStore()
const shopStore = useShopStore()
const initial = computed(() => authStore.displayName.slice(0, 1).toUpperCase())

async function handleCommand(command: string) {
  if (command === 'profile') {
    await router.push({ name: 'profile' })
    return
  }
  if (command !== 'logout') return
  const remoteConfirmed = await authStore.logout()
  await router.replace({ name: 'login' })
  if (!remoteConfirmed) ElMessage.warning('已退出当前页面，但服务端注销未确认')
}

onMounted(() => {
  if (shopStore.status === null) void shopStore.refreshStatus().catch(() => undefined)
})
</script>
