<template>
  <div class="detail-prototype">
    <section class="detail-head">
      <button class="detail-back-button" type="button" @click="router.push('/gallery')">
        <span aria-hidden="true">←</span>
        <span>返回公共图库</span>
      </button>
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

    <section v-else-if="picture" class="detail-layout">
      <div class="detail-main-visual">
        <article class="detail-visual">
          <div class="detail-image-stage">
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
            <span>{{ picture.picformat?.toUpperCase() || '-' }} · {{ formatPictureSize(picture.picsize) }}</span>
          </div>
        </article>
      </div>

      <aside class="detail-info-card">
        <div class="detail-info-header">
          <div class="detail-heading-copy">
            <div class="detail-title-row">
              <h1 class="detail-image-title">{{ picture.name || '未命名图片' }}</h1>
              <a-tag class="detail-title-status proto-status" :class="statusClass">
                {{ pictureStatusText(picture.pictureCheck) }}
              </a-tag>
            </div>
            <p v-if="picture.introduction" class="detail-introduction">{{ picture.introduction }}</p>
          </div>

          <a-dropdown placement="bottomRight" :trigger="['click']">
            <a-button class="detail-more-button" type="text" aria-label="更多操作">···</a-button>
            <template #overlay>
              <a-menu @click="handleMoreMenuClick">
                <a-menu-item key="back">返回公共图库</a-menu-item>
                <a-menu-item key="manage">进入图片管理</a-menu-item>
                <a-menu-item v-if="canSaveToSpace" key="save" :disabled="saving || saved">
                  {{ saved ? '已保存到我的空间' : '保存到我的空间' }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-if="picture.tags?.length" class="detail-tags">
          <a-tag v-for="tag in picture.tags" :key="tag" class="detail-tag">{{ tag }}</a-tag>
        </div>

        <section class="detail-metadata" aria-labelledby="detail-metadata-title">
          <h2 id="detail-metadata-title">图片信息</h2>
          <dl class="detail-metadata-list">
            <div class="detail-metadata-row">
              <dt>分类</dt>
              <dd>{{ picture.category || '未分类' }}</dd>
            </div>
            <div class="detail-metadata-row">
              <dt>所属空间</dt>
              <dd class="detail-space-value">{{ formatSpaceName(picture.spaceId) }}</dd>
            </div>
            <div class="detail-metadata-row">
              <dt>尺寸</dt>
              <dd>{{ picture.picwidth || '-' }} × {{ picture.picheight || '-' }} px</dd>
            </div>
            <div class="detail-metadata-row">
              <dt>比例</dt>
              <dd>{{ formatRatio(picture.picwidth, picture.picheight) }}</dd>
            </div>
            <div class="detail-metadata-row">
              <dt>格式</dt>
              <dd>{{ picture.picformat?.toUpperCase() || '-' }}</dd>
            </div>
            <div class="detail-metadata-row">
              <dt>文件大小</dt>
              <dd>{{ formatPictureSize(picture.picsize) }}</dd>
            </div>
            <div class="detail-metadata-row">
              <dt>上传时间</dt>
              <dd>{{ formatDateTime(picture.createtime) }}</dd>
            </div>
          </dl>
        </section>

        <div v-if="picture.createdUser" class="detail-uploader">
          <h2>上传者</h2>
          <div class="detail-uploader-row">
            <a-avatar
              class="detail-uploader-avatar"
              :src="picture.createdUser.avatarurl"
              :size="42"
            >
              {{ picture.createdUser.username?.charAt(0) || '?' }}
            </a-avatar>
            <div class="detail-uploader-copy">
              <strong>{{ picture.createdUser.username || '未知用户' }}</strong>
            </div>
          </div>
        </div>

        <div v-if="picture.pictureCheck === 2 && picture.checkMessage" class="detail-review-note">
          <strong>审核备注</strong>
          <p>{{ picture.checkMessage }}</p>
        </div>

        <div class="detail-action-area">
          <a-button
            class="detail-manage-button"
            type="primary"
            block
            @click="router.push('/gallery/manage')"
          >
            管理图片
          </a-button>
          <p v-if="canSaveToSpace" class="detail-action-hint">{{ saveHint }}</p>
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

// 详情页的次要操作集中到更多菜单，主视觉区域只保留图片展示。
function handleMoreMenuClick(info: { key: string | number }) {
  const action = String(info.key)
  if (action === 'back') {
    void router.push('/gallery')
    return
  }
  if (action === 'manage') {
    void router.push('/gallery/manage')
    return
  }
  if (action === 'save') {
    void handleSaveClick()
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
  const normalizedWidth = Math.round(Number(width))
  const normalizedHeight = Math.round(Number(height))
  if (!Number.isFinite(normalizedWidth) || !Number.isFinite(normalizedHeight) || normalizedWidth <= 0 || normalizedHeight <= 0) {
    return '-'
  }

  // 用最简整数比展示比例，避免详情页出现后台计算风格的 1.00。
  let left = normalizedWidth
  let right = normalizedHeight
  while (right !== 0) {
    const remainder = left % right
    left = right
    right = remainder
  }
  return `${normalizedWidth / left} : ${normalizedHeight / left}`
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

@media (prefers-reduced-motion: reduce) {
  .detail-prototype :deep(.ant-image-img) {
    transition: none;
  }
}
.detail-prototype {
  height: auto;
  min-height: 100%;
  overflow: visible;
}

.detail-head {
  min-height: 72px;
  align-items: center;
  justify-content: flex-start;
  gap: 0;
  padding: 14px 0 12px;
  border-bottom-color: rgba(17, 20, 22, .08);
}

.detail-back-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--proto-ink);
  cursor: pointer;
  font: inherit;
  font-size: .9375rem;
  font-weight: 700;
}

.detail-back-button span:first-child {
  font-size: 1.5rem;
  font-weight: 400;
  line-height: 1;
  transform: translateY(-1px);
}

.detail-back-button:hover {
  color: #6a9419;
}

.detail-state {
  min-height: 420px;
  padding: 20px 0 24px;
}

.detail-loading-shell {
  width: 100%;
  height: auto;
  min-height: min(70vh, 760px);
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 32px;
  padding-top: 0;
}

.detail-loading-image {
  min-height: 420px;
  border-radius: 16px;
}

.detail-loading-info {
  padding: 28px 0;
}

.detail-layout {
  width: 100%;
  max-width: 1480px;
  margin-inline: auto;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 32px;
  align-items: start;
  padding: 16px 0 24px;
  overflow: visible;
}

.detail-main-visual {
  height: auto;
  min-height: 0;
  display: block;
}

.detail-visual {
  width: 100%;
  height: auto;
  min-height: 0;
  max-height: none;
  display: block;
  flex: none;
  padding: 12px 12px 18px;
  border: 1px solid var(--proto-line);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(17, 20, 22, .06);
}

.detail-image-stage {
  width: 100%;
  height: clamp(420px, calc(100dvh - 190px), 760px);
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 12px;
  background: var(--proto-paper-deep);
}

.detail-image-preview,
.detail-image-preview :deep(.ant-image),
.detail-image-preview :deep(.ant-image-img) {
  display: block;
  width: 100%;
  height: 100%;
}

.detail-image-preview :deep(.ant-image-img) {
  object-fit: contain;
  background: var(--proto-paper-deep);
}

.detail-image-empty {
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--proto-muted);
  font-size: .875rem;
}

.detail-image-caption {
  padding: 12px 2px 0;
  color: var(--proto-muted);
  font-size: .8125rem;
}

.detail-info-card {
  position: sticky;
  top: calc(var(--prototype-topbar-height) + 16px);
  min-height: min(70vh, 760px);
  max-height: calc(100dvh - var(--prototype-topbar-height) - 40px);
  overflow: auto;
  display: flex;
  flex-direction: column;
  padding: 28px 28px 24px;
  border: 1px solid var(--proto-line);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(17, 20, 22, .06);
}

.detail-info-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-heading-copy {
  min-width: 0;
  flex: 1 1 auto;
}

.detail-title-row {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-image-title {
  min-width: 0;
  margin: 0;
  padding: 0;
  color: var(--proto-ink);
  font-size: clamp(1.75rem, 2.8vw, 2.35rem);
  line-height: 1.08;
  letter-spacing: -.055em;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.detail-title-status {
  flex: 0 0 auto;
  margin: 0;
  padding: 4px 9px;
  border: 0;
  border-radius: 6px;
  font-size: .6875rem;
  line-height: 1.25;
  font-weight: 800;
  white-space: nowrap;
  transform: none;
}

.detail-title-status.pass {
  background: rgba(186, 255, 61, .28);
  color: #5e821b;
}

.detail-title-status.wait {
  background: rgba(255, 214, 100, .3);
  color: #8e6610;
}

.detail-title-status.refuse {
  background: rgba(255, 137, 106, .2);
  color: #a34f38;
}

.detail-more-button.ant-btn {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  padding: 0;
  border: 1px solid var(--proto-line);
  border-radius: 10px;
  color: var(--proto-ink);
  font-size: 1.25rem;
  letter-spacing: .12em;
  line-height: 1;
}

.detail-more-button.ant-btn:hover,
.detail-more-button.ant-btn:focus {
  border-color: rgba(106, 148, 25, .5);
  color: #6a9419;
  background: rgba(186, 255, 61, .12);
}

.detail-introduction {
  display: block;
  margin: 16px 0 0;
  color: var(--proto-muted);
  font-size: .9375rem;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.detail-tags {
  margin-top: 18px;
}

.detail-tag.ant-tag {
  margin: 0 6px 6px 0;
  padding: 4px 9px;
  border: 1px solid rgba(17, 20, 22, .1);
  border-radius: 999px;
  background: rgba(241, 242, 237, .72);
  color: var(--proto-muted);
  font-size: .6875rem;
  line-height: 1.25;
}

.detail-metadata {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--proto-line);
}

.detail-metadata h2,
.detail-uploader h2 {
  margin: 0 0 12px;
  color: var(--proto-ink);
  font-size: .9375rem;
  line-height: 1.3;
  font-weight: 800;
}

.detail-metadata-list {
  margin: 0;
}

.detail-metadata-row {
  min-height: 42px;
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid rgba(17, 20, 22, .09);
}

.detail-metadata-row dt {
  color: var(--proto-muted);
  font-size: .8125rem;
}

.detail-metadata-row dd {
  min-width: 0;
  margin: 0;
  color: var(--proto-ink);
  font-size: .8125rem;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.detail-space-value {
  color: #6a9419 !important;
}

.detail-uploader {
  display: block;
  margin-top: 24px;
  padding: 24px 0 0;
  border-top: 1px solid var(--proto-line);
  border-bottom: 0;
}

.detail-uploader-row {
  display: flex;
  align-items: center;
  gap: 12px;
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

.detail-uploader-copy strong {
  display: block;
  margin-top: 0;
  color: var(--proto-ink);
  font-size: .9375rem;
  overflow-wrap: anywhere;
}

.detail-review-note {
  margin-top: 20px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 137, 106, .45);
  border-radius: 9px;
  background: rgba(255, 137, 106, .12);
  color: var(--proto-ink);
  font-size: .8125rem;
  line-height: 1.5;
}

.detail-review-note p {
  margin: 5px 0 0;
  color: var(--proto-muted);
  display: block;
  overflow: visible;
}

.detail-action-area {
  margin-top: auto;
  padding-top: 24px;
}

.detail-manage-button.ant-btn {
  height: 50px;
  border: 0;
  border-radius: 10px;
  background: var(--proto-acid);
  color: var(--proto-ink);
  box-shadow: none;
  font-size: .875rem;
  font-weight: 800;
}

.detail-manage-button.ant-btn:hover,
.detail-manage-button.ant-btn:focus {
  background: #c7ff62;
  color: var(--proto-ink);
}

.detail-action-hint {
  margin: 10px 0 0;
  color: var(--proto-muted);
  font-size: .6875rem;
  line-height: 1.45;
  text-align: center;
}

.detail-prototype :deep(.ant-dropdown-menu) {
  min-width: 160px;
}

@media (max-width: 1180px) {
  .detail-layout,
  .detail-loading-shell {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 24px;
  }

  .detail-info-card {
    padding-inline: 24px;
  }
}

@media (max-width: 820px) {
  .detail-prototype {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .detail-head {
    min-height: 64px;
    padding-top: 10px;
  }

  .detail-layout {
    grid-template-columns: 1fr;
    flex: none;
    min-height: auto;
    padding-top: 14px;
  }

  .detail-loading-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .detail-loading-image {
    min-height: 360px;
  }

  .detail-loading-info {
    padding: 0;
  }

  .detail-image-stage {
    height: min(72vw, 560px);
    min-height: 320px;
  }

  .detail-info-card {
    position: static;
    min-height: 0;
    max-height: none;
    overflow: visible;
    padding: 24px 20px 20px;
  }
}

@media (max-width: 520px) {
  .detail-image-stage {
    height: min(82vw, 480px);
    min-height: 280px;
  }

  .detail-visual {
    padding: 8px 8px 14px;
  }

  .detail-image-caption {
    padding-inline: 2px;
    font-size: .75rem;
  }

  .detail-info-card {
    padding-inline: 16px;
  }

  .detail-image-title {
    font-size: 1.7rem;
  }

  .detail-metadata-row {
    grid-template-columns: 86px minmax(0, 1fr);
    gap: 10px;
  }
}
</style>
