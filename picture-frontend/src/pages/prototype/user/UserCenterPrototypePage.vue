<template>
  <div class="center-prototype">
    <div v-if="loading" class="center-skeleton" aria-label="正在加载个人中心">
      <a-skeleton active :paragraph="{ rows: 2 }" />
      <div class="center-skeleton-grid">
        <a-skeleton avatar active :paragraph="{ rows: 8 }" />
        <a-skeleton active :paragraph="{ rows: 10 }" />
      </div>
    </div>

    <a-result
      v-else-if="centerError"
      status="error"
      title="个人中心暂时无法加载"
      :sub-title="centerError"
    >
      <template #extra>
        <a-button class="proto-button acid-button" type="primary" @click="loadCenter()">重新加载</a-button>
      </template>
    </a-result>

    <div v-else-if="loginUserStore.loginUser" class="center-content">
      <!-- 页面标题已经由公共顶栏提供，这里只保留必要的退出操作。 -->
      <div class="center-top-actions">
        <a-button class="proto-button ghost-button" :loading="logoutLoading" @click="logout">退出登录</a-button>
      </div>

      <section class="center-layout proto-section">
        <aside class="profile-panel">
          <div class="profile-identity">
            <div class="profile-avatar-wrap">
              <a-avatar :size="160" :src="user.avatarurl">{{ displayName.charAt(0) }}</a-avatar>
            </div>
            <h2>{{ displayName }}</h2>
            <p class="profile-account">{{ user.useraccount || '未填写账号' }}</p>
            <p class="profile-bio">{{ user.profile || '还没有填写个人简介。' }}</p>
            <a-tag class="proto-status pass">{{ roleText }}</a-tag>
          </div>

          <a-button class="proto-button ghost-button profile-edit" @click="editOpen = true">
            编辑个人资料
          </a-button>

          <div class="profile-details">
            <div><span>性别</span><strong>{{ genderText }}</strong></div>
            <div><span>手机号</span><strong>{{ user.phone || '未填写' }}</strong></div>
            <div><span>邮箱</span><strong>{{ user.email || '未填写' }}</strong></div>
            <div><span>注册时间</span><strong>{{ formatDate(user.createtime) }}</strong></div>
          </div>
        </aside>

        <main class="center-main">
          <div class="center-main-heading">
            <h2>账户概览</h2>
          </div>

          <div class="center-stat-row">
            <div class="center-stat proto-surface">
              <span>我的图片</span>
              <strong>{{ summaryLoading ? '—' : pictureTotal }}</strong>
              <small>全部审核状态</small>
            </div>
            <div class="center-stat proto-surface">
              <span>待审核</span>
              <strong>{{ summaryLoading ? '—' : pendingTotal }}</strong>
              <small>等待管理员处理</small>
            </div>
            <div class="center-stat proto-surface">
              <span>私人空间</span>
              <strong>{{ hasPrivateSpace ? 1 : 0 }}</strong>
              <small>{{ hasPrivateSpace ? '已创建' : '尚未创建' }}</small>
            </div>
          </div>

          <div class="center-actions">
            <RouterLink to="/gallery/manage" class="center-action-card dark proto-surface bento-link">
              <div>
                <h3>管理我的图片</h3>
              </div>
              <span class="center-action-arrow" aria-hidden="true">↗</span>
            </RouterLink>
            <RouterLink to="/space" class="center-action-card acid proto-surface bento-link">
              <div>
                <h3>打开个人空间</h3>
              </div>
              <span class="center-action-arrow" aria-hidden="true">↗</span>
            </RouterLink>
          </div>

          <div class="center-recent-grid">
            <section v-for="panel in recentPanels" :key="panel.key" class="center-recent proto-surface">
              <div class="center-recent-head">
                <div class="center-recent-title">
                  <h3>{{ panel.title }}</h3>
                  <span>{{ panel.subtitle }}</span>
                </div>
                <a-button type="link" @click="openRecentPanel(panel.key)">查看全部</a-button>
              </div>

              <a-skeleton v-if="recentLoading" active :paragraph="{ rows: 3 }" />
              <a-alert
                v-else-if="recentError"
                type="error"
                show-icon
                :message="recentError"
              >
                <template #action>
                  <a-button size="small" @click="loadRecentPictures(user.spaceId,user.id)">重试</a-button>
                </template>
              </a-alert>
              <a-empty v-else-if="!panel.pictures.length" :description="panel.emptyText">
                <a-button
                  v-if="panel.key === 'private' && !hasPrivateSpace"
                  class="proto-button acid-button"
                  type="primary"
                  @click="router.push('/space')"
                >
                  创建空间
                </a-button>
                <a-button
                  v-else
                  class="proto-button acid-button"
                  type="primary"
                  @click="router.push('/gallery/upload')"
                >
                  上传图片
                </a-button>
              </a-empty>
              <div v-else class="recent-list">
                <div v-for="picture in panel.pictures" :key="String(picture.id)" class="recent-row">
                  <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '图片预览'" />
                  <div>
                    <strong>{{ picture.name || '未命名图片' }}</strong>
                    <span>{{ formatDate(picture.createtime) }}</span>
                  </div>
                  <a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">
                    {{ pictureStatusText(picture.pictureCheck) }}
                  </a-tag>
                </div>
              </div>
            </section>
          </div>
        </main>
      </section>
    </div>

    <ProfileEditModal
      v-model:open="editOpen"
      :user="user"
      @saved="handleProfileSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, userLogout } from '../../../api/userController'
import { queryPicturePage } from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { pictureStatusText } from '../prototypeData'
import ProfileEditModal from './components/ProfileEditModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(true)
const summaryLoading = ref(false)
const logoutLoading = ref(false)
const editOpen = ref(false)
const centerError = ref('')
const summaryError = ref('')
const pictureTotal = ref(0)
const pendingTotal = ref(0)
const recentLoading = ref(false)
const recentError = ref('')
const publicRecentPictures = ref<API.PictureVO[]>([])
const privateRecentPictures = ref<API.PictureVO[]>([])
const emptyUser: API.UserVO = { username: '', useraccount: '', gender: undefined, phone: '', email: '', profile: '', avatarurl: '', spaceId: undefined }
const user = computed<API.UserVO>(() => loginUserStore.loginUser || emptyUser)
const displayName = computed(() => user.value.username || '图库用户')
const roleText = computed(() => user.value.userLevel === 'admin' ? '管理员' : user.value.userLevel === 'vip' ? 'VIP 用户' : '普通用户')
const genderText = computed(() => user.value.gender === 0 ? '男' : user.value.gender === 1 ? '女' : '未填写')
const hasPrivateSpace = computed(() => user.value.spaceId != null && String(user.value.spaceId) !== '0')

type RecentPanelKey = 'public' | 'private'

const recentPanels = computed(() => [
  {
    key: 'public' as RecentPanelKey,
    title: '公共图库',
    subtitle: '最近上传',
    pictures: publicRecentPictures.value,
    emptyText: '公共图库暂无图片',
  },
  {
    key: 'private' as RecentPanelKey,
    title: '个人空间',
    subtitle: hasPrivateSpace.value ? '最近上传' : '尚未创建',
    pictures: privateRecentPictures.value,
    emptyText: hasPrivateSpace.value ? '个人空间暂无图片' : '还没有个人空间',
  },
])

function statusClass(status?: number) { return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait' }

function formatDate(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').replace(/\.\d+.*$/, '')
}

function parseTotal(value?: number | string) {
  const total = Number(value || 0)
  return Number.isFinite(total) ? total : 0
}

/**
 * 按审核状态和空间分别查询，保证公共图库与私人空间的统计都不会混淆。
 * 公共图库固定传0；后端对历史请求中的null只做兜底，不依赖前端传null。
 */
async function loadPictureSummary(userId?: number | string, spaceId?: number | string) {
  if (!userId) {
    pictureTotal.value = 0
    pendingTotal.value = 0
    return
  }

  summaryLoading.value = true
  summaryError.value = ''
  try {
    const statuses = [0, 1, 2] as const
    const scopeIds: Array<number | string> = [0]
    if (spaceId != null && String(spaceId) !== '0') {
      scopeIds.push(spaceId)
    }

    const requests = scopeIds.flatMap((scopeId) =>
      statuses.map((pictureCheck) => ({ scopeId, pictureCheck })),
    )
    const responses = await Promise.all(
      requests.map(async ({ scopeId, pictureCheck }) => ({
        pictureCheck,
        response: await queryPicturePage({
          userId,
          // 公共图库必须显式传0，私人空间只传当前用户的空间id。
          spaceId: scopeId,
          pictureCheck,
          current: 1,
          // 统计只需要total，取1条即可，避免无意义地拉取图片列表。
          pageSize: 1,
          sortFiled: 'createtime',
          sortOrder: 'descend',
        }),
      })),
    )

    responses.forEach(({ response }) => {
      if (response.data?.code !== 200) {
        throw new Error(response.data?.message || '图片概览加载失败')
      }
    })

    pictureTotal.value = responses.reduce(
      (total, { response }) => total + parseTotal(response.data.data?.total),
      0,
    )
    pendingTotal.value = responses
      .filter(({ pictureCheck }) => pictureCheck === 0)
      .reduce((total, { response }) => total + parseTotal(response.data.data?.total), 0)
  } catch (error: any) {
    summaryError.value = error?.response?.data?.message || error?.message || '图片概览加载失败'
  } finally {
    summaryLoading.value = false
  }
}

/**
 * 用户中心分别拉取公共图库和当前私人空间的最近图片，两个面板各自渲染。
 */
async function loadRecentPictures(spaceId?: number | string, userId?: number | string) {
  recentLoading.value = true
  recentError.value = ''
  try {
    const queryBodies: API.PictureQueryRequest[] = [
      {
        userId,
        spaceId: 0,
        pictureCheck: 1,
        current: 1,
        pageSize: 5,
        sortFiled: 'createtime',
        sortOrder: 'descend',
      },
    ]

    if (spaceId != null && String(spaceId) !== '0') {
      queryBodies.push({
        spaceId,
        pictureCheck: 1,
        current: 1,
        pageSize: 5,
        sortFiled: 'createtime',
        sortOrder: 'descend',
      })
    }

    const responses = await Promise.all(queryBodies.map((body) => queryPicturePage(body)))
    responses.forEach((response) => {
      if (response.data?.code !== 200) {
        throw new Error(response.data?.message || '最近上传加载失败')
      }
    })

    publicRecentPictures.value = responses[0].data.data?.pictureList || []
    privateRecentPictures.value = responses[1]?.data.data?.pictureList || []
  } catch (error: any) {
    publicRecentPictures.value = []
    privateRecentPictures.value = []
    recentError.value = error?.response?.data?.message || error?.message || '最近上传加载失败'
  } finally {
    recentLoading.value = false
  }
}

function openRecentPanel(scope: RecentPanelKey) {
  void router.push(scope === 'public' ? '/gallery' : '/space')
}

async function loadCenter(showSkeleton = true) {
  if (showSkeleton) loading.value = true
  centerError.value = ''
  try {
    const res = await getCurrentUser()
    const currentUser = res.data?.code === 200 ? res.data.data : undefined
    if (!currentUser) {
      if (res.data?.code === 40100) {
        loginUserStore.clearLoginUser()
        await router.replace({ path: '/user/login', query: { redirect: '/user/center' } })
      } else {
        centerError.value = res.data?.message || '未能获取当前用户信息'
      }
      return
    }
    loginUserStore.setLoginUser(currentUser)
    await Promise.all([
      loadPictureSummary(currentUser.id, currentUser.spaceId),
      loadRecentPictures(currentUser.spaceId,currentUser.id),
    ])
  } catch (error: any) {
    const unauthorized = error?.response?.status === 401 || error?.response?.data?.code === 40100
    if (unauthorized) {
      loginUserStore.clearLoginUser()
      await router.replace({ path: '/user/login', query: { redirect: '/user/center' } })
    } else {
      centerError.value = error?.response?.data?.message || '获取当前用户失败，请确认后端服务已启动'
    }
  } finally {
    loading.value = false
  }
}

async function handleProfileSaved() {
  // 重新读取后端用户，确保头像、资料和全局登录态同步更新。
  await loadCenter(false)
}

async function logout() {
  logoutLoading.value = true
  try {
    const res = await userLogout()
    if (res.data?.code !== 200) {
      message.error(res.data?.message || '退出登录失败')
      return
    }
    loginUserStore.clearLoginUser()
    message.success('已退出登录')
    await router.replace('/user/login')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '退出登录请求失败')
  } finally {
    logoutLoading.value = false
  }
}

onMounted(loadCenter)
</script>

<style scoped>
.center-prototype { min-height: 100%; }
.center-skeleton { padding-top: 18px; }
.center-skeleton-grid { margin-top: 16px; display: grid; grid-template-columns: minmax(0, .3fr) minmax(0, .7fr); gap: var(--prototype-layout-gap); }
.center-skeleton-grid > * { min-height: 340px; padding: 20px; background: rgba(255,255,255,.45); border: 1px solid var(--proto-line); border-radius: 10px; }
.center-content { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.center-top-actions { display: flex; justify-content: flex-end; min-height: 43px; }
.center-layout.proto-section { flex: 1 1 auto; min-height: 0; padding-top: 0; }
.center-layout { display: grid; grid-template-columns: minmax(0, .3fr) minmax(0, .7fr); gap: var(--prototype-layout-gap); align-items: start; }

/* 左侧只承担身份识别，避免把资料和统计信息挤在同一张大卡片里。 */
.profile-panel { min-width: 0; padding: 4px 10px 0 0; }
.profile-identity { text-align: center; }
.profile-avatar-wrap { display: flex; justify-content: center; margin-bottom: 12px; }
.profile-avatar-wrap :deep(.ant-avatar) { border: 1px solid var(--proto-line-strong); background: rgba(255,255,255,.68); color: var(--proto-ink); font-size: 48px; }
.profile-panel h2 { margin: 0; font-size: 32px; font-weight: 800; line-height: 1.15; letter-spacing: -.06em; }
.profile-account { margin: 7px 0 0; color: var(--proto-muted); font-family: inherit; font-size: 15px; font-weight: 400; line-height: 1.35; }
.profile-bio { max-width: 28ch; min-height: 32px; margin: 14px auto 0; color: var(--proto-muted); font-size: 12px; line-height: 1.55; }
.profile-panel :deep(.proto-status) { margin-top: 12px; }
.profile-edit { width: 100%; margin-top: 16px; }
.profile-details { margin-top: 16px; border-top: 1px solid var(--proto-line); }
.profile-details div { min-height: 44px; padding: 8px 0; display: flex; flex-direction: column; justify-content: space-between; border-bottom: 1px solid var(--proto-line); }
.profile-details span { color: var(--proto-muted); font-size: 10px; }
.profile-details strong { overflow-wrap: anywhere; font-size: 11px; }

.center-main { min-width: 0; min-height: 100%; display: flex; flex-direction: column; gap: 12px; }
.center-main-heading { display: flex; align-items: center; justify-content: space-between; }
.center-main-heading h2 { margin: 0; font-size: 20px; letter-spacing: -.04em; }
.center-stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.center-stat { min-height: 104px; padding: 16px 18px; display: flex; flex-direction: column; justify-content: space-between; }
.center-stat span { color: var(--proto-muted); font-size: 11px; }
.center-stat strong { font-size: 30px; letter-spacing: -.07em; }
.center-stat small { color: var(--proto-muted); font-family: inherit; font-size: 10px; }
.center-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }

/* 快捷入口使用 GitHub 式的轻量边框卡片，保留入口但降低视觉噪声。 */
.center-action-card { min-width: 0; min-height: 78px; padding: 14px 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; color: var(--proto-ink); text-decoration: none; transition: border-color .2s ease, box-shadow .2s ease, background .2s ease; }
.center-action-card:hover { color: var(--proto-ink); border-color: var(--proto-acid); background: rgba(255,255,255,.78); box-shadow: 0 12px 26px rgba(18,23,23,.08); }
.center-action-card.dark { background: var(--proto-ink); border-color: var(--proto-ink); color: var(--proto-paper); }
.center-action-card.dark .center-action-arrow { color: var(--proto-acid); }
.center-action-card.dark:hover { background: #202526; color: var(--proto-paper); border-color: #202526; }
.center-action-card.acid { background: var(--proto-acid); border-color: var(--proto-acid); color: var(--proto-ink); }
.center-action-card.acid:hover { background: #c5ff55; color: var(--proto-ink); border-color: #c5ff55; }
.center-action-card h3 { margin: 0; font-size: 17px; letter-spacing: -.04em; }
.center-action-arrow { flex: 0 0 auto; color: var(--proto-orange); font-size: 20px; line-height: 1; }
.center-recent-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.center-recent { min-width: 0; min-height: 220px; padding: 16px; }
.center-recent-head { display: flex; align-items: center; justify-content: space-between; padding-bottom: 7px; border-bottom: 1px solid var(--proto-line); }
.center-recent-title { min-width: 0; display: flex; align-items: baseline; gap: 8px; }
.center-recent-head h3 { margin: 0; font-size: 18px; font-weight: 800; letter-spacing: -.04em; }
.center-recent-title span { color: var(--proto-muted); font-size: 10px; }
.center-recent-head :deep(.ant-btn-link) { padding-inline: 0; color: var(--proto-ink); font-size: 11px; }
.center-recent :deep(.ant-skeleton),
.center-recent :deep(.ant-alert),
.center-recent :deep(.ant-empty) { margin-top: 12px; }
.center-recent :deep(.ant-empty) { margin-block: 14px 4px; }
.recent-row { min-height: 56px; display: grid; grid-template-columns: 50px 1fr auto; gap: 10px; align-items: center; border-bottom: 1px solid var(--proto-line); }
.recent-row:last-child { border-bottom: 0; }
.recent-row img { width: 50px; height: 40px; object-fit: cover; border-radius: 4px; }
.recent-row strong, .recent-row span { display: block; }
.recent-row strong { font-size: 12px; }
.recent-row span { margin-top: 3px; color: var(--proto-muted); font-family: inherit; font-size: 10px; }
@media (max-width: 980px) {
  .center-layout, .center-skeleton-grid { gap: var(--prototype-layout-gap); grid-template-columns: minmax(0, .3fr) minmax(0, .7fr); }
}
@media (max-width: 760px) {
  .center-content { height: auto; }
  .center-layout.proto-section { flex: none; padding-top: 18px; }
  .center-layout, .center-skeleton-grid { grid-template-columns: 1fr; }
  .center-recent-grid { grid-template-columns: 1fr; }
  .center-top-actions { justify-content: flex-start; }
  .profile-panel { padding-right: 0; }
  .profile-details { display: grid; grid-template-columns: 1fr 1fr; column-gap: 18px; }
}
@media (max-width: 520px) {
  .center-stat-row, .center-actions { grid-template-columns: 1fr; }
  .profile-details { display: block; }
}
</style>
