<template>
  <div class="detail-prototype">
    <section class="detail-head">
      <div v-if="picture" class="detail-title-bar">
        <h1 class="detail-image-title">{{ picture.name || '未命名图片' }}</h1>
        <a-tag class="detail-title-status proto-status" :class="statusClass">
          {{ pictureStatusText(picture.pictureCheck) }}
        </a-tag>
      </div>
      <div class="proto-page-head-actions">
        <a-button class="proto-button ghost-button" @click="router.push('/gallery')">返回图库</a-button>
        <a-button class="proto-button acid-button" type="primary" @click="router.push('/gallery/manage')">管理我的图片</a-button>
      </div>
    </section>

    <div v-if="loading" class="detail-state">
      <div class="detail-loading-shell" aria-label="正在加载图片详情">
        <div class="detail-loading-image"></div>
        <div class="detail-loading-info">
          <a-skeleton active :paragraph="{ rows: 5 }" />
        </div>
      </div>
    </div>

    <a-result v-else-if="errorMessage" class="detail-state detail-result" status="warning" title="图片详情暂时无法打开" :sub-title="errorMessage">
      <template #extra>
        <a-button class="proto-button acid-button" type="primary" @click="fetchPictureDetail">重新加载</a-button>
        <a-button class="proto-button ghost-button" @click="router.push('/gallery')">返回图库</a-button>
      </template>
    </a-result>

    <section v-else-if="picture" class="detail-layout proto-section">
      <div class="detail-main-visual">
        <div class="detail-visual proto-surface proto-rounded">
          <div class="detail-image proto-image-wrap">
            <a-image
              v-if="picture.url"
              class="detail-image-preview"
              :src="picture.url"
              :alt="picture.name || '图片详情'"
              :preview="true"
            />
            <div v-else class="detail-image-empty">暂无图片地址</div>
          </div>
          <div class="detail-image-caption">
            <span>{{ picture.picwidth || '-' }} × {{ picture.picheight || '-' }} px</span>
            <span>{{ picture.picformat?.toUpperCase() || '-' }} / {{ formatPictureSize(picture.picsize) }}</span>
          </div>
        </div>
      </div>

      <aside class="detail-info">
        <div class="detail-introduction-block">
          <span class="detail-section-label">图片简介</span>
          <p class="detail-introduction">{{ picture.introduction || '暂无图片简介' }}</p>
        </div>
        <div v-if="picture.tags?.length" class="detail-tags">
          <a-tag v-for="tag in picture.tags" :key="tag" class="proto-tag acid-tag">{{ tag }}</a-tag>
        </div>

        <a-descriptions
          class="detail-descriptions"
          bordered
          size="small"
          layout="vertical"
          :column="{ xxl: 2, xl: 2, lg: 2, md: 2, sm: 1, xs: 1 }"
        >
          <a-descriptions-item label="分类">{{ picture.category || '未分类' }}</a-descriptions-item>
          <a-descriptions-item label="空间">{{ formatSpaceName(picture.spaceId) }}</a-descriptions-item>
          <a-descriptions-item label="比例">{{ formatRatio(picture.picwidth, picture.picheight) }}</a-descriptions-item>
          <a-descriptions-item label="格式 / 大小">
            {{ picture.picformat?.toUpperCase() || '-' }} / {{ formatPictureSize(picture.picsize) }}
          </a-descriptions-item>
          <a-descriptions-item label="上传时间" :span="2">{{ formatDateTime(picture.createtime) }}</a-descriptions-item>
        </a-descriptions>

        <div v-if="picture.createdUser" class="detail-uploader">
          <a-avatar
            class="detail-uploader-avatar"
            :src="picture.createdUser.avatarurl"
            :size="42"
          >
            {{ picture.createdUser.username?.charAt(0) || '?' }}
          </a-avatar>
          <div class="detail-uploader-copy">
            <span class="detail-uploader-label">上传者</span>
            <strong>{{ picture.createdUser.username || '未知用户' }}</strong>
          </div>
        </div>

        <div v-if="canSaveToSpace" class="detail-save-panel">
          <div class="detail-save-copy">
            <strong>保存到我的空间</strong>
            <p>{{ saveHint }}</p>
          </div>
          <a-button
            class="detail-save-button"
            :loading="saving"
            :disabled="saving || saved"
            @click="handleSaveClick"
          >
            {{ saved ? '已保存' : '保存到我的空间' }}
          </a-button>
        </div>

        <div v-if="picture.pictureCheck === 2 && picture.checkMessage" class="detail-review-note">
          <strong>审核备注</strong>
          <p>{{ picture.checkMessage }}</p>
        </div>
      </aside>
    </section>

    <!-- 没有个人空间时复用已有弹窗，创建完成后会自动继续保存当前图片。 -->
    <SpaceNameModal
      v-model:open="createSpaceOpen"
      mode="create"
      :submitting="creatingSpace"
      @submit="handleCreateSpace"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { getPictureById, save2Space } from '../../../api/pictureController'
import { createSpace } from '../../../api/spaceController'
import SpaceNameModal from '../space/components/SpaceNameModal.vue'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { pictureStatusText } from '../prototypeData'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(true)
const errorMessage = ref('')
const picture = ref<API.PictureVO | null>(null)
const saving = ref(false)
const saved = ref(false)
const createSpaceOpen = ref(false)
const creatingSpace = ref(false)

const statusClass = computed(() => {
  if (picture.value?.pictureCheck === 1) return 'pass'
  if (picture.value?.pictureCheck === 2) return 'refuse'
  return 'wait'
})

// 只有审核通过的公共图片才展示“保存到我的空间”入口。
const canSaveToSpace = computed(() => {
  const spaceId = picture.value?.spaceId
  return String(spaceId ?? '') === '0' && picture.value?.pictureCheck === 1
})

const hasPrivateSpace = computed(() => {
  const spaceId = loginUserStore.loginUser?.spaceId
  return spaceId !== undefined && spaceId !== null && String(spaceId) !== '' && String(spaceId) !== '0'
})

const saveHint = computed(() => {
  if (saved.value) return '已创建一份副本，原图不受影响。'
  if (!loginUserStore.loginUser) return '登录后即可创建一份副本，原图不受影响。'
  if (!hasPrivateSpace.value) return '你还没有个人空间，点击按钮即可创建并保存。'
  return '创建一份副本到你的空间，原图不受影响。'
})

function getPictureId() {
  const rawValue = route.params.id
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue
  const normalizedValue = String(value || '').trim()
  return /^\d+$/.test(normalizedValue) ? normalizedValue : undefined
}

async function fetchPictureDetail() {
  const id = getPictureId()
  if (!id) {
    picture.value = null
    errorMessage.value = '图片 ID 不存在或格式不正确'
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''
  saved.value = false
  try {
    const res = await getPictureById({ id })
    if (res.data?.code !== 200 || !res.data.data) {
      throw new Error(res.data?.message || '图片不存在或已被删除')
    }
    picture.value = res.data.data
  } catch (error: any) {
    picture.value = null
    errorMessage.value = error?.response?.data?.message || error?.message || '图片详情加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

/** 调用后端保存接口，spaceId 保持 number|string，避免 Long 在前端精度丢失。 */
async function savePictureToSpace(spaceId: number | string) {
  const pictureId = picture.value?.id
  if (pictureId === undefined || pictureId === null || saving.value) return

  saving.value = true
  try {
    const res = await save2Space({ pictureId, spaceId })
    if (res.data?.code !== 200 || !res.data.data) {
      throw new Error(res.data?.message || '保存图片失败')
    }
    saved.value = true
    message.success('图片已保存到我的空间')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存图片失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

/** 点击保存按钮：未登录先登录，没有空间则先打开创建空间弹窗。 */
async function handleSaveClick() {
  if (!picture.value?.id || saving.value || saved.value) return

  if (!loginUserStore.loginUser) {
    message.info('请先登录，再保存图片')
    await router.push({ path: '/user/login', query: { redirect: route.fullPath } })
    return
  }

  const spaceId = loginUserStore.loginUser.spaceId
  if (!hasPrivateSpace.value || spaceId === undefined || spaceId === null) {
    message.info('你还没有个人空间，请先创建一个空间')
    createSpaceOpen.value = true
    return
  }

  await savePictureToSpace(spaceId)
}

/** 创建空间成功后同步登录用户状态，并自动继续保存当前图片。 */
async function handleCreateSpace(spaceName: string) {
  if (creatingSpace.value) return

  creatingSpace.value = true
  try {
    const res = await createSpace({ spaceName })
    const createdSpace = res.data?.data
    if (res.data?.code !== 200 || !createdSpace?.id) {
      throw new Error(res.data?.message || '创建空间失败')
    }

    const currentUser = loginUserStore.loginUser
    if (currentUser) {
      loginUserStore.setLoginUser({ ...currentUser, spaceId: createdSpace.id })
    }
    createSpaceOpen.value = false
    await savePictureToSpace(createdSpace.id)
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '创建空间失败，请稍后重试')
  } finally {
    creatingSpace.value = false
  }
}

function formatPictureSize(bytes?: number | string) {
  const normalizedBytes = Number(bytes)
  if (!Number.isFinite(normalizedBytes) || normalizedBytes <= 0) return '-'
  if (normalizedBytes < 1024) return `${normalizedBytes} B`
  if (normalizedBytes < 1024 * 1024) return `${(normalizedBytes / 1024).toFixed(1)} KB`
  return `${(normalizedBytes / 1024 / 1024).toFixed(1)} MB`
}

function formatRatio(width?: number, height?: number) {
  if (!width || !height) return '-'
  return (width / height).toFixed(2)
}

// 后端时间保留原始时刻，只统一为页面需要的 yyyy-MM-dd HH:mm:ss 格式。
function formatDateTime(value?: string) {
  if (!value) return '-'
  const match = String(value).match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2}:\d{2})/)
  return match ? `${match[1]} ${match[2]}` : String(value)
}

// Long 类型空间 ID 可能以字符串返回，只做字符串比较，不转换为 JavaScript Number。
function formatSpaceName(spaceId?: number | string) {
  if (spaceId === undefined || spaceId === null || spaceId === '') return '未指定'
  const normalizedSpaceId = String(spaceId)
  return normalizedSpaceId === '0' ? '公共图库' : `空间 #${normalizedSpaceId}`
}

onMounted(() => {
  void fetchPictureDetail()
})

watch(() => route.params.id, () => {
  void fetchPictureDetail()
})
</script>

<style scoped>
.detail-prototype {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: var(--proto-ink);
  font-family: 'Manrope', 'Geist', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-kerning: normal;
}

.detail-prototype :deep(.ant-btn),
.detail-prototype :deep(.ant-tag),
.detail-prototype :deep(.ant-descriptions),
.detail-prototype :deep(.ant-result),
.detail-prototype :deep(.ant-skeleton) {
  font-family: inherit;
}

.detail-head {
  flex: 0 0 auto;
  min-height: 56px;
  padding-top: 4px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid transparent;
}

.detail-title-bar {
  min-width: 0;
  flex: 1 1 auto;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.detail-head .proto-page-head-actions {
  padding-bottom: 0;
}

.detail-head :deep(.proto-button) {
  height: 37px;
  padding-inline: 14px;
  font-size: .75rem;
}

.detail-state {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  place-items: center;
}

.detail-loading-shell {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  /* 加载态与完成态保持相同列宽，避免内容出现时页面突然跳动。 */
  /* 桌面加载态固定主要图片区占比，保证加载完成后布局不跳动。 */
  grid-template-columns: minmax(0, 3fr) minmax(280px, 1fr);
  gap: var(--prototype-layout-gap);
  padding-top: 12px;
}

.detail-loading-image {
  min-height: 260px;
  border: 1px solid var(--proto-line);
  border-radius: 10px;
  background: var(--proto-paper-deep);
}

.detail-loading-info {
  padding: 14px 4px;
}

.detail-loading-info :deep(.ant-skeleton-title),
.detail-loading-info :deep(.ant-skeleton-paragraph > li) {
  background: rgba(17, 20, 22, .12);
}

.detail-result {
  color: var(--proto-ink);
}

.detail-result :deep(.ant-result-title),
.detail-result :deep(.ant-result-subtitle) {
  color: var(--proto-ink);
}

.detail-result :deep(.ant-result-subtitle) {
  color: var(--proto-ink-soft);
}

.detail-layout {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  /* 桌面端让图片承担主要视觉面积，右侧信息栏保留可读的最小宽度。 */
  /* 桌面端固定图片区为主要区域，右侧信息栏自适应剩余空间。 */
  grid-template-columns: minmax(0, 3fr) minmax(280px, 1fr);
  gap: var(--prototype-layout-gap);
  align-items: stretch;
  padding-top: 12px;
  padding-bottom: 10px;
  overflow: hidden;
}

.detail-main-visual {
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-image-title {
  flex: 0 1 auto;
  min-width: 0;
  margin: 0;
  padding: 0 4px;
  color: var(--proto-ink);
  font-size: 2.1rem;
  line-height: 1.08;
  letter-spacing: -.04em;
  font-weight: 800;
  text-wrap: balance;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail-title-status {
  flex: 0 0 auto;
  margin-bottom: 0;
  padding: 3px 8px;
  font-size: .6875rem;
  line-height: 1.2;
  white-space: nowrap;
  transform: translateY(4px);
}

.detail-visual {
  min-height: 0;
  width: 100%;
  height: auto;
  max-height: none;
  flex: 1 1 auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, .64);
}

.detail-image {
  width: 100%;
  min-height: 0;
  flex: 1 1 auto;
  height: auto;
  max-height: none;
  border-radius: 7px;
  overflow: hidden;
}

.detail-image :deep(.ant-image),
.detail-image :deep(.ant-image-img) {
  display: block;
  width: 100%;
  height: 100%;
}

.detail-image :deep(.ant-image-img) {
  object-fit: contain;
  background: var(--proto-paper-deep);
}

.detail-image-empty {
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--proto-ink-soft);
  font-size: .875rem;
}

.detail-image-caption {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 3px 0;
  color: var(--proto-ink-soft);
  font-size: .75rem;
  line-height: 1.3;
  font-variant-numeric: tabular-nums;
}

.detail-info {
  min-height: 0;
  padding: 4px 4px 0;
  overflow: hidden;
}

.detail-introduction-block {
  margin-top: 0;
  padding: 0 0 12px;
  border-bottom: 1px solid var(--proto-line);
}

.detail-section-label {
  display: block;
  color: var(--proto-ink-soft);
  font-size: .75rem;
  line-height: 1.25;
  font-weight: 700;
}

.detail-introduction {
  max-width: none;
  margin: 6px 0 0;
  color: var(--proto-ink-soft);
  font-size: .9375rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail-tags {
  margin-top: 12px;
}

.detail-tags :deep(.proto-tag) {
  margin: 0 6px 6px 0;
  padding: 4px 8px;
  color: var(--proto-ink);
  font-size: .75rem;
  line-height: 1.2;
}

.detail-descriptions {
  margin-top: 14px;
}

.detail-descriptions :deep(.ant-descriptions-view) {
  border-color: var(--proto-line);
}

.detail-descriptions :deep(table) {
  table-layout: fixed;
}

.detail-descriptions :deep(.ant-descriptions-item-label),
.detail-descriptions :deep(.ant-descriptions-item-content) {
  padding-inline: 12px;
  border-color: var(--proto-line);
  color: var(--proto-ink);
}

.detail-descriptions :deep(.ant-descriptions-item-label) {
  padding-top: 9px;
  padding-bottom: 4px;
  background: rgba(228, 231, 223, .72);
  color: var(--proto-ink-soft);
  font-size: .75rem;
  line-height: 1.25;
  white-space: nowrap;
}

.detail-descriptions :deep(.ant-descriptions-item-content) {
  padding-top: 4px;
  padding-bottom: 10px;
  background: rgba(255, 255, 255, .4);
  font-size: .875rem;
  line-height: 1.3;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.detail-uploader {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px 0;
  border-top: 1px solid var(--proto-line);
  border-bottom: 1px solid var(--proto-line);
}

.detail-uploader-avatar.ant-avatar {
  flex: 0 0 auto;
  background: var(--proto-acid);
  color: var(--proto-ink);
  font-weight: 800;
}

.detail-uploader-copy {
  min-width: 0;
}

.detail-uploader-label,
.detail-uploader-copy strong {
  display: block;
}

.detail-uploader-label {
  color: var(--proto-ink-soft);
  font-size: .75rem;
}

.detail-uploader-copy strong {
  margin-top: 4px;
  color: var(--proto-ink);
  font-size: .9375rem;
  overflow-wrap: anywhere;
}

.detail-save-panel {
  margin-top: 16px;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-top: 1px solid var(--proto-line);
}

.detail-save-copy {
  min-width: 0;
}

.detail-save-copy strong {
  display: block;
  color: var(--proto-ink);
  font-size: .9375rem;
}

.detail-save-copy p {
  margin: 4px 0 0;
  color: var(--proto-ink-soft);
  font-size: .75rem;
  line-height: 1.45;
}

.detail-save-button.ant-btn {
  flex: 0 0 auto;
  color: var(--proto-ink-soft);
  background: rgba(186, 255, 61, .46);
  border-color: rgba(109, 151, 18, .35);
  box-shadow: none;
  font-size: .75rem;
  font-weight: 700;
}

.detail-save-button.ant-btn[disabled] {
  color: var(--proto-ink-soft);
  background: rgba(186, 255, 61, .46);
  border-color: rgba(109, 151, 18, .35);
  cursor: not-allowed;
}

.detail-review-note {
  margin-top: 14px;
  padding: 11px 13px;
  border: 1px solid rgba(255, 137, 106, .55);
  border-radius: 8px;
  background: rgba(255, 137, 106, .16);
  color: var(--proto-ink);
  font-size: .8125rem;
  line-height: 1.5;
}

.detail-review-note strong {
  font-size: .8125rem;
}

.detail-review-note p {
  margin: 5px 0 0;
  color: var(--proto-ink-soft);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 980px) {
  .detail-layout,
  .detail-loading-shell {
    /* 仍沿用桌面端 3:1 主次关系，直到移动端真正需要上下堆叠。 */
    grid-template-columns: minmax(0, 3fr) minmax(260px, 1fr);
    gap: var(--prototype-layout-gap);
  }

  .detail-image-title {
    font-size: 1.7rem;
  }
}

@media (max-width: 800px) {
  .detail-prototype {
    height: auto;
    overflow: visible;
  }

  .detail-head {
    align-items: flex-start;
    flex-direction: column;
    padding-top: 10px;
    justify-content: flex-start;
  }

  .detail-title-bar {
    width: 100%;
  }

  .detail-head .proto-page-head-actions {
    justify-content: flex-start;
  }

  .detail-state {
    min-height: 360px;
  }

  .detail-loading-shell {
    height: auto;
    min-height: 520px;
    grid-template-columns: 1fr;
  }

  .detail-layout {
    display: grid;
    grid-template-columns: 1fr;
    flex: none;
    min-height: auto;
    overflow: visible;
  }

  .detail-main-visual {
    height: auto;
  }

  .detail-visual {
    height: auto;
    flex: none;
  }

  .detail-image {
    height: min(66vh, 520px);
    max-height: none;
    flex: none;
  }

  .detail-image-title {
    font-size: 1.5rem;
  }

  .detail-info {
    overflow: visible;
    padding-top: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .detail-prototype :deep(.ant-image-img) {
    transition: none;
  }
}
</style>
