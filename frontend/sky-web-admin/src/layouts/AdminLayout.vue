<template>
  <div class="admin-shell" :class="{ 'admin-shell--collapsed': uiStore.sidebarCollapsed }">
    <AppSidebar
      :collapsed="uiStore.sidebarCollapsed"
      :mobile-open="uiStore.mobileNavigationOpen"
      @close-mobile="uiStore.closeMobileNavigation"
    />
    <section class="admin-main">
      <AppTopbar
        :collapsed="uiStore.sidebarCollapsed"
        @toggle-sidebar="uiStore.toggleSidebar"
        @toggle-mobile="uiStore.toggleMobileNavigation"
      />
      <main class="admin-content">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from '@/components/AppSidebar.vue'
import AppTopbar from '@/components/AppTopbar.vue'
import { useAdminOrderSocket } from '@/composables/useAdminOrderSocket'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
useAdminOrderSocket()
</script>
