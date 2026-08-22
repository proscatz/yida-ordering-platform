import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { showToast } from 'vant'
import 'vant/lib/index.css'
import './styles/base.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

setUnauthorizedHandler(() => {
  useAuthStore(pinia).clear()
  useCartStore(pinia).reset()
  if (router.currentRoute.value.name !== 'login') {
    showToast('登录状态已失效')
    void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

app.mount('#app')