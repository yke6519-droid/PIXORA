<template>
  <div class="detail-prototype">
    <section class="proto-page-head detail-head">
      <div>
        <span class="proto-eyebrow">图片详情 / getPictureById</span>
        <h1 class="proto-title">{{ picture?.name || '图片详情' }}</h1>
        <p class="proto-copy">详情页直接展示后端 PictureVO，包含图片地址、尺寸、分类、标签、审核状态和上传者信息。</p>
      </div>
      <div class="proto-page-head-actions">
        <a-button class="proto-button ghost-button" @click="router.push('/prototype/gallery')">返回图库</a-button>
        <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery/manage')">管理我的图片</a-button>
      </div>
    </section>

    <div v-if="loading" class="detail-state"><a-spin size="large" tip="加载图片详情中..." /></div>

    <a-result v-else-if="errorMessage" class="detail-state" status="warning" title="图片详情暂时无法打开" :sub-title="errorMessage">
      <template #extra>
        <a-button class="proto-button acid-button" type="primary" @click="fetchPictureDetail">重新加载</a-button>
        <a-button class="proto-button ghost-button" @click="router.push('/prototype/gallery')">返回图库</a-button>
      </template>
    </a-result>

    <section v-else-if="picture" class="detail-layout proto-section">
      <div class="detail-visual proto-surface proto-rounded">
        <div class="detail-image proto-image-wrap">
          <img v-if="picture.url || picture.thumbnailUrl" :src="picture.url || picture.thumbnailUrl" :alt="picture.name || '图片详情'" />
          <div v-else class="detail-image-empty">暂无图片地址</div>
        </div>
        <div class="detail-image-caption">
          <span>{{ picture.picwidth || '-' }} × {{ picture.picheight || '-' }} px</span>
          <span>{{ picture.picformat?.toUpperCase() || '-' }} / {{ formatPictureSize(picture.picsize) }}</span>
        </div>
      </div>

      <div class="detail-info">
        <div class="detail-status-line">
          <a-tag class="proto-status" :class="statusClass">{{ pictureStatusText(picture.pictureCheck) }}</a-tag>
          <span class="proto-mono">ID {{ picture.id }}</span>
        </div>
        <h2>{{ picture.introduction || '暂无图片简介' }}</h2>
        <div v-if="picture.tags?.length" class="detail-tags">
          <a-tag v-for="tag in picture.tags" :key="tag" class="proto-tag acid-tag">{{ tag }}</a-tag>
        </div>

        <div class="detail-meta-grid">
          <div><span>分类</span><strong>{{ picture.category || '未分类' }}</strong></div>
          <div><span>空间</span><strong>{{ picture.spaceId === 0 ? '公共图库' : picture.spaceId ? `空间 #${picture.spaceId}` : '未指定' }}</strong></div>
          <div><span>比例</span><strong>{{ formatRatio(picture.picwidth, picture.picheight) }}</strong></div>
          <div><span>上传时间</span><strong>{{ picture.createtime || '-' }}</strong></div>
        </div>

        <div v-if="picture.createdUser" class="detail-uploader">
          <div class="detail-uploader-avatar">
            <img v-if="picture.createdUser.avatarurl" :src="picture.createdUser.avatarurl" :alt="picture.createdUser.username || '上传者'" />
            <span v-else>{{ picture.createdUser.username?.charAt(0) || '?' }}</span>
          </div>
          <span><small>上传者</small><strong>{{ picture.createdUser.username || '未知用户' }}</strong></span>
        </div>

        <div v-if="picture.pictureCheck === 2 && picture.checkMessage" class="detail-alert">
          <strong>审核备注</strong>
          <span>{{ picture.checkMessage }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPictureByIdUsingGet } from '../../../api/pictureController'
import { pictureStatusText } from '../prototypeData'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const errorMessage = ref('')
const picture = ref<API.PictureVO | null>(null)

const statusClass = computed(() => {
  if (picture.value?.pictureCheck === 1) return 'pass'
  if (picture.value?.pictureCheck === 2) return 'refuse'
  return 'wait'
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
  try {
    const res = await getPictureByIdUsingGet({ id })
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

function formatPictureSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatRatio(width?: number, height?: number) {
  if (!width || !height) return '-'
  return (width / height).toFixed(2)
}

onMounted(() => {
  void fetchPictureDetail()
})

watch(() => route.params.id, () => {
  void fetchPictureDetail()
})
</script>

<style scoped>
.detail-prototype { height: 100%; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.detail-head { flex: 0 0 auto; align-items: center; padding-top: 8px; }
.detail-head .proto-title { margin: 4px 0 6px; font-size: clamp(30px, 3.4vw, 48px); }
.detail-head .proto-copy { max-width: 520px; font-size: 11px; line-height: 1.45; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.detail-head :deep(.proto-button) { height: 37px; padding-inline: 13px; font-size: 12px; }
.detail-state { flex: 1 1 auto; min-height: 0; display: grid; place-items: center; }
.detail-layout { flex: 1 1 auto; min-height: 0; display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(320px, .8fr); gap: clamp(18px, 3.5vw, 48px); align-items: stretch; padding-top: 14px; overflow: hidden; }
.detail-visual { min-height: 0; height: 100%; padding: 8px; display: flex; flex-direction: column; }
.detail-image { min-height: 0; flex: 1 1 auto; height: auto; }
.detail-image :deep(img) { display: block; width: 100%; height: 100%; object-fit: contain; background: var(--proto-paper-deep); }
.detail-image-empty { height: 100%; display: grid; place-items: center; color: var(--proto-muted); font-size: 12px; }
.detail-image-caption { display: flex; justify-content: space-between; padding: 8px 3px 0; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.detail-info { min-height: 0; padding-top: 0; overflow: hidden; }
.detail-status-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; color: var(--proto-muted); }
.detail-info h2 { margin: 0; font-size: clamp(20px, 2.5vw, 33px); line-height: 1.08; letter-spacing: -.06em; }
.detail-tags { margin-top: 12px; }
.detail-meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; margin: 22px 0 15px; background: var(--proto-line); border: 1px solid var(--proto-line); }
.detail-meta-grid div { min-height: 52px; padding: 9px; display: flex; flex-direction: column; justify-content: space-between; background: rgba(255,255,255,.4); }
.detail-meta-grid span { color: var(--proto-muted); font-size: 10px; }
.detail-meta-grid strong { font-size: 13px; }
.detail-uploader { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid var(--proto-line); border-bottom: 1px solid var(--proto-line); }
.detail-uploader-avatar { width: 38px; height: 38px; display: grid; place-items: center; overflow: hidden; border-radius: 50%; background: var(--proto-acid); color: var(--proto-ink); font-weight: 800; }
.detail-uploader-avatar img { width: 100%; height: 100%; object-fit: cover; }
.detail-uploader small, .detail-uploader strong { display: block; }
.detail-uploader small { color: var(--proto-muted); font-size: 10px; }
.detail-uploader strong { margin-top: 4px; font-size: 13px; }
.detail-alert { margin-top: 14px; padding: 10px 12px; display: flex; flex-direction: column; gap: 5px; border-left: 3px solid var(--proto-orange); background: rgba(255,137,106,.14); font-size: 11px; line-height: 1.45; }
.detail-alert strong { font-size: 11px; }
@media (max-width: 800px) { .detail-prototype { height: auto; overflow: visible; } .detail-state { min-height: 360px; } .detail-layout { display: grid; grid-template-columns: 1fr; flex: none; min-height: auto; overflow: visible; } .detail-visual { height: auto; } .detail-image { height: min(66vh, 520px); flex: none; } .detail-info { overflow: visible; } }
</style>
