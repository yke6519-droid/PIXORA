<template>
  <div class="auth-prototype">
    <section class="auth-visual">
      <div class="auth-visual-copy"><span class="proto-eyebrow">智能云图库 / userLogin</span><h1>进入你的<br />图片工作流。</h1><p>登录后可以上传图片、查看审核结果、管理自己的私人空间。</p></div>
      <div class="auth-visual-image"><img src="https://picsum.photos/seed/auth-architecture/900/1200" alt="登录页视觉封面" /></div>
      <div class="auth-visual-index">CLOUD<br />PIC / 2026</div>
    </section>
    <section class="auth-card proto-surface">
      <div class="auth-card-head"><span class="proto-eyebrow">欢迎回来</span><h2>登录图库</h2><p>使用现有后端字段 `useraccount` 与 `userpassword`。</p></div>
      <a-form :model="form" layout="vertical" class="proto-form" @finish="submitLogin">
        <a-form-item name="useraccount" label="账号 useraccount" :rules="accountRules"><a-input v-model:value="form.useraccount" size="large" placeholder="输入账号" /></a-form-item>
        <a-form-item name="userpassword" label="密码 userpassword" :rules="passwordRules"><a-input-password v-model:value="form.userpassword" size="large" placeholder="输入密码" /></a-form-item>
        <div class="auth-help"><span>Session 登录</span><a-button type="link" @click="router.push('/prototype/user/register')">还没有账号？注册</a-button></div>
        <a-button html-type="submit" :loading="loading" class="proto-button acid-button auth-submit" type="primary" size="large">登录</a-button>
      </a-form>
      <div class="auth-contract"><span>登录成功后</span><strong>getCurrentUser</strong><span>刷新当前用户并进入图库</span></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { userLoginUsingPost } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const loading = ref(false)
const form = reactive<API.UserLoginRequest>({ useraccount: '', userpassword: '' })
const accountRules = [
  { required: true, message: '请输入账号' },
  { min: 6, max: 20, message: '账号长度为 6-20 位' },
]
const passwordRules = [
  { required: true, message: '请输入密码' },
  { min: 6, max: 20, message: '密码长度为 6-20 位' },
]

async function submitLogin() {
  loading.value = true
  try {
    const res = await userLoginUsingPost({
      useraccount: form.useraccount,
      userpassword: form.userpassword,
    })
    if (res.data?.code !== 200) {
      message.error(res.data?.message || '登录失败')
      return
    }
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : '/prototype/gallery'
    await router.replace(redirect)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '登录失败，请确认后端服务已启动')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-prototype { height: 100%; min-height: 0; display: grid; grid-template-columns: minmax(0, 1fr) minmax(340px, .72fr); gap: 12px; align-items: stretch; padding: 12px 0 14px; }
.auth-visual { min-height: 0; height: 100%; padding: clamp(22px, 3vw, 42px); display: flex; flex-direction: column; justify-content: space-between; position: relative; overflow: hidden; background: var(--proto-ink); color: var(--proto-paper); }
.auth-visual-copy { position: relative; z-index: 2; }
.auth-visual-copy h1 { margin: 10px 0 14px; font-size: clamp(42px, 6vw, 78px); line-height: .88; letter-spacing: -.09em; }
.auth-visual-copy p { max-width: 330px; color: rgba(241,242,237,.63); font-size: 11px; line-height: 1.55; }
.auth-visual-image { position: absolute; inset: 0; opacity: .46; }
.auth-visual-image img { width: 100%; height: 100%; object-fit: cover; mix-blend-mode: luminosity; filter: contrast(1.2); }
.auth-visual-index { position: relative; z-index: 2; color: var(--proto-acid); font-family: 'DM Mono', monospace; font-size: 10px; line-height: 1.5; }
.auth-card { align-self: center; max-height: 100%; padding: clamp(22px, 3.5vw, 44px); }
.auth-card-head h2 { margin: 10px 0 8px; font-size: clamp(30px, 3.5vw, 46px); letter-spacing: -.08em; }
.auth-card-head p { margin: 0 0 22px; color: var(--proto-muted); font-size: 11px; line-height: 1.55; }
.auth-help { display: flex; align-items: center; justify-content: space-between; margin: -3px 0 12px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
.auth-help :deep(.ant-btn-link) { padding-inline: 0; color: var(--proto-ink); font-size: 11px; }
.auth-submit { width: 100%; }
.auth-contract { display: flex; flex-wrap: wrap; gap: 5px 10px; margin-top: 18px; padding-top: 12px; border-top: 1px solid var(--proto-line); color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.auth-contract strong { color: var(--proto-orange); font-weight: 500; }
@media (max-width: 800px) { .auth-prototype { height: auto; min-height: 0; grid-template-columns: 1fr; padding: 18px 0 30px; } .auth-visual { height: auto; min-height: 330px; } .auth-card { align-self: stretch; max-height: none; } }
</style>
