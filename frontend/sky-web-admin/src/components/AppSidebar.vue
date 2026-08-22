<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed, 'sidebar--mobile-open': mobileOpen }">
    <div class="sidebar__brand">
      <BrandLogo :compact="collapsed && !mobileOpen" />
      <button v-if="mobileOpen" class="icon-button sidebar__close" aria-label="关闭导航" @click="$emit('close-mobile')">
        <Close />
      </button>
    </div>

    <nav class="sidebar__nav" aria-label="管理端主导航">
      <RouterLink
        v-for="item in navigation"
        :key="item.to"
        :to="item.to"
        class="sidebar__link"
        :title="collapsed && !mobileOpen ? item.label : undefined"
        @click="$emit('close-mobile')"
      >
        <component :is="item.icon" class="sidebar__icon" />
        <span v-if="!collapsed || mobileOpen">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar__support" :class="{ 'sidebar__support--compact': collapsed && !mobileOpen }">
      <div class="sidebar__support-icon"><Headset /></div>
      <template v-if="!collapsed || mobileOpen">
        <strong>需要帮助？</strong>
        <span>平台运营支持</span>
      </template>
    </div>
  </aside>
  <button v-if="mobileOpen" class="sidebar-backdrop" aria-label="关闭导航" @click="$emit('close-mobile')" />
</template>

<script setup lang="ts">
import {
  Collection,
  DataAnalysis,
  Dish,
  Headset,
  HomeFilled,
  List,
  UserFilled,
  Wallet,
  Close,
} from '@element-plus/icons-vue'
import BrandLogo from './BrandLogo.vue'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

defineProps<{ collapsed: boolean; mobileOpen: boolean }>()
defineEmits<{ 'close-mobile': [] }>()

const authStore = useAuthStore()
const allNavigation = [
  { to: '/workspace', label: '工作台', icon: HomeFilled },
  { to: '/orders', label: '订单管理', icon: List },
  { to: '/dishes', label: '菜品管理', icon: Dish },
  { to: '/setmeals', label: '套餐管理', icon: Wallet },
  { to: '/categories', label: '分类管理', icon: Collection },
  { to: '/employees', label: '员工管理', icon: UserFilled, adminOnly: true },
  { to: '/reports', label: '数据统计', icon: DataAnalysis },
]
const navigation = computed(() => allNavigation.filter((item) => !item.adminOnly || authStore.isAdmin))
</script>
