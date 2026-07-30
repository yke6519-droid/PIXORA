/**
 * 全局检测
 * 检验当前登录用户是否有权限进入该页面
 */
import router from "./config/router";
import { useLoginUserStore } from "./stores/useLoginUserStore";
import { message } from "ant-design-vue";

let firstLoginFlag = true

/**
 * 全局权限校验功能
 */
router.beforeEach(async(to, from, next)=>{
    console.log('路由跳转:', to.fullPath)

    const loginUserStore = useLoginUserStore()

    // 只在首次加载时获取用户信息，不阻塞路由跳转
    if(firstLoginFlag){
        firstLoginFlag = false
        // 异步获取用户信息，不等待
        loginUserStore.fetchLoginUser().catch(() => {
            // 忽略错误，继续跳转
        })
    }

    const loginUser = loginUserStore.loginUser;
    console.log('当前用户:', loginUser)

    const toURL = to.fullPath
    /**
     * 根据你的业务校验逻辑，来限制跳转
     * 比如：某些页面只有管理员才能访问
     */
    if(toURL.startsWith("/admin")){
        console.log('需要管理员权限')
        // 判断是否有权限
        if(!loginUser){
            message.error("用户未登录")
            next(`/user/login?redirect=${to.fullPath}`)
            return
        }
        if(loginUser.userstatus!='admin'){
            message.error("当前用户没有权限")
            next('/')
            return
        }
    }
    // 没有被阻拦 则直接跳转即可
    next()
})
