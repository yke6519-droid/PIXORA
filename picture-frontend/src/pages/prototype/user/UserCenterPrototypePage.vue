<template>
  <div class="center-prototype">
    <a-spin :spinning="loading">
      <div v-if="loginUserStore.loginUser" class="center-content">
        <section class="proto-page-head">
          <div>
            <span class="proto-eyebrow">用户中心 / getCurrentUser + updateSelf</span>
            <h1 class="proto-title">你的账户，<br />也应该像图库一样有秩序。</h1>
            <p class="proto-copy">页面展示当前用户脱敏后的资料，并接入真实的当前用户、资料更新和退出登录接口。</p>
          </div>
          <a-button class="proto-button ghost-button" :loading="logoutLoading" @click="logout">退出登录</a-button>
        </section>

        <section class="center-grid proto-section">
          <article class="profile-card proto-surface proto-rounded">
            <div class="profile-card-top"><a-avatar :size="78" :src="user.avatarurl">{{ displayName.charAt(0) }}</a-avatar><a-tag class="proto-status pass">{{ roleText }}</a-tag></div>
            <h2>{{ displayName }}</h2><p class="profile-account">{{ user.useraccount || '未填写账号' }}</p><p class="profile-bio">{{ user.profile || '还没有填写个人简介。' }}</p>
            <div class="profile-details"><div><span>性别</span><strong>{{ user.gender === 0 ? '男士' : user.gender === 1 ? '女士' : '未填写' }}</strong></div><div><span>手机号</span><strong>{{ user.phone || '未填写' }}</strong></div><div><span>邮箱</span><strong>{{ user.email || '未填写' }}</strong></div><div><span>注册时间</span><strong>{{ user.createtime || '—' }}</strong></div></div>
            <a-button class="proto-button acid-button profile-edit" type="primary" @click="openEdit">编辑个人资料</a-button>
          </article>

          <div class="center-side">
            <div class="center-stat-row"><div class="center-stat proto-surface"><span>我的图片</span><strong>{{ ownPictures.length }}</strong><small>queryPicturePageCache</small></div><div class="center-stat proto-surface"><span>待审核</span><strong>{{ pendingCount }}</strong><small>pictureCheck = 0</small></div><div class="center-stat proto-surface"><span>私人空间</span><strong>{{ user.spaceId ? 1 : 0 }}</strong><small>spaceId = {{ user.spaceId || '—' }}</small></div></div>
            <div class="center-actions proto-bento"><RouterLink to="/prototype/gallery/manage" class="proto-bento-card dark bento-link"><h3>管理我的图片</h3><p>查看审核状态、拒绝原因和重新上传入口。</p><span class="bento-corner">picture/manage</span></RouterLink><RouterLink to="/prototype/space" class="proto-bento-card acid bento-link"><h3>打开个人空间</h3><p>查看空间容量和数量使用情况。</p><span class="bento-corner">my-space</span></RouterLink></div>
            <div class="center-recent proto-surface"><div class="center-recent-head"><h3>最近上传</h3><a-button type="link" @click="router.push('/prototype/gallery/manage')">查看全部</a-button></div><a-empty v-if="!ownPictures.length" description="暂无图片" /><div v-for="picture in ownPictures.slice(0, 3)" :key="picture.id" class="recent-row"><img :src="picture.thumbnailUrl || picture.url" :alt="picture.name" /><div><strong>{{ picture.name || '未命名图片' }}</strong><span>{{ picture.createtime || '—' }}</span></div><a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">{{ pictureStatusText(picture.pictureCheck) }}</a-tag></div></div>
          </div>
        </section>
      </div>
    </a-spin>

    <a-modal v-model:open="editOpen" title="编辑个人资料" ok-text="保存资料" cancel-text="取消" :confirm-loading="saving" @ok="saveProfile">
      <a-form :model="editForm" layout="vertical" class="proto-form">
        <a-form-item label="用户名 username" required><a-input v-model:value="editForm.username" /></a-form-item>
        <a-form-item label="性别 gender"><a-radio-group v-model:value="editForm.gender"><a-radio :value="0">男士</a-radio><a-radio :value="1">女士</a-radio></a-radio-group></a-form-item>
        <a-form-item label="手机号 phone"><a-input v-model:value="editForm.phone" /></a-form-item>
        <a-form-item label="邮箱 email"><a-input v-model:value="editForm.email" /></a-form-item>
        <a-form-item label="个人简介 profile"><a-textarea v-model:value="editForm.profile" :rows="4" /></a-form-item>
        <a-form-item label="头像上传审核预留"><div class="avatar-edit-row"><a-avatar :size="44" :src="pendingAvatarPreview || user.avatarurl">{{ displayName.charAt(0) }}</a-avatar><a-upload :before-upload="beforeAvatarUpload" :show-upload-list="false" accept=".jpg,.jpeg,.png,.webp"><a-button class="proto-button ghost-button">选择头像文件</a-button></a-upload></div><small class="avatar-permission-note">头像文件暂存为待审核预览，不会绕过审核直接更新 avatarurl。</small></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getCurrentUserUsingGet, updateSelfUsingPost, userLogoutUsingGet } from '../../../api/userController'
import { queryPicturePageCacheUsingPost } from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { pictureStatusText } from '../prototypeData'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(true)
const saving = ref(false)
const logoutLoading = ref(false)
const editOpen = ref(false)
const pendingAvatarPreview = ref('')
const ownPictures = ref<API.PictureVO[]>([])
const emptyUser: API.User = { username: '', useraccount: '', gender: undefined, phone: '', email: '', profile: '', avatarurl: '', spaceId: undefined }
const user = computed<API.User>(() => loginUserStore.loginUser || emptyUser)
const displayName = computed(() => user.value.username || '图库用户')
const roleText = computed(() => user.value.userstatus === 'admin' ? '管理员' : user.value.userstatus === 'vip' ? 'VIP 用户' : '普通用户')
const pendingCount = computed(() => ownPictures.value.filter((picture) => picture.pictureCheck === 0).length)
const editForm = reactive<API.UpdateSelfRequest>({ username: '', gender: 0, phone: '', email: '', profile: '' })

function statusClass(status?: number) { return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait' }

function syncEditForm(currentUser: API.User) {
  editForm.username = currentUser.username || ''
  editForm.gender = currentUser.gender ?? 0
  editForm.phone = currentUser.phone || ''
  editForm.email = currentUser.email || ''
  editForm.profile = currentUser.profile || ''
}

async function loadOwnPictures(userId?: number) {
  if (!userId) {
    ownPictures.value = []
    return
  }
  const res = await queryPicturePageCacheUsingPost({ userId, current: 1, pageSize: 8 })
  if (res.data?.code === 200) ownPictures.value = res.data.data?.pictureList || []
}

async function loadCenter() {
  loading.value = true
  try {
    const res = await getCurrentUserUsingGet()
    const currentUser = res.data?.code === 200 ? res.data.data : undefined
    if (!currentUser) {
      message.warning(res.data?.message || '请先登录')
      await router.replace({ path: '/prototype/user/login', query: { redirect: '/prototype/user/center' } })
      return
    }
    loginUserStore.setLoginUser(currentUser)
    syncEditForm(currentUser)
    await loadOwnPictures(currentUser.id)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '获取当前用户失败，请确认后端服务已启动')
    await router.replace({ path: '/prototype/user/login', query: { redirect: '/prototype/user/center' } })
  } finally {
    loading.value = false
  }
}

function openEdit() {
  syncEditForm(user.value)
  pendingAvatarPreview.value = ''
  editOpen.value = true
}

async function saveProfile() {
  if (!editForm.username?.trim()) {
    message.error('用户名不能为空')
    return
  }
  if (editForm.phone && !/^1[3-9]\d{9}$/.test(editForm.phone)) {
    message.error('请输入有效的 11 位手机号')
    return
  }
  saving.value = true
  try {
    const res = await updateSelfUsingPost({
      username: editForm.username,
      gender: editForm.gender,
      phone: editForm.phone,
      email: editForm.email,
      profile: editForm.profile,
    })
    if (res.data?.code !== 200) {
      message.error(res.data?.message || '资料更新失败')
      return
    }
    await loginUserStore.fetchLoginUser()
    if (loginUserStore.loginUser) {
      syncEditForm(loginUserStore.loginUser)
      await loadOwnPictures(loginUserStore.loginUser.id)
    }
    pendingAvatarPreview.value = ''
    editOpen.value = false
    message.success('个人资料已更新')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '资料更新失败')
  } finally {
    saving.value = false
  }
}

async function logout() {
  logoutLoading.value = true
  try {
    const res = await userLogoutUsingGet()
    if (res.data?.code === 200) message.success('已退出登录')
    else message.error(res.data?.message || '退出登录失败')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '退出登录请求失败')
  } finally {
    loginUserStore.clearLoginUser()
    logoutLoading.value = false
    await router.replace('/prototype/user/login')
  }
}

function beforeAvatarUpload(file: any) {
  if (!file?.type?.startsWith('image/')) {
    message.error('只能选择图片文件')
    return false
  }
  if (file.size / 1024 / 1024 >= 5) {
    message.error('头像大小不能超过 5MB')
    return false
  }
  pendingAvatarPreview.value = URL.createObjectURL(file)
  message.info('头像已暂存，等待头像审核接口接入')
  return false
}

onMounted(loadCenter)
</script>

<style scoped>
.center-prototype { height: 100%; min-height: 0; overflow: hidden; }
.center-prototype :deep(.ant-spin-nested-loading), .center-prototype :deep(.ant-spin-container) { height: 100%; }
.center-content { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.center-content > .proto-page-head { padding-top: 4px; }
.center-content > .proto-page-head .proto-title { margin-top: 8px; margin-bottom: 10px; font-size: clamp(32px, 4.2vw, 58px); }
.center-content > .proto-page-head .proto-copy { font-size: 12px; line-height: 1.55; }
.center-grid.proto-section { flex: 1 1 auto; min-height: 0; padding-top: 14px; }
.center-grid { display: grid; grid-template-columns: minmax(290px, .68fr) minmax(450px, 1.32fr); gap: 10px; align-items: stretch; }
.profile-card { padding: 20px; }
.profile-card-top { display: flex; align-items: flex-start; justify-content: space-between; }
.profile-card h2 { margin: 12px 0 0; font-size: 30px; letter-spacing: -.07em; }
.profile-account { margin: 4px 0 10px; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; }
.profile-bio { min-height: 32px; color: var(--proto-muted); font-size: 11px; line-height: 1.55; }
.profile-details { margin: 18px 0; display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--proto-line); }
.profile-details div { min-height: 52px; padding: 9px; display: flex; flex-direction: column; justify-content: space-between; background: rgba(255,255,255,.42); }
.profile-details span { color: var(--proto-muted); font-size: 10px; }
.profile-details strong { font-size: 10px; }
.profile-edit { width: 100%; }
.center-side { min-height: 0; display: flex; flex-direction: column; gap: 10px; }
.center-stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.center-stat { min-height: 86px; padding: 13px; display: flex; flex-direction: column; justify-content: space-between; }
.center-stat span { color: var(--proto-muted); font-size: 10px; }
.center-stat strong { font-size: 28px; letter-spacing: -.09em; }
.center-stat small { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 8px; }
.center-actions { gap: 10px; }
.center-actions .proto-bento-card { min-height: 108px; padding: 18px; }
.center-actions .proto-bento-card h3 { font-size: 18px; }
.center-recent { min-height: 0; padding: 14px; }
.center-recent-head { display: flex; align-items: center; justify-content: space-between; padding-bottom: 7px; border-bottom: 1px solid var(--proto-line); }
.center-recent-head h3 { margin: 0; font-size: 15px; letter-spacing: -.04em; }
.center-recent-head :deep(.ant-btn-link) { padding-inline: 0; color: var(--proto-ink); font-size: 11px; }
.recent-row { min-height: 53px; display: grid; grid-template-columns: 44px 1fr auto; gap: 9px; align-items: center; border-bottom: 1px solid var(--proto-line); }
.recent-row:last-child { border-bottom: 0; }
.recent-row img { width: 44px; height: 36px; object-fit: cover; }
.recent-row strong, .recent-row span { display: block; }
.recent-row strong { font-size: 11px; }
.recent-row span { margin-top: 3px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 8px; }
.avatar-edit-row { display: flex; align-items: center; gap: 13px; margin-bottom: 10px; }
.avatar-permission-note { display: block; margin-top: 7px; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 9px; line-height: 1.5; }
@media (max-width: 850px) { .center-prototype { height: auto; overflow: visible; } .center-prototype :deep(.ant-spin-nested-loading), .center-prototype :deep(.ant-spin-container) { height: auto; } .center-content { height: auto; } .center-grid.proto-section { flex: none; padding-top: 18px; } .center-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .center-stat-row { grid-template-columns: 1fr; } }
</style>
