<template>
  <div class="register-prototype">
    <section class="register-intro">
      <span class="proto-eyebrow">注册账号 / userRegister</span>
      <h1 class="proto-title">给你的图片<br />一个私人入口。</h1>
      <p class="proto-copy">注册接口字段完整保留：账号、用户名、性别、密码、确认密码和手机号。注册成功后返回登录页。</p>
      <div class="register-flow"><div><span>01</span><strong>填写信息</strong></div><i></i><div><span>02</span><strong>创建账号</strong></div><i></i><div><span>03</span><strong>进入图库</strong></div></div>
    </section>
    <section class="register-card proto-surface proto-rounded">
      <div class="register-card-head"><span class="proto-eyebrow">new account</span><h2>创建账号</h2></div>
      <a-form :model="form" layout="vertical" class="proto-form" @finish="submitRegister">
        <div class="register-two-col"><a-form-item name="username" label="用户名 username" :rules="usernameRules"><a-input v-model:value="form.username" /></a-form-item><a-form-item name="useraccount" label="账号 useraccount" :rules="accountRules"><a-input v-model:value="form.useraccount" /></a-form-item></div>
        <div class="register-two-col"><a-form-item name="userpassword" label="密码 userpassword" :rules="passwordRules"><a-input-password v-model:value="form.userpassword" /></a-form-item><a-form-item name="reUserPassword" label="确认密码 reUserPassword" :rules="confirmPasswordRules"><a-input-password v-model:value="form.reUserPassword" /></a-form-item></div>
        <div class="register-two-col"><a-form-item name="gender" label="性别 gender" :rules="[{ required: true, message: '请选择性别' }]"><a-radio-group v-model:value="form.gender"><a-radio :value="0">男士</a-radio><a-radio :value="1">女士</a-radio></a-radio-group></a-form-item><a-form-item name="phone" label="手机号 phone" :rules="phoneRules"><a-input v-model:value="form.phone" /></a-form-item></div>
        <div class="register-actions"><a-button class="proto-button ghost-button" @click="router.push('/prototype/user/login')">返回登录</a-button><a-button html-type="submit" :loading="loading" class="proto-button acid-button" type="primary">创建账号</a-button></div>
      </a-form>
    </section>
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
.register-prototype { height: 100%; min-height: 0; display: grid; grid-template-columns: minmax(300px, .8fr) minmax(450px, 1.2fr); gap: clamp(24px, 5vw, 72px); align-items: center; padding: 20px 0 24px; }
.register-intro .proto-title { font-size: clamp(48px, 7vw, 92px); }
.register-flow { display: flex; align-items: center; gap: 11px; margin-top: 27px; }
.register-flow div { display: flex; flex-direction: column; gap: 5px; }
.register-flow span { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; }
.register-flow strong { font-size: 11px; }
.register-flow i { width: 42px; height: 1px; background: var(--proto-line); }
.register-card { padding: clamp(22px, 3.5vw, 42px); }
.register-card-head h2 { margin: 10px 0 22px; font-size: 35px; letter-spacing: -.07em; }
.register-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.register-actions { display: flex; justify-content: space-between; gap: 10px; margin-top: 14px; }
@media (max-width: 820px) { .register-prototype { height: auto; min-height: 0; grid-template-columns: 1fr; gap: 30px; padding: 28px 0 42px; } }
@media (max-width: 520px) { .register-two-col { grid-template-columns: 1fr; gap: 0; } .register-flow { flex-wrap: wrap; } .register-flow i { width: 20px; } }
</style>
