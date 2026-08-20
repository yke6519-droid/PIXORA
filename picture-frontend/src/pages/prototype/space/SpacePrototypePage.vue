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
            <div class="space-heading-copy">
              <h1 class="proto-title">{{ space.spaceName || '个人空间' }}</h1>
              <div class="space-heading-meta" aria-label="空间属性">
                <span class="space-meta-tag">私密空间</span>
                <span class="space-meta-tag">{{ formatSpaceLevel(space.spaceLevel || 0) }}</span>
                <!-- <span class="space-meta-tag is-count">{{ totalPictureCount }} 张图片</span> -->
              </div>
            </div>
            <!-- <a-button class="proto-button ghost-button space-rename-button" @click="renameOpen = true">
              重命名
            </a-button> -->
          </div>

          <!-- 危险操作收进更多菜单，避免和上传按钮形成同级竞争。 -->
          <a-dropdown placement="bottomRight" :trigger="['click']">
            <button type="button" class="space-more-button" aria-label="空间设置">
              <MoreOutlined aria-hidden="true" />
            </button>
            <template #overlay>
              <a-menu @click="handleSpaceMenuClick">
                <a-menu-item key="rename">重命名空间</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="delete" danger>删除空间</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </section>

      <!-- 压缩后的空间摘要只保留用户真正需要关注的数字。 -->
      <section class="space-usage-strip proto-surface" aria-label="空间使用情况">
        <div class="usage-strip-intro">
          <span>空间使用</span>
          <strong>{{ sizePercent }}%</strong>
        </div>

        <div class="usage-strip-meter">
          <div class="usage-strip-meter-head">
            <span>{{ formatSize(space.usedSize) }} / {{ formatSize(space.maxSize) }}</span>
            <small>容量</small>
          </div>
          <a-progress
            :percent="sizePercent"
            :show-info="false"
            stroke-color="#baff3d"
            trail-color="#e4e7df"
          />
        </div>

        <div class="usage-strip-stat">
          <span>图片</span>
          <strong>{{ toNumber(space.usedCount) }} / {{ toNumber(space.maxCount) }} 张</strong>
        </div>

        <div class="usage-strip-privacy">
          <span>私密空间</span>
          <small>仅自己可见</small>
        </div>
      </section>

      <section class="space-gallery proto-section">
        <div class="space-gallery-head">
          <div class="space-gallery-title">
            <h2 class="proto-subtitle">我的图片</h2>
            <span class="space-gallery-count">{{ pictureSummaryText }}</span>
          </div>
          <div class="space-gallery-actions">
            <div class="space-status-tabs" role="tablist" aria-label="图片审核状态">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                type="button"
                class="space-status-tab"
                :class="{ 'is-active': pictureCheck === option.value }"
                role="tab"
                :aria-selected="pictureCheck === option.value"
                @click="changeStatus(option.value)"
              >
                <span>{{ option.label }}</span>
                <strong v-if="option.value === 'all'">{{ totalPictureCount }}</strong>
              </button>
            </div>
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
          :description="pictureEmptyText"
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
              <a-tag class="space-picture-status" :class="statusClass(picture.pictureCheck)">
                {{ pictureStatusLabel(picture.pictureCheck) }}
              </a-tag>
              <a-button
                class="space-picture-delete"
                size="small"
                danger
                aria-label="删除图片"
                @click.stop="confirmDeletePicture(picture)"
              ><DeleteOutlined aria-hidden="true" /></a-button>
              <div class="space-picture-overlay">
                <strong>{{ picture.name || '未命名图片' }}</strong>
                <span>{{ picture.category || '未分类' }}</span>
              </div>
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
import { DeleteOutlined, MoreOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import {
  createSpace as createSpaceApi,
  deleteById,
  querySpaceById,
  updateById,
} from '../../../api/spaceController'
import {
  deletePicture,
  queryPicturePage,
} from '../../../api/pictureController'
import { getCurrentUser } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { formatSpaceLevel, pictureStatusText } from '../prototypeData'
import SpaceNameModal from './components/SpaceNameModal.vue'

type PictureStatus = 0 | 1 | 2
type PictureStatusFilter = PictureStatus | 'all'

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
const allPictureTotal = ref<number | null>(null)
const pictureCheck = ref<PictureStatusFilter>('all')
const current = ref(1)
const pageSize = 9
const statusOptions: Array<{ label: string; value: PictureStatusFilter }> = [
  { label: '全部', value: 'all' as const },
  { label: '已通过', value: 1 },
  { label: '待审核', value: 0 },
  { label: '未通过', value: 2 },
]

const sizePercent = computed(() => usagePercent(space.value?.usedSize, space.value?.maxSize))
const totalPictureCount = computed(() => allPictureTotal.value ?? toNumber(space.value?.usedCount))
const pictureSummaryText = computed(() => {
  if (pictureCheck.value === 'all') return `${pictureTotal.value} 张图片`
  return `${pictureTotal.value} 张${pictureStatusText(pictureCheck.value)}图片`
})
const pictureEmptyText = computed(() => {
  if (pictureCheck.value === 'all') return '空间中暂无图片'
  return `空间中暂无${pictureStatusText(pictureCheck.value)}图片`
})

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

function statusClass(status?: number) {
  return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait'
}

function pictureStatusLabel(status?: number) {
  return status === 1 ? '已通过' : status === 2 ? '未通过' : '待审核'
}

function hasSpaceId(user?: API.UserVO | null) {
  return user?.spaceId != null && String(user.spaceId) !== '0'
}

async function refreshCurrentUser() {
  const res = await getCurrentUser()
  if (res.data?.code !== 200 || !res.data.data) {
    if (res.data?.code === 40100) {
      loginUserStore.clearLoginUser()
      await router.replace({
        path: '/user/login',
        query: { redirect: '/space' },
      })
      return null
    }
    throw new Error(res.data?.message || '当前用户信息加载失败')
  }
  loginUserStore.setLoginUser(res.data.data)
  return res.data.data
}

async function loadSpace(spaceId: number | string) {
  const res = await querySpaceById({ spaceId })
  if (res.data?.code !== 200 || !res.data.data) {
    throw new Error(res.data?.message || '空间信息加载失败')
  }
  space.value = res.data.data
}

/**
 * 后端省略 pictureCheck 时默认只查审核通过。
 * 因此“全部”状态由前端分别读取0/1/2，再合并成一个按时间排序的列表。
 */
async function loadPictures() {
  if (!space.value?.id) return
  pictureLoading.value = true
  pictureError.value = ''
  try {
    if (pictureCheck.value === 'all') {
      const allPageSize = Math.max(
        pageSize,
        toNumber(space.value.usedCount),
        toNumber(space.value.maxCount),
      )
      const responses = await Promise.all(
        ([0, 1, 2] as const).map((status) => queryPicturePage({
          spaceId: space.value!.id,
          pictureCheck: status,
          current: 1,
          pageSize: allPageSize,
          sortFiled: 'createtime',
          sortOrder: 'descend',
        })),
      )

      responses.forEach((response) => {
        if (response.data?.code !== 200) {
          throw new Error(response.data?.message || '空间图片加载失败')
        }
      })

      const uniquePictures = new Map<string, API.PictureVO>()
      responses
        .flatMap((response) => response.data.data?.pictureList || [])
        .forEach((picture) => uniquePictures.set(String(picture.id), picture))

      const mergedPictures = [...uniquePictures.values()]
        .sort((left, right) => pictureTime(right.createtime) - pictureTime(left.createtime))
      allPictureTotal.value = responses.reduce(
        (total, response) => total + toNumber(response.data.data?.total),
        0,
      )
      pictureTotal.value = allPictureTotal.value
      const start = (current.value - 1) * pageSize
      pictures.value = mergedPictures.slice(start, start + pageSize)
      return
    }

    const query: API.PictureQueryRequest = {
      spaceId: space.value.id,
      current: current.value,
      pageSize,
      sortFiled: 'createtime',
      sortOrder: 'descend',
    }

    query.pictureCheck = pictureCheck.value

    const res = await queryPicturePage(query)
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

function pictureTime(value?: string) {
  return value ? Date.parse(value.replace(' ', 'T')) || 0 : 0
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
      allPictureTotal.value = null
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
    const res = await createSpaceApi({ spaceName })
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
    const res = await updateById({ spaceId: space.value.id, updatedName: spaceName })
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
      const spaceId = space.value?.id
      if (spaceId == null) return
      const res = await deleteById({ spaceId })
      if (res.data?.code !== 200) throw new Error(res.data?.message || '空间删除失败')
      await refreshCurrentUser()
      space.value = null
      pictures.value = []
      pictureTotal.value = 0
      allPictureTotal.value = null
      message.success('私人空间已删除')
    },
  })
}

function handleSpaceMenuClick(event: { key: string }) {
  if (event.key === 'rename') renameOpen.value = true
  if (event.key === 'delete') confirmDeleteSpace()
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
      const res = await deletePicture({ id: picture.id })
      if (res.data?.code !== 200) throw new Error(res.data?.message || '图片删除失败')
      if (pictures.value.length === 1 && current.value > 1) current.value -= 1
      await Promise.all([loadSpace(space.value!.id!), loadPictures()])
      message.success('图片已删除')
    },
  })
}

function changeStatus(value: PictureStatusFilter) {
  if (pictureCheck.value === value) return
  pictureCheck.value = value
  current.value = 1
  void loadPictures()
}

function changePage(page: number) {
  current.value = page
  void loadPictures()
}

function openPicture(id?: number | string) {
  if (!id) return
  void router.push(`/gallery/detail/${encodeURIComponent(String(id))}`)
}

function openSpaceUpload() {
  void router.push({ path: '/gallery/upload', query: { target: 'space' } })
}

onMounted(loadPage)
</script>

<style scoped>
/* 空间页优先展示真实图片，顶部只保留必要的空间信息。 */
.space-prototype { padding-top: 16px; padding-bottom: 24px; }
.space-skeleton { padding-top: 12px; }
.space-skeleton-grid { margin-top: 16px; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(0, .85fr); gap: var(--prototype-layout-gap); }
.space-skeleton-grid > * { min-height: 300px; padding: 20px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.45); }
.space-empty {
  width: 100%;
  min-height: 340px;
  margin: 18px 0 0;
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) minmax(300px, .84fr);
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid var(--proto-line);
  background: rgba(255, 255, 255, .42);
  box-shadow: var(--proto-shadow);
}
.space-empty-main {
  min-width: 0;
  padding: 34px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 26px;
}
.space-empty-copy { display: flex; flex-direction: column; align-items: flex-start; gap: 14px; }
.space-empty h1 {
  max-width: none;
  margin: 0;
  font-size: 48px;
  line-height: .98;
  letter-spacing: -.06em;
}
.space-empty h1 span { display: block; white-space: nowrap; }
.space-empty-copy p {
  max-width: 46ch;
  margin: 0;
  color: var(--proto-muted);
  font-size: 14px;
  line-height: 1.55;
  text-wrap: pretty;
}
.space-empty-action { display: flex; align-items: center; gap: 0; }
.space-empty-action .acid-button {
  min-width: 136px;
  height: 44px;
  padding-inline: 20px;
  font-size: 14px;
}
.space-empty-spec {
  padding: 28px 30px;
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
.space-empty-spec dl { margin: 12px 0 0; }
.space-empty-spec dl div {
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid rgba(241,242,237,.15);
}
.space-empty-spec dt { color: rgba(241,242,237,.58); font-size: 11px; }
.space-empty-spec dd { margin: 0; font-size: 18px; font-weight: 700; letter-spacing: -.03em; }
.space-prototype > .proto-page-head { padding-top: 0; padding-bottom: 0; align-items: center; }
.space-heading-row { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.space-heading-main { min-width: 0; display: flex; align-items: flex-end; gap: 16px; }
.space-heading-copy { min-width: 0; display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px 14px; }
.space-prototype > .proto-page-head .proto-title { max-width: min(58vw, 560px); margin: 0; overflow: hidden; font-size: clamp(30px, 3vw, 42px); line-height: 1.06; text-overflow: ellipsis; white-space: nowrap; }
.space-heading-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.space-meta-tag { padding: 5px 9px; border: 1px solid rgba(17,20,22,.1); border-radius: 999px; background: rgba(255,255,255,.54); color: var(--proto-muted); font-size: 11px; line-height: 1.1; }
.space-meta-tag.is-count { color: var(--proto-ink); font-family: 'DM Mono', monospace; }
.space-rename-button { height: 32px; align-self: flex-end; padding-inline: 12px; flex: 0 0 auto; font-size: 12px; line-height: 1; }
.space-more-button { display: inline-flex; width: 36px; height: 36px; flex: 0 0 36px; align-items: center; justify-content: center; padding: 0; border: 1px solid var(--proto-line); border-radius: 8px; background: rgba(255,255,255,.58); color: var(--proto-ink); cursor: pointer; font-size: 18px; transition: border-color .2s ease, background-color .2s ease; }
.space-more-button:hover, .space-more-button:focus-visible { border-color: var(--proto-ink); background: var(--proto-paper-deep); outline: none; }

/* 用一条紧凑信息带替代大面积仪表盘，让图片更早进入首屏。 */
.space-usage-strip { margin-top: 16px; padding: 14px 18px; display: grid; grid-template-columns: auto minmax(220px, 1fr) auto auto; align-items: center; gap: 22px; border-radius: 13px; background: rgba(255,255,255,.58); box-shadow: 0 10px 28px rgba(18,23,23,.045); }
.usage-strip-intro { display: flex; align-items: baseline; gap: 9px; white-space: nowrap; }
.usage-strip-intro span, .usage-strip-meter-head small, .usage-strip-stat span, .usage-strip-privacy small { color: var(--proto-muted); font-size: 11px; }
.usage-strip-intro strong { color: var(--proto-ink); font-family: 'DM Mono', monospace; font-size: 24px; font-weight: 500; letter-spacing: -.08em; }
.usage-strip-meter { min-width: 0; }
.usage-strip-meter-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; margin-bottom: 5px; }
.usage-strip-meter-head span { color: var(--proto-ink-soft); font-family: 'DM Mono', monospace; font-size: 11px; }
.usage-strip-meter :deep(.ant-progress) { display: block; margin: 0; line-height: 1; }
.usage-strip-meter :deep(.ant-progress-inner) { border-radius: 999px; }
.usage-strip-stat, .usage-strip-privacy { min-width: 0; padding-left: 20px; border-left: 1px solid var(--proto-line); }
.usage-strip-stat span, .usage-strip-stat strong, .usage-strip-privacy span, .usage-strip-privacy small { display: block; }
.usage-strip-stat strong { margin-top: 4px; color: var(--proto-ink); font-family: 'DM Mono', monospace; font-size: 12px; font-weight: 500; white-space: nowrap; }
.usage-strip-privacy { padding: 10px 14px; border: 0; border-radius: 10px; background: var(--proto-ink); color: var(--proto-paper); }
.usage-strip-privacy span { color: var(--proto-paper); font-size: 12px; font-weight: 700; }
.usage-strip-privacy small { margin-top: 4px; color: rgba(241,242,237,.7); white-space: nowrap; }

/* 图片标题与筛选操作保持同一层级，先浏览，再进行状态筛选。 */
.space-gallery.proto-section { padding-top: 22px; }
.space-gallery-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 14px; }
.space-gallery-title { min-width: 0; display: flex; align-items: baseline; gap: 14px; }
.space-gallery-title .proto-subtitle { margin: 0; font-size: 30px; line-height: 1.1; }
.space-gallery-count { color: var(--proto-muted); font-size: 12px; }
.space-gallery-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.space-status-tabs { display: flex; align-items: center; gap: 3px; padding: 3px; border: 1px solid var(--proto-line); border-radius: 9px; background: rgba(255,255,255,.52); }
.space-status-tab { min-height: 31px; padding: 0 10px; display: inline-flex; align-items: center; gap: 6px; border: 0; border-radius: 6px; background: transparent; color: var(--proto-muted); cursor: pointer; font-family: inherit; font-size: 11px; font-weight: 700; transition: color .2s ease, background-color .2s ease; }
.space-status-tab strong { color: inherit; font-family: 'DM Mono', monospace; font-size: 10px; font-weight: 500; }
.space-status-tab:hover { background: var(--proto-paper-deep); color: var(--proto-ink); }
.space-status-tab.is-active { background: var(--proto-ink); color: var(--proto-paper); }
.space-status-tab.is-active strong { color: var(--proto-acid); }
.space-picture-alert { margin-bottom: 14px; }
.space-picture-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.space-picture-skeleton { min-height: 210px; padding: 18px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.45); }
.space-picture-card { min-width: 0; overflow: hidden; border: 0; border-radius: 12px; background: transparent; cursor: pointer; transition: box-shadow .2s ease; }
.space-picture-card:hover, .space-picture-card:focus-visible { box-shadow: 0 10px 26px rgba(18,23,23,.12); }
.space-picture-image { position: relative; aspect-ratio: 4 / 3; overflow: hidden; border: 1px solid rgba(17,20,22,.08); border-radius: 12px; background: var(--proto-paper-deep); }
.space-picture-image::after { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(17,20,22,0) 45%, rgba(17,20,22,.78) 100%); content: ''; pointer-events: none; }
.space-picture-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.space-picture-status { position: absolute; z-index: 2; top: 10px; left: 10px; margin: 0; padding: 4px 8px; border: 0; border-radius: 999px; background: rgba(246,247,242,.9); color: var(--proto-ink); font-size: 10px; font-weight: 700; line-height: 1.2; }
.space-picture-status.pass { background: rgba(224,244,186,.94); }
.space-picture-status.wait { background: rgba(255,195,77,.94); }
.space-picture-status.refuse { background: rgba(255,152,125,.94); }
.space-picture-delete { position: absolute; z-index: 3; top: 9px; right: 9px; width: 30px; height: 30px; padding: 0; display: inline-flex; align-items: center; justify-content: center; border: 0; border-radius: 50%; background: rgba(246,247,242,.92); color: var(--proto-ink); opacity: 0; transition: opacity .2s ease, background-color .2s ease; }
.space-picture-delete:hover { background: #ff987d; color: var(--proto-ink); }
.space-picture-card:hover .space-picture-delete, .space-picture-card:focus-within .space-picture-delete { opacity: 1; }
.space-picture-overlay { position: absolute; z-index: 2; right: 12px; bottom: 10px; left: 12px; display: flex; align-items: baseline; justify-content: space-between; gap: 8px; color: var(--proto-paper); }
.space-picture-overlay strong { min-width: 0; overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.space-picture-overlay span { flex: 0 0 auto; color: rgba(241,242,237,.75); font-size: 10px; }
.space-picture-empty { min-height: 240px; padding: 30px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.42); }
.space-pagination { margin-top: 16px; display: flex; justify-content: flex-end; }

@media (max-width: 1120px) {
  .space-picture-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 980px) {
  .space-heading-row, .space-gallery-head { align-items: flex-start; }
  .space-gallery-head { flex-direction: column; }
  .space-gallery-actions { width: 100%; justify-content: space-between; }
  .space-usage-strip { grid-template-columns: minmax(0, 1fr) minmax(0, 1.7fr); }
  .usage-strip-stat { padding-top: 10px; padding-bottom: 2px; }
}
@media (max-width: 820px) {
  .space-picture-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .space-heading-main { width: 100%; align-items: flex-start; }
  .space-heading-copy { align-items: flex-start; flex-direction: column; gap: 8px; }
  .space-prototype > .proto-page-head .proto-title { max-width: 100%; white-space: normal; }
  .space-usage-strip { grid-template-columns: 1fr 1fr; gap: 14px 18px; }
  .usage-strip-intro, .usage-strip-meter { grid-column: span 1; }
  .usage-strip-stat, .usage-strip-privacy { grid-column: span 1; }
  .space-gallery-actions { align-items: stretch; flex-direction: column; }
  .space-status-tabs { width: 100%; }
  .space-status-tab { flex: 1; justify-content: center; }
  .space-gallery-actions .acid-button { width: 100%; }
}
@media (max-width: 580px) {
  .space-empty { margin-top: 18px; }
  .space-empty-main { padding: 28px 20px; }
  .space-empty h1 { font-size: 40px; }
  .space-empty h1 span { white-space: normal; }
  .space-heading-row { gap: 12px; }
  .space-heading-main { gap: 10px; }
  .space-heading-meta { gap: 5px; }
  .space-meta-tag { padding-inline: 7px; font-size: 10px; }
  .space-rename-button { padding-inline: 9px; }
  .space-usage-strip { grid-template-columns: 1fr; padding: 15px; }
  .usage-strip-intro, .usage-strip-meter, .usage-strip-stat, .usage-strip-privacy { grid-column: auto; }
  .usage-strip-stat { padding-top: 0; padding-bottom: 0; padding-left: 0; border-left: 0; }
  .space-picture-grid { grid-template-columns: 1fr; }
  .space-status-tabs { overflow-x: auto; }
  .space-status-tab { flex: 0 0 auto; padding-inline: 9px; }
}
</style>
