<template>
  <div class="auth-prototype">
    <section class="auth-visual">
      <div class="auth-visual-copy">
        <p class="auth-visual-brand">PIXORA</p>
        <h1>
          <span class="auth-visual-tagline">让每一张图片，<br />都有清晰的归处。</span>
        </h1>
      </div>
      <div class="auth-visual-image"><img src="https://picsum.photos/seed/auth-architecture/900/1200" alt="登录页视觉封面" /></div>
    </section>
    <a-card class="auth-card proto-surface" :bordered="false">
      <div class="auth-card-head">
        <span class="auth-card-greeting">欢迎回来，继续管理你的图库。</span>
        <a-typography-title :level="2" class="auth-title">登录PIXORA</a-typography-title>
      </div>
      <a-form :model="form" layout="vertical" class="proto-form" @finish="submitLogin">
        <a-form-item name="useraccount" :rules="accountRules"><a-input v-model:value="form.useraccount" aria-label="账号" size="large" placeholder="输入账号" /></a-form-item>
        <a-form-item name="userpassword" :rules="passwordRules"><a-input-password v-model:value="form.userpassword" aria-label="密码" size="large" placeholder="输入密码" /></a-form-item>
        <a-space class="auth-help" :size="0" align="center">
          <a-button type="link" @click="router.push('/user/register')">还没有账号？注册</a-button>
        </a-space>
        <a-button html-type="submit" :loading="loading" class="proto-button acid-button auth-submit" type="primary" size="large">登录</a-button>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { userLogin } from '../../../api/userController'
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
    const res = await userLogin({
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
      : '/gallery'
    await router.replace(redirect)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '登录失败，请确认后端服务已启动')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 登录页单独保留更鲜明的黑、白、酸绿色视觉，避免影响其他页面的业务色彩。 */
.auth-prototype {
  --proto-ink: #0d1112;
  --proto-paper: #f6f7f2;
  --proto-paper-deep: #e7ebe2;
  --proto-acid: #c7ff42;
  --proto-line: rgba(13, 17, 18, .16);
  --proto-muted: #4c5755;
  --proto-shadow: 0 24px 58px rgba(13, 17, 18, .13);
  min-height: calc(100dvh - var(--prototype-topbar-height) - 20px);
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(0, .94fr);
  gap: var(--prototype-layout-gap);
  align-items: stretch;
  padding: 16px 0 12px;
  color: var(--proto-ink);
  font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif;
}
.auth-visual {
  min-height: 0;
  height: 100%;
  padding: clamp(28px, 3.6vw, 54px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
  isolation: isolate;
  background: var(--proto-ink);
  color: var(--proto-paper);
  box-shadow: 0 24px 58px rgba(13, 17, 18, .18);
}
.auth-visual-copy { position: relative; z-index: 2; }
.auth-visual-brand { margin: 0 0 20px; color: #fff; font-family: 'DM Mono', monospace; font-size: 23px; font-weight: 500; letter-spacing: .14em; line-height: 1; }
.auth-visual-copy h1 { margin: 0; color: #fff; font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: clamp(40px, 4.5vw, 60px); font-weight: 700; line-height: .98; letter-spacing: -.04em; }
.auth-visual-tagline { display: block; text-wrap: balance; }
.auth-visual::after { content: ''; position: absolute; inset: 0; z-index: 1; pointer-events: none; background: linear-gradient(90deg, rgba(13,17,18,.46) 0%, rgba(13,17,18,.12) 62%, rgba(13,17,18,.28) 100%), linear-gradient(180deg, rgba(13,17,18,.10) 0%, rgba(13,17,18,.08) 44%, rgba(13,17,18,.62) 100%); }
.auth-visual-image { position: absolute; inset: 0; z-index: 0; opacity: .58; }
.auth-visual-image img { width: 100%; height: 100%; object-fit: cover; mix-blend-mode: luminosity; filter: grayscale(1) contrast(1.16) brightness(.82); }
.auth-card.proto-surface { align-self: stretch; height: 100%; max-height: none; padding: 0; border: 1px solid rgba(13, 17, 18, .16); border-radius: 12px; background: rgba(255, 255, 255, .78); box-shadow: var(--proto-shadow); }
.auth-card :deep(.ant-card-body) { height: 100%; display: flex; flex-direction: column; justify-content: center; padding: clamp(36px, 5vw, 70px) clamp(24px, 3.8vw, 52px); }
.auth-card-head { margin-bottom: clamp(28px, 4vw, 42px); }
.auth-card-greeting { display: block; margin-bottom: 16px; color: var(--proto-muted); font-size: 15px; font-weight: 600; line-height: 1.4; letter-spacing: -.01em; }
.auth-title.ant-typography { margin: 0; color: var(--proto-ink); font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: clamp(32px, 3.5vw, 48px); font-weight: 800; line-height: 1.08; letter-spacing: -.04em; }
.auth-prototype .proto-form :deep(.ant-form-item) { margin-bottom: 22px; }
.auth-prototype .proto-form :deep(.ant-input),
.auth-prototype .proto-form :deep(.ant-input-affix-wrapper) { min-height: 52px; border: 1px solid rgba(13, 17, 18, .18) !important; border-radius: 8px !important; background: rgba(255, 255, 255, .76) !important; box-shadow: none !important; font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: 15px; }
.auth-prototype .proto-form :deep(.ant-input) { padding-inline: 16px; }
.auth-prototype .proto-form :deep(.ant-input-affix-wrapper) { padding: 0 16px; }
.auth-prototype .proto-form :deep(.ant-input-affix-wrapper .ant-input) { min-height: auto; padding-inline: 0; border: 0 !important; background: transparent !important; }
.auth-prototype .proto-form :deep(.ant-input::placeholder) { color: #64706c; opacity: 1; }
.auth-prototype .proto-form :deep(.ant-input:hover),
.auth-prototype .proto-form :deep(.ant-input-affix-wrapper:hover) { border-color: rgba(13, 17, 18, .42) !important; }
.auth-prototype .proto-form :deep(.ant-input:focus),
.auth-prototype .proto-form :deep(.ant-input-affix-wrapper-focused) { border-color: var(--proto-ink) !important; box-shadow: 0 0 0 3px rgba(199, 255, 66, .28) !important; }
.auth-help { display: flex; align-items: center; justify-content: flex-end; margin: -1px 0 18px; color: var(--proto-muted); font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: 12px; }
.auth-help :deep(.ant-btn-link) { padding-inline: 0; color: var(--proto-ink); font-size: 12px; font-weight: 600; }
.auth-help :deep(.ant-btn-link:hover) { color: #4c6800; }
.auth-submit.ant-btn { width: 100%; height: 52px; border-radius: 8px; font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: 16px; font-weight: 700; letter-spacing: .02em; transition: transform .2s ease, filter .2s ease, box-shadow .2s ease; }
.auth-submit.ant-btn:hover { transform: translateY(-2px); filter: brightness(.97); box-shadow: 0 12px 24px rgba(13, 17, 18, .12); }
.auth-submit.ant-btn:active { transform: translateY(0); }
@media (max-width: 980px) { .auth-prototype { min-height: 0; grid-template-columns: 1fr; padding: 18px 0 30px; } .auth-visual { height: auto; min-height: 330px; } .auth-card { height: auto; } .auth-card :deep(.ant-card-body) { height: auto; } .auth-visual-copy h1 { font-size: clamp(32px, 9vw, 40px); } }
</style>
