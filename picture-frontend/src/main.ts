import { createApp } from 'vue'
import App from './App.vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { createPinia } from 'pinia'
import Router from './config/router'
import './access'

const pinia = createPinia()
const app = createApp(App)
const router:any = Router

// 重要：先注册 pinia，再注册 router
app.use(pinia)
app.use(router)
app.use(Antd)
app.mount('#app')
