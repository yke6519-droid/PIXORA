<template>
  <div class="review-prototype">
    <section class="review-heading">
      <div class="review-heading-copy">
        <h1 class="review-title">图片审核</h1>
      </div>
      <div class="review-counter" aria-label="待审核图片数量">
        <span>待处理图片</span>
        <strong>{{ pendingTotal }}</strong>
      </div>
    </section>

    <a-spin v-if="authChecking" class="review-auth-loading" tip="正在确认管理员权限..." />

    <a-result
      v-else-if="!authorized"
      status="403"
      title="暂时无法进入审核台"
      :sub-title="accessError || '只有管理员可以审核图片。'"
    >
      <template #extra>
        <a-button class="proto-button ghost-button" @click="ensureAdmin">重新检查权限</a-button>
      </template>
    </a-result>

    <template v-else>
      <a-alert
        v-if="loadError"
        class="review-alert"
        type="error"
        show-icon
        :message="loadError"
        description="请确认后端服务、Redis 和当前登录会话均正常后重试。"
        closable
        @close="loadError = ''"
      />

      <section class="review-toolbar proto-section">
        <div class="review-tabs" role="tablist" aria-label="图片审核状态">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.value"
            :class="{ active: activeTab === tab.value }"
            @click="changeTab(tab.value)"
          >
            {{ tab.label }}
            <b>{{ tabCount(tab.value) }}</b>
          </button>
        </div>

        <div class="review-batch proto-surface">
          <div class="proto-flex proto-gap-12">
            <a-checkbox :checked="allSelected" :indeterminate="partlySelected" @change="toggleAll" />
            <span>选择当前列表</span>
            <span v-if="selectedIds.length" class="review-selected-count">已选 {{ selectedIds.length }} 张</span>
          </div>
          <div class="review-batch-actions">
            <a-button
              class="proto-button ghost-button"
              :disabled="!selectedIds.length || actionLoading"
              @click="batchPass"
            >
              批量通过
            </a-button>
            <a-button
              class="proto-button ghost-button danger-button"
              :disabled="!selectedIds.length || actionLoading"
              @click="batchRefuse"
            >
              批量拒绝
            </a-button>
          </div>
        </div>
      </section>

      <section v-if="loading" class="review-grid" aria-label="审核列表加载中">
        <article v-for="item in 6" :key="item" class="review-card proto-surface proto-rounded review-skeleton-card">
          <a-skeleton active :paragraph="{ rows: 4 }" />
        </article>
      </section>

      <section v-else-if="pictures.length" class="review-grid">
        <article
          v-for="picture in pictures"
          :key="normalizeId(picture.id)"
          class="review-card proto-surface proto-rounded"
        >
          <div class="review-card-image proto-image-wrap">
            <img
              :src="picture.thumbnailUrl || picture.url"
              :alt="picture.name || '未命名图片'"
              loading="lazy"
            />
            <div class="review-image-check">
              <a-checkbox
                :checked="selectedIds.includes(normalizeId(picture.id))"
                @change="(event: any) => toggleOne(picture.id, event.target.checked)"
              />
            </div>
            <span class="review-image-format">{{ picture.picformat || 'IMAGE' }}</span>
          </div>

          <div class="review-card-body">
            <div class="review-card-title">
              <strong :title="picture.name || '未命名图片'">{{ picture.name || '未命名图片' }}</strong>
              <a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">
                {{ pictureStatusText(picture.pictureCheck) }}
              </a-tag>
            </div>
            <p class="review-introduction">{{ picture.introduction || '上传者未填写图片简介。' }}</p>
            <div class="review-meta">
              <span class="review-uploader" :title="uploaderLabel(picture)">
                上传者 {{ uploaderLabel(picture) }}
              </span>
              <span>{{ formatDate(picture.createtime) }}</span>
            </div>
            <div class="review-meta review-meta-secondary">
              <span>{{ formatSize(picture.picsize) }} · {{ picture.picwidth || '—' }}×{{ picture.picheight || '—' }}</span>
              <span>{{ picture.category || '未分类' }}</span>
            </div>
            <div class="review-card-actions">
              <a-button class="proto-button ghost-button" @click="openReview(picture)">审核</a-button>
              <a-button class="proto-button ghost-button" @click="openDetail(picture.id)">详情</a-button>
              <a-button
                v-if="picture.pictureCheck === 0"
                class="proto-button acid-button"
                type="primary"
                :loading="actionLoading"
                @click="passOne(picture)"
              >
                通过
              </a-button>
            </div>
            <div v-if="picture.pictureCheck === 2 && picture.checkMessage" class="review-message">
              <span>拒绝原因</span>{{ picture.checkMessage }}
            </div>
          </div>
        </article>
      </section>

      <a-empty v-else description="当前状态没有图片">
        <template #image>
          <div class="review-empty-mark">0</div>
        </template>
        <template #footer>
          <a-button class="proto-button ghost-button" @click="loadPage">重新加载</a-button>
        </template>
      </a-empty>

      <div v-if="!loading && total > 0" class="review-pagination">
        <span class="proto-mono">当前页 {{ pictures.length }} 张 / 共 {{ total }} 张</span>
        <a-pagination
          v-model:current="current"
          :page-size="pageSize"
          :total="total"
          :show-size-changer="false"
          :show-quick-jumper="total > pageSize * 3"
          @change="handlePageChange"
        />
      </div>
    </template>

    <a-modal
      v-model:open="reviewOpen"
      :title="reviewingBatch ? `批量审核 ${batchTargetIds.length} 张图片` : `审核图片 #${normalizeId(reviewPicture?.id)}`"
      ok-text="提交审核"
      cancel-text="取消"
      :confirm-loading="actionLoading"
      @ok="submitReview"
    >
      <div v-if="reviewPicture" class="review-modal-preview">
        <img :src="reviewPicture.url" :alt="reviewPicture.name || '图片预览'" />
        <div>
          <strong>{{ reviewPicture.name || '未命名图片' }}</strong>
          <p>{{ reviewPicture.introduction || '上传者未填写图片简介。' }}</p>
          <span class="proto-mono">
            {{ reviewPicture.category || '未分类' }} / {{ parseTags(reviewPicture.tags).join(' · ') || '无标签' }}
          </span>
        </div>
      </div>
      <div v-else class="review-batch-note">
        本次操作将更新当前选中的 {{ batchTargetIds.length }} 张图片。批量拒绝时，所有图片共用同一条拒绝原因。
      </div>

      <a-form layout="vertical" class="proto-form">
        <a-form-item label="审核结果">
          <a-radio-group v-model:value="reviewResult">
            <a-radio :value="1">审核通过</a-radio>
            <a-radio :value="2">审核拒绝</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="reviewResult === 2" label="拒绝原因" required>
          <a-textarea
            v-model:value="checkMessage"
            :rows="4"
            :maxlength="200"
            show-count
            placeholder="请输入审核拒绝原因，后端会将它保存到 checkMessage"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  adminCheckPictureBatch,
  adminCheckPicture,
  queryAll,
} from '../../../api/pictureController'
import { getCurrentUser } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { pictureStatusText } from '../prototypeData'

type ReviewTab = 'all' | 'pending' | 'pass' | 'refuse'

const tabs: Array<{ label: string; value: ReviewTab }> = [
  { label: '全部图片', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '审核通过', value: 'pass' },
  { label: '审核拒绝', value: 'refuse' },
]
const statusByTab: Record<ReviewTab, number | undefined> = {
  all: undefined,
  pending: 0,
  pass: 1,
  refuse: 2,
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const authChecking = ref(true)
const authorized = ref(false)
const accessError = ref('')
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const pictures = ref<API.PictureVO[]>([])
const selectedIds = ref<string[]>([])
const activeTab = ref<ReviewTab>('pending')
const current = ref(1)
const pageSize = 6
const total = ref(0)
const statusCounts = reactive<Record<ReviewTab, number>>({ all: 0, pending: 0, pass: 0, refuse: 0 })

const reviewOpen = ref(false)
const reviewingBatch = ref(false)
const reviewPicture = ref<API.PictureVO | null>(null)
const batchTargetIds = ref<string[]>([])
const reviewResult = ref<1 | 2>(1)
const checkMessage = ref('')

const pendingTotal = computed(() => statusCounts.pending)
const allSelected = computed(() =>
  pictures.value.length > 0 && pictures.value.every((picture) => selectedIds.value.includes(normalizeId(picture.id))),
)
const partlySelected = computed(() => selectedIds.value.length > 0 && !allSelected.value)

function normalizeId(id?: number | string) {
  return id == null ? '' : String(id)
}

function numberValue(value?: number | string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function statusClass(status?: number) {
  return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait'
}

function formatDate(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '未记录时间'
}

function formatSize(value?: number | string) {
  const bytes = numberValue(value)
  if (!bytes) return '大小未知'
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

function parseTags(value?: string | string[]) {
  if (!value) return []
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.map(String)
  } catch {
    // 兼容历史数据：标签字段不是合法 JSON 时，仍允许按逗号展示。
  }
  return value.split(/[,，]/).map((item) => item.trim()).filter(Boolean)
}

/** 优先展示后端脱敏后的用户名和账号，只有旧数据缺字段时才回退到用户 ID。 */
function uploaderLabel(picture: API.PictureVO) {
  const username = picture.createdUser?.username?.trim()
  const useraccount = picture.createdUser?.useraccount?.trim()
  if (username && useraccount) return `${username} · ${useraccount}`
  return username || useraccount || (picture.userId != null ? `用户 ${picture.userId}` : '未知用户')
}

function tabCount(tab: ReviewTab) {
  return statusCounts[tab]
}

function queryBody(status?: number, size = pageSize): API.PictureQueryRequest {
  return {
    current: 1,
    pageSize: size,
    ...(status == null ? {} : { pictureCheck: status }),
    sortFiled: 'createtime',
    sortOrder: 'descend',
  }
}

/**
 * 审核台只允许管理员进入；当前用户信息仍然通过后端 Session 获取，避免只相信前端缓存。
 */
async function ensureAdmin() {
  authChecking.value = true
  accessError.value = ''
  try {
    const res = await getCurrentUser()
    if (res.data?.code === 40100 || !res.data?.data) {
      loginUserStore.clearLoginUser()
      await router.replace({ path: '/user/login', query: { redirect: route.fullPath } })
      return false
    }
    if (res.data.code !== 200) {
      accessError.value = res.data.message || '当前用户信息加载失败'
      authorized.value = false
      return false
    }
    loginUserStore.setLoginUser(res.data.data)
    if (res.data.data.userLevel !== 'admin') {
      accessError.value = '当前账号不是管理员，无法调用管理员审核接口。'
      authorized.value = false
      return false
    }
    authorized.value = true
    await loadPage()
    await loadCounts()
    return true
  } catch (error: any) {
    authorized.value = false
    accessError.value = error?.response?.data?.message || error?.message || '管理员权限检查失败'
    return false
  } finally {
    authChecking.value = false
  }
}

async function loadPage() {
  if (!authorized.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await queryAll({
      ...queryBody(statusByTab[activeTab.value], pageSize),
      current: current.value,
    })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '审核图片列表加载失败')
    pictures.value = res.data.data?.records || []
    total.value = numberValue(res.data.data?.total)
    selectedIds.value = []
  } catch (error: any) {
    pictures.value = []
    total.value = 0
    loadError.value = error?.response?.data?.message || error?.message || '审核图片列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  if (!authorized.value) return
  try {
    const results = await Promise.all([
      queryAll(queryBody(0, 1)),
      queryAll(queryBody(1, 1)),
      queryAll(queryBody(2, 1)),
    ])
    statusCounts.pending = results[0].data?.code === 200 ? numberValue(results[0].data.data?.total) : 0
    statusCounts.pass = results[1].data?.code === 200 ? numberValue(results[1].data.data?.total) : 0
    statusCounts.refuse = results[2].data?.code === 200 ? numberValue(results[2].data.data?.total) : 0
    statusCounts.all = statusCounts.pending + statusCounts.pass + statusCounts.refuse
  } catch {
    // 列表本身仍可用，统计失败时不阻塞审核；下一次刷新会重新获取统计。
    message.warning('审核数量统计暂时加载失败')
  }
}

async function changeTab(tab: ReviewTab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  current.value = 1
  selectedIds.value = []
  await loadPage()
}

async function handlePageChange(page: number) {
  current.value = page
  await loadPage()
}

function toggleOne(id: number | string | undefined, checked: boolean) {
  const normalizedId = normalizeId(id)
  if (!normalizedId) return
  selectedIds.value = checked
    ? [...new Set([...selectedIds.value, normalizedId])]
    : selectedIds.value.filter((item) => item !== normalizedId)
}

function toggleAll(event: any) {
  selectedIds.value = event.target.checked
    ? pictures.value.map((picture) => normalizeId(picture.id)).filter(Boolean)
    : []
}

function openReview(picture: API.PictureVO) {
  reviewPicture.value = picture
  reviewingBatch.value = false
  batchTargetIds.value = []
  reviewResult.value = picture.pictureCheck === 2 ? 2 : 1
  checkMessage.value = picture.checkMessage || ''
  reviewOpen.value = true
}

function batchRefuse() {
  if (!selectedIds.value.length) return
  reviewPicture.value = null
  reviewingBatch.value = true
  batchTargetIds.value = [...selectedIds.value]
  reviewResult.value = 2
  checkMessage.value = ''
  reviewOpen.value = true
}

function passOne(picture: API.PictureVO) {
  const id = normalizeId(picture.id)
  if (!id) return
  Modal.confirm({
    title: '确认通过这张图片？',
    content: `通过后图片 #${id} 会从待审核列表中移出。`,
    okText: '确认通过',
    cancelText: '取消',
    onOk: () => submitSingle(id, 1),
  })
}

function batchPass() {
  if (!selectedIds.value.length) return
  Modal.confirm({
    title: `确认批量通过 ${selectedIds.value.length} 张图片？`,
    content: '批量通过会直接更新这些图片的审核状态。',
    okText: '确认通过',
    cancelText: '取消',
    onOk: () => submitBatch([...selectedIds.value], 1),
  })
}

async function submitSingle(id: string, result: 1 | 2, reason?: string) {
  actionLoading.value = true
  try {
    const res = await adminCheckPicture({
      picId: id,
      checkResult: result,
      ...(result === 2 ? { checkMessage: reason?.trim() } : {}),
    })
    if (res.data?.code !== 200 || res.data.data === false) {
      throw new Error(res.data?.message || '单张审核提交失败')
    }
    message.success(result === 1 ? '图片已审核通过' : '图片已拒绝')
    await Promise.all([loadPage(), loadCounts()])
    return true
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '单张审核提交失败')
    return false
  } finally {
    actionLoading.value = false
  }
}

async function submitBatch(ids: string[], result: 1 | 2, reason?: string) {
  if (!ids.length) return false
  actionLoading.value = true
  try {
    const res = await adminCheckPictureBatch({
      picIds: ids,
      checkResult: result,
      ...(result === 2 ? { checkMessage: reason?.trim() } : {}),
    })
    if (res.data?.code !== 200 || res.data.data === false) {
      throw new Error(res.data?.message || '批量审核提交失败')
    }
    message.success(result === 1 ? `已通过 ${ids.length} 张图片` : `已拒绝 ${ids.length} 张图片`)
    await Promise.all([loadPage(), loadCounts()])
    return true
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '批量审核提交失败')
    return false
  } finally {
    actionLoading.value = false
  }
}

async function submitReview() {
  if (reviewResult.value === 2 && !checkMessage.value.trim()) {
    message.warning('请填写审核拒绝原因')
    return
  }

  const success = reviewingBatch.value
    ? await submitBatch(batchTargetIds.value, reviewResult.value, checkMessage.value)
    : reviewPicture.value?.id != null
      ? await submitSingle(normalizeId(reviewPicture.value.id), reviewResult.value, checkMessage.value)
      : false

  if (success) {
    reviewOpen.value = false
    reviewPicture.value = null
    batchTargetIds.value = []
    checkMessage.value = ''
  }
}

async function openDetail(id?: number | string) {
  const normalizedId = normalizeId(id)
  if (!normalizedId) return
  await router.push(`/gallery/detail/${encodeURIComponent(normalizedId)}`)
}

onMounted(() => {
  void ensureAdmin()
})
</script>

<style scoped>
.review-prototype { color: var(--proto-ink); font-family: 'Geist', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.review-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 14px; padding: 14px 0 15px; border-bottom: 1px solid var(--proto-line); }
.review-heading-copy { display: flex; align-items: baseline; gap: 14px; min-width: 0; }
.review-title { margin: 0; color: var(--proto-ink); font-size: clamp(32px, 3.5vw, 42px); font-weight: 800; letter-spacing: -.055em; line-height: 1; text-wrap: balance; }
.review-counter { min-width: 138px; padding: 11px 13px; background: var(--proto-ink); color: var(--proto-paper); }
.review-counter span, .review-counter strong { display: block; }
.review-counter span { color: var(--proto-orange); font-size: 11px; font-weight: 700; }
.review-counter strong { margin-top: 7px; color: var(--proto-acid); font-size: 32px; font-weight: 800; line-height: 1; letter-spacing: -.04em; }
.review-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.review-alert { margin-bottom: 14px; }
.review-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.review-toolbar.proto-section { padding-top: 0; }
.review-tabs { display: flex; gap: 4px; flex-wrap: wrap; }
.review-tabs button { min-height: 42px; padding: 0 14px; border: 1px solid var(--proto-line); background: transparent; color: var(--proto-muted); cursor: pointer; font: inherit; font-size: 13px; font-weight: 650; }
.review-tabs button b { margin-left: 7px; color: var(--proto-orange); font-size: 12px; font-weight: 700; }
.review-tabs button:hover { border-color: var(--proto-ink); color: var(--proto-ink); }
.review-tabs button:focus-visible { outline: 2px solid var(--proto-orange); outline-offset: 2px; }
.review-tabs button.active { border-color: var(--proto-ink); background: var(--proto-ink); color: var(--proto-paper); }
.review-batch { display: flex; align-items: center; gap: 12px; padding: 7px 9px; }
.review-batch-actions { display: flex; gap: 6px; }
.review-selected-count { color: var(--proto-orange); font-size: 12px; font-weight: 700; }
.danger-button:not(:disabled) { color: var(--proto-orange); }
.review-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.review-card { overflow: hidden; }
.review-skeleton-card { min-height: 370px; padding: 15px; }
.review-card-image { position: relative; height: 205px; }
.review-card-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.review-image-check { position: absolute; top: 11px; left: 11px; padding: 4px; background: rgba(241,242,237,.9); }
.review-image-format { position: absolute; right: 11px; bottom: 10px; padding: 4px 6px; background: rgba(11,15,17,.78); color: var(--proto-acid); font-size: 10px; font-weight: 700; }
.review-card-body { padding: 14px; }
.review-card-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.review-card-title strong { overflow: hidden; font-size: 16px; font-weight: 800; letter-spacing: -.035em; text-overflow: ellipsis; white-space: nowrap; }
.review-introduction { display: -webkit-box; min-height: 36px; margin: 8px 0 11px; overflow: hidden; color: var(--proto-muted); font-size: 12px; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.review-meta { display: flex; justify-content: space-between; gap: 8px; color: var(--proto-muted); font-size: 11px; line-height: 1.4; }
.review-meta span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.review-uploader { min-width: 0; }
.review-meta-secondary { margin-top: 5px; color: color-mix(in srgb, var(--proto-muted) 84%, transparent); font-size: 11px; }
.review-card-actions { display: flex; gap: 6px; margin-top: 14px; }
.review-card-actions .proto-button { flex: 1; min-width: 0; padding-inline: 6px; font-size: 12px; }
.review-message { margin-top: 12px; padding: 8px 10px; border: 1px solid rgba(255,137,106,.35); border-radius: 4px; background: rgba(255,137,106,.12); color: var(--proto-muted); font-size: 11px; line-height: 1.5; }
.review-message span { margin-right: 5px; color: var(--proto-orange); font-weight: 700; }
.review-empty-mark { display: grid; width: 58px; height: 58px; place-items: center; border: 1px solid var(--proto-line); color: var(--proto-orange); font-size: 20px; font-weight: 700; }
.review-pagination { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 18px; }
.review-pagination .ant-pagination { margin: 0; }
.review-modal-preview { display: grid; grid-template-columns: 135px 1fr; gap: 13px; margin-bottom: 22px; }
.review-modal-preview img { width: 135px; height: 105px; object-fit: cover; }
.review-modal-preview strong { font-size: 15px; font-weight: 800; }
.review-modal-preview p { margin: 8px 0; color: var(--proto-muted); font-size: 12px; line-height: 1.5; }
.review-modal-preview span { color: var(--proto-orange); font-size: 11px; }
.review-batch-note { margin-bottom: 20px; padding: 12px 14px; border: 1px solid rgba(186,255,61,.42); border-radius: 4px; background: rgba(186,255,61,.1); color: var(--proto-muted); font-size: 12px; line-height: 1.6; }
@media (max-width: 1100px) { .review-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .review-toolbar { align-items: flex-start; flex-direction: column; } }
@media (max-width: 650px) { .review-heading { align-items: flex-start; flex-direction: column; } .review-grid { grid-template-columns: 1fr; } .review-tabs button { padding-inline: 10px; } .review-batch, .review-pagination { align-items: flex-start; flex-direction: column; } .review-card-actions { flex-wrap: wrap; } .review-card-actions .proto-button { flex-basis: calc(50% - 4px); } }
</style>
