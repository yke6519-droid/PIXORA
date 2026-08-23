import type { Router } from 'vue-router'

/**
 * 返回用户刚才所在的页面。
 * 直接打开页面时没有应用内历史记录，这时才使用兜底地址。
 */
export function goBack(router: Router, fallbackPath: string) {
  if (window.history.state?.back) {
    router.back()
    return
  }
  void router.replace(fallbackPath)
}
