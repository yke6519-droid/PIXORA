/**
 * 全局检测
 * 检验当前登录用户是否有权限进入该页面
 */
import router from './config/router'
import { useLoginUserStore } from './stores/useLoginUserStore'
import { message } from 'ant-design-vue'

let firstLoginFlag = true

/**
 * 全局权限校验功能
 */
router.beforeEach(async (to) => {
  const loginUserStore = useLoginUserStore()

  // 首次进入应用时等待 Session 校验完成，避免管理员刷新页面后被误判为未登录。
  if (firstLoginFlag) {
    firstLoginFlag = false
    await loginUserStore.fetchLoginUser()
  }

  const loginUser = loginUserStore.loginUser
  const requiresAdmin = Boolean(to.meta.requiresAdmin)
  const requiresAuth = Boolean(to.meta.requiresAuth || requiresAdmin)

  if (requiresAuth && !loginUser) {
    message.error('请先登录')
    return {
      path: '/user/login',
      query: { redirect: to.fullPath },
    }
  }

  if (requiresAdmin && loginUser?.userLevel !== 'admin') {
    message.error('当前用户没有管理员权限')
    return '/gallery'
  }

  return true
})
