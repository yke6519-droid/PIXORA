<template>
  <div class="register-prototype">
    <section class="register-intro">
      <div class="register-intro-image"><img src="https://picsum.photos/seed/auth-architecture/900/1200" alt="注册页视觉封面" /></div>
      <div class="register-intro-copy">
        <span class="register-kicker"> </span>
        <h1 class="proto-title">给你的图片<br />一个私人入口。</h1>
      </div>
    </section>
    <a-card class="register-card proto-surface" :bordered="false">
      <div class="register-card-head">
        <a-typography-title :level="2" class="register-card-title">创建账号</a-typography-title>
      </div>
      <a-form :model="form" layout="vertical" class="proto-form register-form" @finish="submitRegister">
        <!-- 输入项用占位文字说明用途，保留 aria-label 让表单语义和校验提示完整。 -->
        <div class="register-two-col">
          <a-form-item name="username" :rules="usernameRules"><a-input v-model:value="form.username" aria-label="用户名" placeholder="请输入用户名" size="large" /></a-form-item>
          <a-form-item name="useraccount" :rules="accountRules"><a-input v-model:value="form.useraccount" aria-label="账号" placeholder="请输入账号" size="large" /></a-form-item>
        </div>
        <div class="register-two-col">
          <a-form-item name="userpassword" :rules="passwordRules"><a-input-password v-model:value="form.userpassword" aria-label="密码" placeholder="请输入密码" size="large" /></a-form-item>
          <a-form-item name="reUserPassword" :rules="confirmPasswordRules"><a-input-password v-model:value="form.reUserPassword" aria-label="确认密码" placeholder="请再次输入密码" size="large" /></a-form-item>
        </div>
        <div class="register-two-col">
          <a-form-item name="gender" :rules="[{ required: true, message: '请选择性别' }]">
            <a-radio-group v-model:value="form.gender" aria-label="性别" button-style="solid" size="large" class="gender-picker"><a-radio-button :value="0">男</a-radio-button><a-radio-button :value="1">女</a-radio-button></a-radio-group>
          </a-form-item>
          <a-form-item name="phone" :rules="phoneRules"><a-input v-model:value="form.phone" aria-label="手机号" placeholder="请输入手机号" size="large" /></a-form-item>
        </div>
        <div class="register-actions">
          <a-button class="proto-button ghost-button" size="large" @click="router.push('/prototype/user/login')">返回登录</a-button>
          <a-button html-type="submit" :loading="loading" class="proto-button acid-button" type="primary" size="large">创建账号</a-button>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userRegisterUsingPost } from '../../../api/userController'

const router = useRouter()
const loading = ref(false)
const form = reactive<API.RegisterRequest>({ username: '', useraccount: '', userpassword: '', reUserPassword: '', gender: 0, phone: '' })
const usernameRules = [
  { required: true, message: '请输入用户名' },
  { max: 20, message: '用户名不能超过 20 位' },
]
const accountRules = [
  { required: true, message: '请输入账号' },
  { min: 6, max: 20, message: '账号长度为 6-20 位' },
]
const passwordRules = [
  { required: true, message: '请输入密码' },
  { min: 6, max: 20, message: '密码长度为 6-20 位' },
]
const confirmPasswordRules = [
  { required: true, message: '请再次输入密码' },
  { validator: async (_rule: unknown, value: string) => value === form.userpassword ? Promise.resolve() : Promise.reject('两次输入的密码不一致') },
]
const phoneRules = [
  { required: true, message: '请输入手机号' },
  { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的 11 位手机号' },
]

async function submitRegister() {
  loading.value = true
  try {
    const res = await userRegisterUsingPost({
      username: form.username,
      useraccount: form.useraccount,
      gender: form.gender,
      userpassword: form.userpassword,
      reUserPassword: form.reUserPassword,
      phone: form.phone,
    })
    if (res.data?.code !== 200) {
      message.error(res.data?.message || '注册失败')
      return
    }
    message.success('注册成功，请登录')
    await router.replace('/prototype/user/login')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '注册失败，请确认后端服务已启动')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 注册页沿用登录页的字体和三色系统，但保持注册表单的信息密度。 */
.register-prototype {
  --proto-ink: #0d1112;
  --proto-paper: #f6f7f2;
  --proto-acid: #c7ff42;
  --proto-line: rgba(13, 17, 18, .16);
  --proto-muted: #4c5755;
  height: auto;
  min-height: calc(100dvh - var(--prototype-topbar-height) - 20px);
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(0, .94fr);
  gap: var(--prototype-layout-gap);
  align-items: center;
  padding: 16px 0 12px;
  color: var(--proto-ink);
  font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif;
}
.register-intro { align-self: stretch; min-height: 0; height: auto; position: relative; display: flex; align-items: flex-start; overflow: hidden; isolation: isolate; padding: clamp(28px, 3.6vw, 54px); background: var(--proto-ink); color: var(--proto-paper); box-shadow: 0 24px 58px rgba(13, 17, 18, .18); }
.register-intro::after { content: ''; position: absolute; inset: 0; z-index: 1; pointer-events: none; background: linear-gradient(90deg, rgba(13,17,18,.48) 0%, rgba(13,17,18,.14) 66%, rgba(13,17,18,.30) 100%), linear-gradient(180deg, rgba(13,17,18,.12) 0%, rgba(13,17,18,.08) 42%, rgba(13,17,18,.64) 100%); }
.register-intro-image { position: absolute; inset: 0; z-index: 0; opacity: .58; }
.register-intro-image img { width: 100%; height: 100%; display: block; object-fit: cover; mix-blend-mode: luminosity; filter: grayscale(1) contrast(1.16) brightness(.82); }
.register-intro-copy { position: relative; z-index: 2; }
.register-kicker { display: block; margin-bottom: 22px; color: #fff; font-family: 'DM Mono', monospace; font-size: 15px; font-weight: 500; letter-spacing: .08em; line-height: 1.4; }
.register-intro .proto-title { max-width: 700px; margin: 0; color: #fff; font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: clamp(40px, 4.5vw, 62px); font-weight: 800; line-height: .98; letter-spacing: -.04em; white-space: normal; }
.register-card.proto-surface { align-self: stretch; width: 100%; padding: 0; border: 1px solid rgba(13, 17, 18, .16); border-radius: 12px; background: rgba(255, 255, 255, .78); box-shadow: 0 24px 58px rgba(13, 17, 18, .13); }
.register-card :deep(.ant-card-body) { height: 100%; display: flex; flex-direction: column; justify-content: center; padding: clamp(36px, 5vw, 70px) clamp(24px, 3.8vw, 52px); }
.register-card-head { margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--proto-line); }
.register-card-title.ant-typography { margin: 0; color: var(--proto-ink); font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: clamp(32px, 3.4vw, 48px); font-weight: 800; line-height: 1.08; letter-spacing: -.04em; }
.register-card :deep(.ant-form-item-required::before) { display: none !important; }
.register-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.register-two-col :deep(.ant-form-item) { margin-bottom: 16px; }
.register-two-col :deep(.ant-input),
.register-two-col :deep(.ant-input-affix-wrapper) { min-height: 52px; border: 1px solid rgba(13, 17, 18, .16) !important; border-radius: 10px !important; background: rgba(246, 247, 242, .82) !important; box-shadow: none !important; font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: 15px; }
.register-two-col :deep(.ant-input) { padding-inline: 14px; }
.register-two-col :deep(.ant-input-affix-wrapper) { padding: 0 14px; }
.register-two-col :deep(.ant-input-affix-wrapper .ant-input) { min-height: auto; padding-inline: 0; border: 0 !important; background: transparent !important; }
.register-two-col :deep(.ant-input:hover),
.register-two-col :deep(.ant-input-affix-wrapper:hover) { border-color: rgba(13, 17, 18, .38) !important; }
.register-two-col :deep(.ant-input:focus),
.register-two-col :deep(.ant-input-affix-wrapper-focused) { border-color: var(--proto-ink) !important; box-shadow: 0 0 0 3px rgba(199, 255, 66, .28) !important; }
.register-two-col :deep(.ant-input:focus) { border-color: var(--proto-ink) !important; box-shadow: 0 0 0 3px rgba(199, 255, 66, .28) !important; }
.gender-picker { display: flex; width: 100%; min-height: 52px; gap: 8px; }
.gender-picker :deep(.ant-radio-button-wrapper) { flex: 1; height: 52px; padding-inline: 12px; border: 1px solid rgba(13, 17, 18, .16) !important; border-radius: 10px !important; background: rgba(246, 247, 242, .82); color: var(--proto-muted); font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: 15px; line-height: 50px; text-align: center; }
.gender-picker :deep(.ant-radio-button-wrapper::before) { display: none !important; }
.gender-picker :deep(.ant-radio-button-wrapper:hover) { color: var(--proto-ink); }
.gender-picker :deep(.ant-radio-button-wrapper-checked) { border-color: var(--proto-ink) !important; background: var(--proto-ink); color: var(--proto-paper); box-shadow: none; }
.register-actions { display: flex; justify-content: space-between; gap: 12px; margin-top: 8px; padding-top: 18px; border-top: 1px solid var(--proto-line); }
.register-actions .ant-btn { min-width: 116px; height: 50px; border-radius: 10px; font-family: 'Geist', 'Manrope', 'Segoe UI', Arial, sans-serif; font-size: 15px; font-weight: 700; }
.register-actions .acid-button.ant-btn-primary { background: var(--proto-acid); border-color: var(--proto-acid); color: var(--proto-ink); }
.register-actions .acid-button.ant-btn-primary:hover { filter: brightness(.97); box-shadow: 0 12px 24px rgba(13, 17, 18, .12); }
@media (max-width: 980px) { .register-prototype { height: auto; min-height: 0; grid-template-columns: 1fr; gap: 30px; padding: 28px 0 42px; } .register-intro { height: auto; min-height: 330px; } .register-card.proto-surface { align-self: stretch; } .register-card :deep(.ant-card-body) { height: auto; } .register-intro .proto-title { max-width: none; font-size: clamp(42px, 8vw, 64px); white-space: normal; } }
@media (max-width: 560px) { .register-two-col { grid-template-columns: 1fr; gap: 0; } .register-actions { flex-wrap: wrap; } .register-actions .ant-btn { flex: 1 1 140px; } }
</style>
