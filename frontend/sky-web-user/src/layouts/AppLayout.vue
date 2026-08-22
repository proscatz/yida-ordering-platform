<template>
  <div class="app-shell">
    <aside class="app-header">
      <BrandLogo />
      <nav class="app-nav" aria-label="主要导航">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="app-nav__item">
          <van-icon :name="item.icon" /><span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>
    <main class="app-content"><RouterView /></main>
    <nav class="app-nav mobile-nav" aria-label="主要导航">
      <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="app-nav__item">
        <van-icon :name="item.icon" /><span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { Icon as VanIcon } from 'vant'
import BrandLogo from '@/components/BrandLogo.vue'

const nav = [
  { to: '/', icon: 'shop-o', label: '点餐' },
  { to: '/orders', icon: 'orders-o', label: '订单' },
  { to: '/profile', icon: 'contact-o', label: '我的' },
]
</script>

<style scoped>
@media (min-width: 900px) { .mobile-nav { display: none; } }
</style>