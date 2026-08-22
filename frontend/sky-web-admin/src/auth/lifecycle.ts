import { ElMessage } from 'element-plus'
import type { Router } from 'vue-router'
import { configureUnauthorizedHandler } from '@/api/unauthorized'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { sessionStorageKey } from '@/utils/session'

export function installAuthLifecycle(router: Router) {
  configureUnauthorizedHandler(async () => {
    const authStore = useAuthStore(pinia)
    if (!authStore.isAuthenticated) return

    const currentRoute = router.currentRoute.value
    const redirect = currentRoute.meta.public ? undefined : currentRoute.fullPath
    authStore.clearLocalSession()
    ElMessage.warning('登录状态已失效，请重新登录')

    if (currentRoute.name !== 'login') {
      await router.replace({ name: 'login', query: redirect ? { redirect } : {} })
    }
  })

  window.addEventListener('storage', (event) => {
    if (event.key !== sessionStorageKey) return
    const authStore = useAuthStore(pinia)
    authStore.restoreSession()
    if (!authStore.isAuthenticated && !router.currentRoute.value.meta.public) {
      void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    }
  })
}
