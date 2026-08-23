import routes from './route'
import { createRouter, createWebHistory } from 'vue-router'
const router = createRouter({
    history: createWebHistory(),
    routes,
    // 浏览器后退时恢复进入详情页前的滚动位置。
    scrollBehavior(_to, _from, savedPosition) {
        return savedPosition || { top: 0 }
    },
})

export default router
