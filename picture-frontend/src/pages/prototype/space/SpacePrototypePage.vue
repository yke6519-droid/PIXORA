<template>
  <div class="space-prototype">
    <div v-if="pageLoading" class="space-skeleton" aria-label="正在加载个人空间">
      <a-skeleton active :paragraph="{ rows: 2 }" />
      <div class="space-skeleton-grid">
        <a-skeleton active :paragraph="{ rows: 8 }" />
        <a-skeleton active :paragraph="{ rows: 8 }" />
      </div>
    </div>

    <a-result
      v-else-if="pageError"
      status="error"
      title="个人空间暂时无法加载"
      :sub-title="pageError"
    >
      <template #extra>
        <a-button class="proto-button acid-button" type="primary" @click="loadPage">重新加载</a-button>
      </template>
    </a-result>

    <section v-else-if="!space" class="space-empty">
      <div class="space-empty-main">
        <div class="space-empty-copy">
          <h1>
            <span>创建个人空间</span>
          </h1>
          <p>私人空间仅你可见，用于保存和管理个人图片。</p>
        </div>

        <div class="space-empty-action">
          <a-button class="proto-button acid-button" type="primary" @click="createOpen = true">
            创建我的空间
          </a-button>
        </div>
      </div>

      <aside class="space-empty-spec" aria-label="基础空间配置">
        <div class="space-empty-spec-head">
          <span>空间配置</span>
          <strong>仅持有人</strong>
        </div>
        <dl>
          <div>
            <dt>容量上限</dt>
            <dd>100 MB</dd>
          </div>
          <div>
            <dt>图片上限</dt>
            <dd>50 张</dd>
          </div>
          <div>
            <dt>访问范围</dt>
            <dd>仅持有人</dd>
          </div>
        </dl>
      </aside>
    </section>

    <template v-else>
      <section class="proto-page-head">
        <div class="space-heading-row">
          <div class="space-heading-main">
            <h1 class="proto-title">{{ space.spaceName || '个人空间' }}</h1>
            <a-button class="proto-button ghost-button" @click="renameOpen = true">重命名</a-button>
          </div>
        </div>
      </section>

      <section class="space-overview proto-section">
        <div class="space-usage-card proto-surface proto-rounded">
          <div class="usage-card-head">
            <h2>空间使用</h2>
          </div>

          <div
            class="usage-ring"
            :style="{ background: `conic-gradient(var(--proto-acid) ${sizePercent}%, var(--proto-paper-deep) 0)` }"
          >
            <div class="usage-ring-inner">
              <strong>{{ sizePercent }}%</strong>
              <span>容量使用率</span>
            </div>
          </div>

          <div class="usage-values">
            <div>
              <span>已使用空间</span>
              <strong>{{ formatSize(space.usedSize) }} / {{ formatSize(space.maxSize) }}</strong>
            </div>
            <div>
              <span>图片数量</span>
              <strong>{{ toNumber(space.usedCount) }} / {{ toNumber(space.maxCount) }}</strong>
            </div>
          </div>
          <a-progress :percent="sizePercent" :show-info="false" stroke-color="#baff3d" trail-color="#e4e7df" />
          <div class="usage-foot">
            <span>持有人 {{ space.createdUser?.username || loginUserStore.loginUser?.username || '当前用户' }}</span>
            <span>最近更新 {{ formatDate(space.updateTime) }}</span>
          </div>
        </div>

        <div class="space-side-stack">
          <div class="proto-bento-card dark">
            <span class="space-card-label">空间权限</span>
            <h3>仅自己可见</h3>
            <p>只有空间持有人可以查看和管理其中的图片。</p>
          </div>
          <div class="proto-bento-card acid">
            <span class="space-card-label">空间等级</span>
            <h3>{{ formatSpaceLevel(space.spaceLevel || 0) }}</h3>
          </div>
        </div>
      </section>

      <section class="space-gallery proto-section">
        <div class="space-gallery-head">
          <div class="space-gallery-title">
            <h2 class="proto-subtitle">空间图片</h2>
            <span class="space-gallery-count">{{ pictureTotal }} 张{{ statusText }}图片</span>
          </div>
          <div class="space-gallery-actions">
            <a-segmented v-model:value="pictureCheck" :options="statusOptions" @change="changeStatus" />
            <a-button class="proto-button ghost-button danger-action" @click="confirmDeleteSpace">删除空间</a-button>
            <a-button class="proto-button acid-button" type="primary" @click="openSpaceUpload">上传到空间</a-button>
          </div>
        </div>

        <a-alert
          v-if="pictureError"
          class="space-picture-alert"
          type="error"
          show-icon
          :message="pictureError"
        >
          <template #action>
            <a-button size="small" @click="loadPictures">重试</a-button>
          </template>
        </a-alert>

        <div v-if="pictureLoading" class="space-picture-grid">
          <a-skeleton v-for="index in 6" :key="index" active :paragraph="{ rows: 2 }" class="space-picture-skeleton" />
        </div>

        <a-empty
          v-else-if="!pictureError && !pictures.length"
          :description="`空间中暂无${statusText}图片`"
          class="space-picture-empty"
        >
          <a-button class="proto-button acid-button" type="primary" @click="openSpaceUpload">上传图片</a-button>
        </a-empty>

        <div v-else-if="pictures.length" class="space-picture-grid">
          <article
            v-for="picture in pictures"
            :key="String(picture.id)"
            class="space-picture-card"
            role="button"
            tabindex="0"
            @click="openPicture(picture.id)"
            @keydown.enter="openPicture(picture.id)"
          >
            <div class="space-picture-image proto-image-wrap">
              <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '空间图片'" />
              <a-button
                class="space-picture-delete"
                size="small"
                danger
                aria-label="删除图片"
                @click.stop="confirmDeletePicture(picture)"
              >
                删除
              </a-button>
            </div>
            <div class="space-picture-copy">
              <div>
                <strong>{{ picture.name || '未命名图片' }}</strong>
                <span>{{ picture.category || '未分类' }} · {{ formatSize(picture.picsize) }}</span>
              </div>
              <a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">
                {{ pictureStatusText(picture.pictureCheck) }}
              </a-tag>
            </div>
          </article>
        </div>

        <a-pagination
          v-if="pictureTotal > pageSize"
          v-model:current="current"
          class="space-pagination"
          :page-size="pageSize"
          :total="pictureTotal"
          :show-size-changer="false"
          show-less-items
          @change="changePage"
        />
      </section>
    </template>

    <SpaceNameModal
      v-model:open="createOpen"
      mode="create"
      :submitting="creating"
      @submit="createSpace"
    />
    <SpaceNameModal
      v-model:open="renameOpen"
      mode="rename"
      :initial-name="space?.spaceName"
      :submitting="renaming"
      @submit="renameSpace"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import {
  createSpaceUsingPost,
  deleteByIdUsingDelete,
  querySpaceByIdUsingGet,
  updateByIdUsingPut,
} from '../../../api/spaceController'
import {
  deletePictureUsingDelete,
  queryPicturePageUsingPost,
} from '../../../api/pictureController'
import { getCurrentUserUsingGet } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { formatSpaceLevel, pictureStatusText } from '../prototypeData'
import SpaceNameModal from './components/SpaceNameModal.vue'

type PictureStatus = 0 | 1 | 2

const router = useRouter()
const loginUserStore = useLoginUserStore()
const pageLoading = ref(true)
const pageError = ref('')
const pictureLoading = ref(false)
const pictureError = ref('')
const creating = ref(false)
const renaming = ref(false)
const createOpen = ref(false)
const renameOpen = ref(false)
const space = ref<API.SpaceVO | null>(null)
const pictures = ref<API.PictureVO[]>([])
const pictureTotal = ref(0)
const pictureCheck = ref<PictureStatus>(1)
const current = ref(1)
const pageSize = 9
const statusOptions = [
  { label: '已通过', value: 1 },
  { label: '待审核', value: 0 },
  { label: '未通过', value: 2 },
]

const sizePercent = computed(() => usagePercent(space.value?.usedSize, space.value?.maxSize))
const statusText = computed(() => pictureStatusText(pictureCheck.value))

function toNumber(value?: number | string) {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed : 0
}

function usagePercent(used?: number | string, max?: number | string) {
  const maximum = toNumber(max)
  if (!maximum) return 0
  return Math.min(100, Math.round((toNumber(used) / maximum) * 100))
}

function formatSize(value?: number | string) {
  const bytes = toNumber(value)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').replace(/\.\d+.*$/, '')
}

function statusClass(status?: number) {
  return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait'
}

function hasSpaceId(user?: API.UserVO | null) {
  return user?.spaceId != null && String(user.spaceId) !== '0'
}

async function refreshCurrentUser() {
  const res = await getCurrentUserUsingGet()
  if (res.data?.code !== 200 || !res.data.data) {
    if (res.data?.code === 40100) {
      loginUserStore.clearLoginUser()
      await router.replace({
        path: '/prototype/user/login',
        query: { redirect: '/prototype/space' },
      })
      return null
    }
    throw new Error(res.data?.message || '当前用户信息加载失败')
  }
  loginUserStore.setLoginUser(res.data.data)
  return res.data.data
}

async function loadSpace(spaceId: number | string) {
  const res = await querySpaceByIdUsingGet({ spaceId })
  if (res.data?.code !== 200 || !res.data.data) {
    throw new Error(res.data?.message || '空间信息加载失败')
  }
  space.value = res.data.data
}

/**
 * 后端未传 pictureCheck 时默认只查审核通过，因此页面始终显式传递当前状态。
 */
async function loadPictures() {
  if (!space.value?.id) return
  pictureLoading.value = true
  pictureError.value = ''
  try {
    const res = await queryPicturePageUsingPost({
      spaceId: space.value.id,
      pictureCheck: pictureCheck.value,
      current: current.value,
      pageSize,
      sortFiled: 'createtime',
      sortOrder: 'descend',
    })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '空间图片加载失败')
    }
    pictures.value = res.data.data?.pictureList || []
    pictureTotal.value = toNumber(res.data.data?.total)
  } catch (error: any) {
    pictureError.value = error?.response?.data?.message || error?.message || '空间图片加载失败'
  } finally {
    pictureLoading.value = false
  }
}

async function loadPage() {
  pageLoading.value = true
  pageError.value = ''
  try {
    const currentUser = await refreshCurrentUser()
    if (!currentUser || !hasSpaceId(currentUser)) {
      space.value = null
      pictures.value = []
      pictureTotal.value = 0
      return
    }
    await loadSpace(currentUser.spaceId!)
    await loadPictures()
  } catch (error: any) {
    pageError.value = error?.response?.data?.message || error?.message || '个人空间加载失败'
  } finally {
    pageLoading.value = false
  }
}

async function createSpace(spaceName: string) {
  creating.value = true
  try {
    const res = await createSpaceUsingPost({ spaceName })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '空间创建失败')
    createOpen.value = false
    message.success('私人空间已创建')
    await loadPage()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '空间创建失败')
  } finally {
    creating.value = false
  }
}

async function renameSpace(spaceName: string) {
  if (!space.value?.id) return
  renaming.value = true
  try {
    const res = await updateByIdUsingPut({ spaceId: space.value.id, updatedName: spaceName })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '空间重命名失败')
    renameOpen.value = false
    message.success('空间名称已更新')
    await loadSpace(space.value.id)
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '空间重命名失败')
  } finally {
    renaming.value = false
  }
}

function confirmDeleteSpace() {
  if (!space.value?.id) return
  Modal.confirm({
    title: '删除私人空间？',
    content: '删除后，空间中的全部图片也会被删除，且无法恢复。',
    okText: '删除空间',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteByIdUsingDelete({ spaceId: space.value!.id })
      if (res.data?.code !== 200) throw new Error(res.data?.message || '空间删除失败')
      await refreshCurrentUser()
      space.value = null
      pictures.value = []
      pictureTotal.value = 0
      message.success('私人空间已删除')
    },
  })
}

function confirmDeletePicture(picture: API.PictureVO) {
  if (!picture.id) return
  Modal.confirm({
    title: '删除这张图片？',
    content: `「${picture.name || '未命名图片'}」删除后无法恢复。`,
    okText: '删除图片',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deletePictureUsingDelete({ id: picture.id })
      if (res.data?.code !== 200) throw new Error(res.data?.message || '图片删除失败')
      if (pictures.value.length === 1 && current.value > 1) current.value -= 1
      await Promise.all([loadSpace(space.value!.id!), loadPictures()])
      message.success('图片已删除')
    },
  })
}

function changeStatus() {
  current.value = 1
  void loadPictures()
}

function changePage(page: number) {
  current.value = page
  void loadPictures()
}

function openPicture(id?: number | string) {
  if (!id) return
  void router.push(`/prototype/gallery/detail/${encodeURIComponent(String(id))}`)
}

function openSpaceUpload() {
  void router.push({ path: '/prototype/gallery/upload', query: { target: 'space' } })
}

onMounted(loadPage)
</script>

<style scoped>
/* 空间页优先展示真实空间状态，收紧解释性区域，避免首屏被空白拉开。 */
.space-prototype { padding-top: 20px; padding-bottom: 18px; }
.space-skeleton { padding-top: 12px; }
.space-skeleton-grid { margin-top: 16px; display: grid; grid-template-columns: 1.15fr .85fr; gap: 16px; }
.space-skeleton-grid > * { min-height: 300px; padding: 20px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.45); }
.space-empty {
  width: 100%;
  min-height: 420px;
  margin: 18px 0 0;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, .85fr);
  border: 1px solid var(--proto-line);
}
.space-empty-main {
  min-width: 0;
  padding: 44px 54px 42px 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 40px;
}
.space-empty-copy { display: flex; flex-direction: column; align-items: flex-start; gap: 14px; }
.space-empty h1 {
  max-width: none;
  margin: 0;
  font-size: clamp(38px, 4vw, 56px);
  line-height: 1;
  letter-spacing: -.045em;
}
.space-empty h1 span { display: block; white-space: nowrap; }
.space-empty-copy p {
  max-width: 46ch;
  margin: 0;
  color: var(--proto-muted);
  font-size: 13px;
  line-height: 1.6;
  text-wrap: pretty;
}
.space-empty-action { display: flex; align-items: center; gap: 0; }
.space-empty-spec {
  padding: 32px 36px;
  display: flex;
  flex-direction: column;
  background: var(--proto-ink);
  color: var(--proto-paper);
}
.space-empty-spec-head {
  padding-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(241,242,237,.2);
}
.space-empty-spec-head span { font-size: 14px; font-weight: 700; }
.space-empty-spec-head strong { color: var(--proto-acid); font-family: 'DM Mono', monospace; font-size: 10px; letter-spacing: .08em; }
.space-empty-spec dl { margin: 20px 0 0; }
.space-empty-spec dl div {
  min-height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid rgba(241,242,237,.15);
}
.space-empty-spec dt { color: rgba(241,242,237,.58); font-size: 11px; }
.space-empty-spec dd { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -.03em; }
.space-prototype > .proto-page-head { padding-top: 0; padding-bottom: 0; align-items: flex-end; }
.space-heading-row { width: 100%; display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; }
.space-heading-main { min-width: 0; display: flex; align-items: flex-end; gap: 16px; }
.space-prototype > .proto-page-head .proto-title { max-width: min(70vw, 660px); margin: 0; overflow: hidden; font-size: clamp(32px, 3vw, 42px); line-height: 1.06; text-overflow: ellipsis; white-space: nowrap; }
.space-heading-main .proto-button { height: 36px; padding-inline: 12px; font-size: 12px; }
.space-level-block { min-width: 0; padding: 7px 12px; display: inline-flex; align-items: center; gap: 8px; background: var(--proto-orange); }
.space-level-block span, .space-level-block strong, .space-level-block small { display: block; }
.space-level-block span { font-family: 'DM Mono', monospace; font-size: 10px; }
.space-level-block strong { margin: 0; font-size: 18px; letter-spacing: -.06em; }
.space-level-block small { display: none; }
/* 统一概览框承载容量、数量和权限，减少首屏纵向占用。 */
.space-overview.proto-section, .space-gallery.proto-section { padding-top: 12px; }
.space-overview { margin-top: 18px; display: grid; grid-template-columns: minmax(380px, 1.18fr) minmax(260px, .82fr); gap: 12px; align-items: stretch; padding: 14px; border: 1px solid var(--proto-line); border-radius: 10px; background: rgba(255,255,255,.46); box-shadow: var(--proto-shadow); }
.space-usage-card { min-width: 0; padding: 4px 8px 2px; border: 0; border-radius: 0; background: transparent; box-shadow: none; display: grid; grid-template-columns: 124px minmax(0, 1fr); grid-template-rows: auto auto auto auto; column-gap: 18px; }
.usage-card-head { grid-column: 1 / -1; display: flex; align-items: center; justify-content: flex-start; gap: 15px; }
.usage-card-head h2 { margin: 0; font-size: 20px; letter-spacing: -.06em; }
.usage-ring { width: 104px; height: 104px; margin: 12px auto 8px; padding: 9px; display: grid; place-items: center; border-radius: 50%; grid-column: 1; grid-row: 2 / span 2; align-self: center; }
.usage-ring-inner { width: 100%; height: 100%; display: grid; place-content: center; text-align: center; border-radius: 50%; background: var(--proto-paper); }
.usage-ring-inner strong, .usage-ring-inner span { display: block; }
.usage-ring-inner strong { font-size: 28px; letter-spacing: -.08em; }
.usage-ring-inner span { color: var(--proto-muted); font-size: 9px; }
.usage-values { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 0 0 8px; grid-column: 2; grid-row: 2; align-self: end; }
.usage-values span, .usage-values strong { display: block; }
.usage-values span { color: var(--proto-muted); font-size: 10px; }
.usage-values strong { margin-top: 4px; font-size: 13px; }
.space-usage-card :deep(.ant-progress-inner) { border-radius: 0; }
.space-usage-card > :deep(.ant-progress) { grid-column: 2; grid-row: 3; align-self: start; }
.usage-foot { display: flex; grid-column: 1 / -1; grid-row: 4; justify-content: space-between; gap: 10px; margin-top: 9px; padding-top: 9px; border-top: 1px solid var(--proto-line); color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
.space-side-stack { display: grid; grid-template-rows: repeat(2, minmax(0, 1fr)); gap: 10px; }
.space-side-stack .proto-bento-card { min-height: 0; }
.space-side-stack .proto-bento-card { padding: 16px; }
.space-side-stack .proto-bento-card { display: flex; flex-direction: column; justify-content: center; }
.space-side-stack h3 { margin: 8px 0 4px; font-size: 20px; }
.space-side-stack p { font-size: 10px; line-height: 1.45; }
.space-card-label { font-size: 10px; font-weight: 700; opacity: .58; }
/* 图片标题与筛选操作保持同一层级，先看状态，再进行操作。 */
.space-gallery-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 14px; }
.space-gallery-title { display: flex; align-items: baseline; gap: 14px; min-width: 0; }
.space-gallery-title .proto-subtitle { margin: 0; font-size: 30px; line-height: 1.1; }
.space-gallery-count { color: var(--proto-muted); font-size: 12px; }
.space-gallery-actions { display: flex; align-items: center; justify-content: flex-end; gap: 9px; flex-wrap: wrap; }
.danger-action:hover { color: #a61d24 !important; border-color: #a61d24 !important; }
.space-picture-alert { margin-bottom: 14px; }
.space-picture-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.space-picture-skeleton { min-height: 250px; padding: 18px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.45); }
.space-picture-card { overflow: hidden; background: rgba(255,255,255,.5); border: 1px solid var(--proto-line); cursor: pointer; transition: border-color .2s ease, box-shadow .2s ease; }
.space-picture-card:hover, .space-picture-card:focus-visible { border-color: rgba(17,20,22,.46); box-shadow: var(--proto-shadow); }
.space-picture-image { height: 200px; position: relative; }
.space-picture-delete { position: absolute; top: 9px; right: 9px; opacity: 0; transition: opacity .2s ease; }
.space-picture-card:hover .space-picture-delete, .space-picture-card:focus-within .space-picture-delete { opacity: 1; }
.space-picture-copy { min-height: 58px; padding: 11px; display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.space-picture-copy strong, .space-picture-copy span { display: block; }
.space-picture-copy strong { max-width: 180px; overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.space-picture-copy span { margin-top: 4px; color: var(--proto-muted); font-size: 10px; }
.space-picture-empty { min-height: 240px; padding: 30px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.42); }
.space-pagination { margin-top: 14px; display: flex; justify-content: flex-end; }
/* 桌面端把空间概览收进首屏节奏，保证用户进入页面即可看到图片第一行。 */
@media (min-width: 981px) {
  .space-overview {
    height: clamp(190px, 22vh, 210px);
    min-height: 190px;
    overflow: hidden;
  }
  .space-usage-card {
    transform: translateX(24px);
    grid-template-rows: 28px minmax(0, 1fr) 8px 22px;
  }
  .usage-ring {
    width: 92px;
    height: 92px;
    margin: 0 auto;
  }
  .usage-ring-inner strong { font-size: 24px; }
  .usage-values { margin-bottom: 4px; }
  .usage-foot { margin-top: 6px; padding-top: 6px; }
  .space-side-stack { width: 50%; justify-self: end; }
  .space-side-stack .proto-bento-card { padding: 12px 16px; }
  .space-side-stack h3 { margin: 4px 0 2px; font-size: 18px; }
  .space-side-stack p { line-height: 1.35; }
  .space-gallery.proto-section { padding-top: 10px; }
  .space-gallery-head { margin-bottom: 10px; }
}
@media (max-width: 980px) {
  .space-gallery-head { align-items: flex-start; flex-direction: column; }
  .space-gallery-actions { justify-content: flex-start; }
}
@media (max-width: 820px) {
  .space-overview, .space-skeleton-grid { grid-template-columns: 1fr; }
  .space-picture-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .space-heading-row { align-items: flex-start; flex-direction: column; gap: 10px; }
  .space-heading-main { width: 100%; }
  .space-level-block { align-self: flex-start; }
  .space-usage-card { grid-template-columns: 1fr; grid-template-rows: auto; padding: 4px 0 2px; }
  .usage-card-head, .usage-ring, .usage-values, .space-usage-card > :deep(.ant-progress), .usage-foot { grid-column: auto; grid-row: auto; }
  .usage-ring { margin: 14px auto 12px; }
  .usage-values { margin-bottom: 12px; }
  .space-empty { min-height: 0; grid-template-columns: 1fr; }
  .space-empty-main { padding: 38px 0; gap: 32px; }
  .space-empty-spec { min-height: 320px; }
}
@media (max-width: 580px) {
  .space-empty { margin-top: 18px; }
  .space-empty-main { padding: 32px 0; }
  .space-empty h1 { font-size: 40px; }
  .space-empty h1 span { white-space: normal; }
  .space-heading-main { align-items: flex-start; flex-direction: column; gap: 8px; }
  .space-prototype > .proto-page-head .proto-title { max-width: 100%; white-space: normal; }
  .space-empty-action { align-items: flex-start; flex-direction: column; gap: 12px; }
  .space-empty-spec { min-height: 300px; padding: 28px 24px; }
  .space-picture-grid { grid-template-columns: 1fr; }
  .usage-foot { align-items: flex-start; flex-direction: column; }
  .space-gallery-actions { align-items: stretch; flex-direction: column; width: 100%; }
}
</style>
