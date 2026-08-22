import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import { installAuthLifecycle } from './auth/lifecycle'
import { pinia } from './stores'
import router from './router'
import './styles/base.css'
import { runLegacyServiceWorkerMigration } from './utils/serviceWorkerMigration'

installAuthLifecycle(router)
createApp(App).use(pinia).use(router).use(ElementPlus).mount('#app')
void runLegacyServiceWorkerMigration()
