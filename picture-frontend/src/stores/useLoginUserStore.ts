import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUserUsingGet } from '../api/userController'

/**
 * 存储登录用户信息
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  // 定义状态
  const loginUser = ref<API.User | null>(null)

  // 定义方法
  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }

  function clearLoginUser(){
    loginUser.value = null
  }

  // 从后端获取到当前用户信息
  async function fetchLoginUser() {
    try {
      const res = await getCurrentUserUsingGet()
      const userData = res?.data?.data
      if(userData){
        setLoginUser(userData)
      }else{
        setLoginUser(null)
      }
    } catch (error) {
      // 网络错误或其他异常时，不阻止页面访问
      console.log('获取用户信息失败', error)
      setLoginUser(null)
    }
  }

  // 必须 return 出去！
  return {
    loginUser,
    setLoginUser,
    fetchLoginUser,
    clearLoginUser
  }
})