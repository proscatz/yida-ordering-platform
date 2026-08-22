import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const mobileNavigationOpen = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function toggleMobileNavigation() {
    mobileNavigationOpen.value = !mobileNavigationOpen.value
  }

  function closeMobileNavigation() {
    mobileNavigationOpen.value = false
  }

  return {
    sidebarCollapsed,
    mobileNavigationOpen,
    toggleSidebar,
    toggleMobileNavigation,
    closeMobileNavigation,
  }
})
