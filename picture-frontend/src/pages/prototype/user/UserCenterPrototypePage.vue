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
      <section class="center-layout proto-section">
        <aside class="profile-column">
          <section class="profile-card proto-surface">
            <div class="profile-identity">
              <!-- 头像继续复用原有上传弹窗，点击头像即可更新。 -->
              <button type="button" class="profile-avatar-wrap" aria-label="更新头像" @click="avatarOpen = true">
                <a-avatar :size="176" :src="user.avatarurl">{{ displayName.charAt(0) }}</a-avatar>
                <span class="profile-avatar-hint">点击更新头像</span>
              </button>
              <h1>{{ displayName }}</h1>
              <p class="profile-account">@{{ user.useraccount || '未填写账号' }}</p>
              <p class="profile-bio">{{ user.profile || '还没有填写个人简介。' }}</p>
            </div>

            <a-button class="profile-edit" @click="editOpen = true">
              <span>编辑个人资料</span>
              <RightOutlined />
            </a-button>

            <!-- 统计数据从已有接口读取，不新增任何后端字段。 -->
            <div class="profile-stats" aria-label="账户统计">
              <div>
                <strong>{{ summaryLoading ? '—' : pictureTotal }}</strong>
                <span>图片</span>
              </div>
              <div>
                <strong>{{ hasPrivateSpace ? 1 : 0 }}</strong>
                <span>空间</span>
              </div>
              <div>
                <strong>{{ summaryLoading ? '—' : pendingTotal }}</strong>
                <span>待审核</span>
              </div>
            </div>
          </section>

          <!-- 先把后续功能的入口位置固定下来，暂未接入收藏、回收站等接口。 -->
          <nav class="profile-menu proto-surface" aria-label="用户中心菜单">
            <RouterLink to="/user/center" class="profile-menu-item is-active">
              <HomeOutlined />
              <span>概览</span>
            </RouterLink>
            <RouterLink to="/gallery/manage" class="profile-menu-item">
              <PictureOutlined />
              <span>我的图片</span>
            </RouterLink>
            <RouterLink to="/space" class="profile-menu-item">
              <FolderOpenOutlined />
              <span>我的空间</span>
            </RouterLink>
            <span class="profile-menu-item is-disabled" aria-disabled="true">
              <HeartOutlined />
              <span>收藏夹</span>
            </span>
            <span class="profile-menu-item is-disabled" aria-disabled="true">
              <DeleteOutlined />
              <span>回收站</span>
            </span>
            <div class="profile-menu-divider" aria-hidden="true"></div>
            <span class="profile-menu-item is-disabled" aria-disabled="true">
              <SettingOutlined />
              <span>账户设置</span>
            </span>
            <span class="profile-menu-item is-disabled" aria-disabled="true">
              <QuestionCircleOutlined />
              <span>帮助与反馈</span>
            </span>
          </nav>
        </aside>

        <main class="center-main">
          <header class="welcome-block">
            <h2>欢迎回来，{{ displayName }}</h2>
            <p>记录灵感，收藏美好。</p>
          </header>

          <section class="quick-actions" aria-label="快捷操作">
            <RouterLink to="/gallery/upload" class="quick-action-card">
              <span class="quick-action-icon is-acid"><CloudUploadOutlined /></span>
              <span class="quick-action-copy">
                <strong>上传图片</strong>
                <small>分享你的精彩瞬间</small>
              </span>
            </RouterLink>
            <RouterLink to="/gallery/manage" class="quick-action-card">
              <span class="quick-action-icon"><PictureOutlined /></span>
              <span class="quick-action-copy">
                <strong>管理我的图片</strong>
                <small>查看、编辑与管理</small>
              </span>
            </RouterLink>
            <RouterLink to="/space" class="quick-action-card">
              <span class="quick-action-icon"><PlusOutlined /></span>
              <span class="quick-action-copy">
                <strong>{{ hasPrivateSpace ? '进入我的空间' : '新建空间' }}</strong>
                <small>{{ hasPrivateSpace ? '查看空间内的照片' : '创建你的专属相册' }}</small>
              </span>
            </RouterLink>
          </section>

          <section class="dashboard-section proto-surface">
            <div class="dashboard-section-head">
              <h3>我的图片</h3>
              <RouterLink to="/gallery/manage" class="section-link">查看全部 <span aria-hidden="true">›</span></RouterLink>
            </div>

            <a-skeleton v-if="recentLoading" active :paragraph="{ rows: 3 }" />
            <a-alert v-else-if="recentError" type="error" show-icon :message="recentError">
              <template #action>
                <a-button size="small" @click="loadRecentPictures(user.spaceId, user.id)">重试</a-button>
              </template>
            </a-alert>
            <a-empty v-else-if="!myPictures.length" description="还没有上传图片">
              <RouterLink to="/gallery/upload" class="proto-button acid-button">上传第一张图片</RouterLink>
            </a-empty>
            <div v-else class="picture-strip">
              <RouterLink
                v-for="picture in myPictures"
                :key="String(picture.id)"
                :to="`/gallery/detail/${picture.id}`"
                class="picture-tile"
              >
                <div class="picture-tile-image">
                  <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '我的图片'" />
                  <!-- 收藏功能尚未接入，这里只保留原型中的视觉位置。 -->
                  <span class="picture-favorite" aria-hidden="true"><HeartOutlined /></span>
                  <span class="picture-status" :class="statusClass(picture.pictureCheck)">
                    <i aria-hidden="true"></i>{{ statusLabel(picture.pictureCheck) }}
                  </span>
                </div>
              </RouterLink>
            </div>
          </section>

          <section class="dashboard-section proto-surface">
            <div class="dashboard-section-head">
              <h3>我的空间</h3>
              <RouterLink to="/space" class="section-link">查看全部 <span aria-hidden="true">›</span></RouterLink>
            </div>

            <a-skeleton v-if="recentLoading" active :paragraph="{ rows: 3 }" />
            <a-alert v-else-if="recentError" type="error" show-icon :message="recentError">
              <template #action>
                <a-button size="small" @click="loadRecentPictures(user.spaceId, user.id)">重试</a-button>
              </template>
            </a-alert>
            <a-empty v-else-if="!hasPrivateSpace" description="还没有创建个人空间">
              <RouterLink to="/space" class="proto-button acid-button">创建个人空间</RouterLink>
            </a-empty>
            <a-empty v-else-if="!privateRecentPictures.length" description="个人空间中暂无照片">
              <RouterLink to="/gallery/upload" class="proto-button acid-button">上传到空间</RouterLink>
            </a-empty>
            <div v-else class="space-picture-strip">
              <!-- 当前后端只有一个私人空间，因此这里展示该空间内真实照片，不伪造多个空间卡片。 -->
              <RouterLink
                v-for="picture in privateRecentPictures"
                :key="String(picture.id)"
                :to="`/gallery/detail/${picture.id}`"
                class="space-picture-tile"
              >
                <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '个人空间照片'" />
                <span class="space-picture-overlay">
                  <strong>{{ picture.name || '未命名图片' }}</strong>
                  <small>{{ formatDate(picture.createtime) }}</small>
                </span>
              </RouterLink>
            </div>
          </section>
        </main>
      </section>
    </div>

    <ProfileEditModal
      v-model:open="editOpen"
      :user="user"
      @saved="refreshCenter"
    />
    <AvatarUpdateModal
      v-model:open="avatarOpen"
      :user="user"
      @saved="refreshCenter"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CloudUploadOutlined,
  DeleteOutlined,
  FolderOpenOutlined,
  HeartOutlined,
  HomeOutlined,
  PictureOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  RightOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { getCurrentUser } from '../../../api/userController'
import { queryPicturePage } from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import AvatarUpdateModal from './components/AvatarUpdateModal.vue'
import ProfileEditModal from './components/ProfileEditModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(true)
const summaryLoading = ref(false)
const editOpen = ref(false)
const avatarOpen = ref(false)
const centerError = ref('')
const pictureTotal = ref(0)
const pendingTotal = ref(0)
const recentLoading = ref(false)
const recentError = ref('')
const myPictures = ref<API.PictureVO[]>([])
const privateRecentPictures = ref<API.PictureVO[]>([])
const emptyUser: API.UserVO = {
  username: '',
  useraccount: '',
  gender: undefined,
  phone: '',
  email: '',
  profile: '',
  avatarurl: '',
  spaceId: undefined,
}
const user = computed<API.UserVO>(() => loginUserStore.loginUser || emptyUser)
const displayName = computed(() => user.value.username || '图库用户')
const hasPrivateSpace = computed(() => user.value.spaceId != null && String(user.value.spaceId) !== '0')

function statusClass(status?: number) {
  return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait'
}

function statusLabel(status?: number) {
  return status === 1 ? '已公开' : status === 2 ? '未通过' : '审核中'
}

function formatDate(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').replace(/\.\d+.*$/, '')
}

function parseTotal(value?: number | string) {
  const total = Number(value || 0)
  return Number.isFinite(total) ? total : 0
}

function pictureTime(value?: string) {
  return value ? Date.parse(value.replace(' ', 'T')) || 0 : 0
}

/**
 * 按审核状态和空间分别查询，保证统计数据不会把公共图库与私人空间混淆。
 * 公共图库固定传0；私人空间只传当前用户自己的空间id。
 */
async function loadPictureSummary(userId?: number | string, spaceId?: number | string) {
  if (!userId) {
    pictureTotal.value = 0
    pendingTotal.value = 0
    return
  }

  summaryLoading.value = true
  try {
    const statuses = [0, 1, 2] as const
    const scopeIds: Array<number | string> = [0]
    if (spaceId != null && String(spaceId) !== '0') scopeIds.push(spaceId)

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
          // 统计只需要total，取1条即可，避免拉取无意义的图片列表。
          pageSize: 1,
          sortFiled: 'createtime',
          sortOrder: 'descend',
        }),
      })),
    )

    responses.forEach(({ response }) => {
      if (response.data?.code !== 200) throw new Error(response.data?.message || '图片概览加载失败')
    })

    pictureTotal.value = responses.reduce(
      (total, { response }) => total + parseTotal(response.data.data?.total),
      0,
    )
    pendingTotal.value = responses
      .filter(({ pictureCheck }) => pictureCheck === 0)
      .reduce((total, { response }) => total + parseTotal(response.data.data?.total), 0)
  } catch {
    // 统计失败时保留页面主体，数字使用0或占位符，不阻断用户继续浏览。
    pictureTotal.value = 0
    pendingTotal.value = 0
  } finally {
    summaryLoading.value = false
  }
}

/**
 * “我的图片”展示当前用户在公共图库中的最近图片，并保留三种审核状态。
 * “我的空间”单独读取私人空间中已通过审核的真实照片，避免静态伪造内容。
 */
async function loadRecentPictures(spaceId?: number | string, userId?: number | string) {
  recentLoading.value = true
  recentError.value = ''
  try {
    const publicResponses = await Promise.all(
      ([0, 1, 2] as const).map((pictureCheck) => queryPicturePage({
        userId,
        spaceId: 0,
        pictureCheck,
        current: 1,
        pageSize: 5,
        sortFiled: 'createtime',
        sortOrder: 'descend',
      })),
    )
    publicResponses.forEach((response) => {
      if (response.data?.code !== 200) throw new Error(response.data?.message || '我的图片加载失败')
    })

    const uniquePictures = new Map<string, API.PictureVO>()
    publicResponses
      .flatMap((response) => response.data.data?.pictureList || [])
      .forEach((picture) => uniquePictures.set(String(picture.id), picture))
    myPictures.value = [...uniquePictures.values()]
      .sort((left, right) => pictureTime(right.createtime) - pictureTime(left.createtime))
      .slice(0, 5)

    if (spaceId == null || String(spaceId) === '0') {
      privateRecentPictures.value = []
      return
    }

    const privateResponse = await queryPicturePage({
      spaceId,
      pictureCheck: 1,
      current: 1,
      pageSize: 5,
      sortFiled: 'createtime',
      sortOrder: 'descend',
    })
    if (privateResponse.data?.code !== 200) {
      throw new Error(privateResponse.data?.message || '个人空间图片加载失败')
    }
    privateRecentPictures.value = privateResponse.data.data?.pictureList || []
  } catch (error: any) {
    myPictures.value = []
    privateRecentPictures.value = []
    recentError.value = error?.response?.data?.message || error?.message || '最近图片加载失败'
  } finally {
    recentLoading.value = false
  }
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
      loadRecentPictures(currentUser.spaceId, currentUser.id),
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

async function refreshCenter() {
  // 编辑资料或更新头像成功后重新读取用户，确保页面和全局登录态同步。
  await loadCenter(false)
}

onMounted(loadCenter)
</script>

<style scoped>
.center-prototype { min-height: 100%; }
.center-skeleton { padding-top: 18px; }
.center-skeleton-grid { margin-top: 16px; display: grid; grid-template-columns: minmax(250px, .29fr) minmax(0, .71fr); gap: 28px; }
.center-skeleton-grid > * { min-height: 420px; padding: 20px; background: rgba(255,255,255,.58); border: 1px solid var(--proto-line); border-radius: 18px; }
.center-content { min-height: 100%; }
.center-layout.proto-section { padding-top: 18px; }
/* 两列保持自然高度，避免为了对齐左侧菜单而在右侧制造大块空白。 */
.center-layout { display: grid; grid-template-columns: minmax(250px, .29fr) minmax(0, .71fr); gap: 28px; align-items: start; }
.profile-column { min-width: 0; display: flex; flex-direction: column; gap: 22px; }

/* 左侧资料卡对应原型中的身份区，统计信息也归入同一张卡片。 */
.profile-card { padding: 26px 26px 20px; border: 1px solid rgba(17,20,22,.05); border-radius: 20px; background: rgba(255,255,255,.72); box-shadow: 0 12px 32px rgba(18,23,23,.035); }
.profile-identity { text-align: center; }
.profile-avatar-wrap { position: relative; width: fit-content; display: flex; justify-content: center; margin: 0 auto 15px; padding: 0; border: 0; background: transparent; color: inherit; cursor: pointer; }
.profile-avatar-wrap :deep(.ant-avatar) { border: 1px solid rgba(17,20,22,.08); background: var(--proto-paper-deep); color: var(--proto-ink); font-size: 54px; box-shadow: 0 8px 20px rgba(18,23,23,.08); }
.profile-avatar-hint { position: absolute; right: 3px; bottom: 2px; padding: 4px 7px; border: 1px solid var(--proto-line); border-radius: 999px; background: var(--proto-paper); color: var(--proto-ink); font-size: 10px; opacity: 0; transform: translateY(4px); transition: opacity .2s ease, transform .2s ease; }
.profile-avatar-wrap:hover .profile-avatar-hint, .profile-avatar-wrap:focus-visible .profile-avatar-hint { opacity: 1; transform: translateY(0); }
.profile-avatar-wrap:focus-visible { outline: 2px solid var(--proto-ink); outline-offset: 5px; border-radius: 50%; }
.profile-card h1 { margin: 0; font-size: 28px; font-weight: 800; line-height: 1.15; letter-spacing: -.06em; }
.profile-account { margin: 7px 0 0; color: var(--proto-muted); font-size: 14px; line-height: 1.35; }
.profile-bio { min-height: 22px; margin: 16px auto 0; color: var(--proto-muted); font-size: 12px; line-height: 1.55; }
.profile-edit { width: 100%; height: 44px; margin-top: 20px; display: inline-flex; align-items: center; justify-content: center; gap: 12px; border: 1px solid rgba(17,20,22,.18); border-radius: 11px; background: transparent; color: var(--proto-ink); font-size: 13px; font-weight: 700; }
.profile-edit:hover { border-color: var(--proto-ink); color: var(--proto-ink); background: rgba(241,242,237,.55); }
.profile-stats { margin-top: 20px; padding-top: 18px; display: grid; grid-template-columns: repeat(3, 1fr); border-top: 1px solid rgba(17,20,22,.08); }
.profile-stats > div { min-width: 0; display: flex; flex-direction: column; align-items: center; gap: 4px; border-right: 1px solid rgba(17,20,22,.08); }
.profile-stats > div:last-child { border-right: 0; }
.profile-stats strong { font-family: 'Abril Fatface', Georgia, serif; font-size: 24px; font-weight: 400; line-height: 1; }
.profile-stats span { color: var(--proto-muted); font-size: 11px; }

/* 侧边菜单先固定视觉层级；没有后端能力的项目仍保持不可误点。 */
.profile-menu { padding: 14px 10px; border: 1px solid rgba(17,20,22,.04); border-radius: 18px; background: rgba(255,255,255,.6); box-shadow: 0 12px 32px rgba(18,23,23,.025); }
.profile-menu-item { min-height: 42px; padding: 0 14px; display: flex; align-items: center; gap: 14px; border-radius: 10px; color: var(--proto-ink-soft); font-size: 13px; font-weight: 600; text-decoration: none; transition: background .2s ease, color .2s ease; }
.profile-menu-item :deep(svg) { flex: 0 0 auto; color: var(--proto-ink-soft); font-size: 18px; }
.profile-menu-item:hover { color: var(--proto-ink); background: rgba(241,242,237,.72); }
.profile-menu-item.is-active { color: #5f8e1b; background: rgba(186,255,61,.12); }
.profile-menu-item.is-active :deep(svg) { color: #6a9b1f; }
.profile-menu-item.is-disabled { cursor: default; color: var(--proto-muted); opacity: .88; }
.profile-menu-item.is-disabled:hover { background: transparent; }
.profile-menu-divider { height: 1px; margin: 12px 14px; background: rgba(17,20,22,.08); }

.center-main { min-width: 0; height: auto; display: flex; flex-direction: column; gap: 20px; }
.center-main > .dashboard-section:last-child { flex: 0 0 auto; }
.welcome-block { padding: 12px 2px 2px; }
.welcome-block h2 { margin: 0; font-size: clamp(25px, 2.4vw, 34px); font-weight: 800; letter-spacing: -.055em; line-height: 1.2; }
.welcome-block p { margin: 8px 0 0; color: var(--proto-muted); font-size: 15px; }

/* 三个快捷入口复用现有路由，只调整为原型中的横向功能卡片。 */
.quick-actions { min-height: 108px; padding: 22px 26px; display: grid; grid-template-columns: repeat(3, 1fr); align-items: center; border: 1px solid rgba(17,20,22,.05); border-radius: 20px; background: rgba(255,255,255,.72); box-shadow: 0 12px 32px rgba(18,23,23,.035); }
.quick-action-card { min-width: 0; min-height: 62px; padding: 0 24px; display: flex; align-items: center; gap: 16px; border-right: 1px solid rgba(17,20,22,.08); color: var(--proto-ink); text-decoration: none; }
.quick-action-card:first-child { padding-left: 14px; }
.quick-action-card:last-child { padding-right: 8px; border-right: 0; }
.quick-action-card:hover { color: var(--proto-ink); }
.quick-action-card:hover .quick-action-icon { transform: translateY(-2px); box-shadow: 0 8px 18px rgba(18,23,23,.08); }
.quick-action-icon { width: 54px; height: 54px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 16px; background: rgba(17,20,22,.045); color: var(--proto-ink-soft); font-size: 25px; transition: transform .2s ease, box-shadow .2s ease; }
.quick-action-icon.is-acid { background: rgba(186,255,61,.15); color: #6a9b1f; }
/* 三个快捷入口分别使用品牌绿、蓝、橙，避免后两个图标都落在同一灰色背景里。 */
.quick-action-card:nth-child(2) .quick-action-icon { background: rgba(167,201,255,.2); color: #5279ae; }
.quick-action-card:nth-child(3) .quick-action-icon { background: rgba(255,137,106,.16); color: #b96149; }
.quick-action-copy { min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.quick-action-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.quick-action-copy small { overflow: hidden; color: var(--proto-muted); text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }

.dashboard-section { min-width: 0; padding: 23px 22px 24px; border: 1px solid rgba(17,20,22,.05); border-radius: 20px; background: rgba(255,255,255,.72); box-shadow: 0 12px 32px rgba(18,23,23,.035); }
.dashboard-section-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.dashboard-section-head h3 { margin: 0; font-size: 19px; font-weight: 800; letter-spacing: -.045em; }
.section-link { flex: 0 0 auto; color: #6a9b1f; font-size: 12px; font-weight: 700; text-decoration: none; }
.section-link:hover { color: #4d7413; }
.section-link span { margin-left: 3px; font-size: 18px; line-height: 0; vertical-align: -1px; }
.dashboard-section :deep(.ant-skeleton), .dashboard-section :deep(.ant-alert), .dashboard-section :deep(.ant-empty) { margin-top: 12px; }
.dashboard-section :deep(.ant-empty) { margin-bottom: 0; }
.picture-strip, .space-picture-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; }
.space-picture-strip { padding: 2px 0 20px; }
.picture-tile, .space-picture-tile { min-width: 0; display: block; overflow: hidden; border-radius: 14px; background: var(--proto-paper-deep); text-decoration: none; }
.picture-tile-image { position: relative; aspect-ratio: 1 / 1.12; overflow: hidden; }
.picture-tile-image img, .space-picture-tile img { width: 100%; height: 100%; display: block; object-fit: cover; transition: transform .3s ease; }
.picture-tile:hover img, .space-picture-tile:hover img { transform: scale(1.035); }
.picture-favorite { position: absolute; top: 10px; right: 10px; width: 28px; height: 28px; display: grid; place-items: center; border-radius: 50%; background: rgba(255,255,255,.9); color: #81898a; font-size: 16px; }
.picture-status { position: absolute; right: 10px; bottom: 10px; padding: 5px 9px; display: inline-flex; align-items: center; gap: 5px; border-radius: 999px; background: rgba(255,255,255,.92); color: var(--proto-muted); font-size: 10px; line-height: 1; }
.picture-status i { width: 7px; height: 7px; display: block; border-radius: 50%; background: #b7bec0; }
.picture-status.pass i { background: #87c52d; }
.picture-status.wait i { background: #f4bd20; }
.picture-status.refuse i { background: #eb6262; }
.space-picture-tile { position: relative; aspect-ratio: 1 / 1.08; }
.space-picture-overlay { position: absolute; right: 0; bottom: 0; left: 0; padding: 34px 13px 12px; display: flex; flex-direction: column; gap: 4px; background: linear-gradient(transparent, rgba(17,20,22,.78)); color: var(--proto-paper); }
.space-picture-overlay strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.space-picture-overlay small { color: rgba(241,242,237,.76); font-size: 10px; }

@media (max-width: 1050px) {
  .center-layout, .center-skeleton-grid { grid-template-columns: minmax(220px, .31fr) minmax(0, .69fr); gap: 20px; }
  .profile-card { padding-inline: 18px; }
  .quick-actions { padding-inline: 16px; }
  .quick-action-card { padding-inline: 14px; gap: 10px; }
  .quick-action-icon { width: 46px; height: 46px; font-size: 21px; }
  .picture-strip, .space-picture-strip { gap: 10px; }
}
@media (max-width: 820px) {
  .center-layout, .center-skeleton-grid { grid-template-columns: 1fr; }
  .profile-column { display: grid; grid-template-columns: minmax(0, .9fr) minmax(220px, 1.1fr); align-items: start; }
  .center-main { height: auto; gap: 16px; }
  .center-main > .dashboard-section:last-child { flex: 0 0 auto; }
}
@media (max-width: 650px) {
  .profile-column { display: flex; }
  .quick-actions { grid-template-columns: 1fr; padding: 12px 18px; }
  .quick-action-card, .quick-action-card:first-child, .quick-action-card:last-child { min-height: 66px; padding: 8px 0; border-right: 0; border-bottom: 1px solid rgba(17,20,22,.08); }
  .quick-action-card:last-child { border-bottom: 0; }
  .picture-strip, .space-picture-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .picture-tile:last-child, .space-picture-tile:last-child { display: none; }
}
@media (max-width: 420px) {
  .profile-stats strong { font-size: 21px; }
  .dashboard-section { padding-inline: 15px; }
}
@media (prefers-reduced-motion: reduce) {
  .profile-avatar-hint, .quick-action-icon, .picture-tile-image img, .space-picture-tile img { transition: none; }
}
</style>
