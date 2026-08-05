<template>
  <div class="manage-prototype">
    <section class="manage-page-head">
      <div class="manage-heading">
        <div class="manage-heading-line">
          <h1>我的图片</h1>
          <span class="manage-count"><b>{{ total }}</b> 张</span>
        </div>
        <p class="manage-context" aria-label="当前图片范围和审核状态">
          <span>{{ scopeText }}</span>
          <span class="manage-context-separator">·</span>
          <span>{{ statusText(filters.pictureCheck) }}</span>
        </p>
      </div>
      <div class="manage-page-actions">
        <a-button
          class="proto-button acid-button"
          type="primary"
          @click="router.push('/prototype/gallery/upload')"
        >
          上传图片
        </a-button>
      </div>
    </section>

    <section class="manage-toolbar proto-surface proto-rounded proto-section">
      <div class="manage-toolbar-line">
        <a-input-search
          v-model:value="filters.searchText"
          placeholder="按名称或简介搜索"
          allow-clear
          enter-button="搜索"
          :loading="loading"
          @search="runSearch"
        />
        <a-select
          v-model:value="filters.pictureCheck"
          aria-label="审核状态"
          style="width: 142px"
          @change="runSearch"
        >
          <a-select-option :value="0">待审核</a-select-option>
          <a-select-option :value="1">审核通过</a-select-option>
          <a-select-option :value="2">审核拒绝</a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.scope"
          aria-label="图库范围"
          style="width: 158px"
          @change="handleScopeChange"
        >
          <a-select-option value="public">公共图库</a-select-option>
          <a-select-option value="private" :disabled="!hasPrivateSpace">
            我的私人空间
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.category"
          :options="categoryOptions"
          :loading="optionLoading"
          allow-clear
          show-search
          placeholder="全部分类"
          style="width: 142px"
          @change="runSearch"
        />
        <a-select
          v-model:value="filters.tags"
          mode="multiple"
          :options="tagOptions"
          :loading="optionLoading"
          :max-tag-count="1"
          allow-clear
          placeholder="筛选标签"
          style="min-width: 170px"
          @change="runSearch"
        />
        <div class="manage-filter-actions">
          <a-button class="proto-button ghost-button" @click="resetFilters">
            重置
          </a-button>
          <a-button
            class="proto-button ghost-button"
            danger
            :disabled="!selectedIds.length"
            @click="confirmBatchDelete"
          >
            删除已选 {{ selectedIds.length ? `(${selectedIds.length})` : '' }}
          </a-button>
        </div>
      </div>
    </section>

    <a-alert
      v-if="loadError"
      class="manage-alert"
      type="error"
      show-icon
      :message="loadError"
    >
      <template #action>
        <a-button size="small" @click="loadPictures">重新加载</a-button>
      </template>
    </a-alert>

    <section class="manage-table proto-section">
      <div class="manage-table-head">
        <div class="proto-flex proto-gap-12">
          <a-checkbox
            :checked="allSelected"
            :indeterminate="partlySelected"
            :disabled="!pictures.length"
            @change="toggleAll"
          />
          <span>选择当前页</span>
        </div>
      </div>

      <div v-if="loading" class="manage-loading proto-surface proto-rounded">
        <a-skeleton active :paragraph="{ rows: 6 }" />
      </div>

      <a-empty
        v-else-if="!pictures.length && !loadError"
        class="manage-empty proto-surface proto-rounded"
        :description="emptyDescription"
      >
        <a-button
          class="proto-button acid-button"
          type="primary"
          @click="router.push('/prototype/gallery/upload')"
        >
          上传第一张图片
        </a-button>
      </a-empty>

      <div v-else class="manage-list">
        <article v-for="picture in pictures" :key="String(picture.id)" class="manage-row">
          <div class="manage-select">
            <a-checkbox
              :checked="selectedIds.includes(normalizeId(picture.id))"
              @change="(event: any) => toggleOne(picture.id, event.target.checked)"
            />
          </div>

          <button
            type="button"
            class="manage-thumb proto-image-wrap"
            :aria-label="`查看 ${picture.name || '图片'} 详情`"
            @click="openDetail(picture.id)"
          >
            <img
              v-if="picture.thumbnailUrl || picture.url"
              :src="picture.thumbnailUrl || picture.url"
              :alt="picture.name || '图片'"
            />
            <span v-else>暂无预览</span>
          </button>

          <div class="manage-main">
            <div class="manage-title-line">
              <strong>{{ picture.name || '未命名图片' }}</strong>
              <a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">
                {{ statusText(picture.pictureCheck) }}
              </a-tag>
            </div>
            <p>{{ picture.introduction || '暂无图片简介' }}</p>
            <div class="manage-tags">
              <a-tag v-if="picture.category" class="proto-tag acid-tag">
                {{ picture.category }}
              </a-tag>
              <a-tag v-for="tag in picture.tags || []" :key="tag" class="proto-tag">
                {{ tag }}
              </a-tag>
            </div>
            <div v-if="picture.pictureCheck === 2" class="reject-message">
              拒绝原因：{{ picture.checkMessage || '管理员未填写原因' }}
            </div>
          </div>

          <div class="manage-meta" aria-label="图片属性">
            <div class="manage-meta-item">
              <span>图库范围</span>
              <strong>{{ picture.spaceId && String(picture.spaceId) !== '0' ? '私人空间' : '公共图库' }}</strong>
            </div>
            <div class="manage-meta-item">
              <span>尺寸</span>
              <strong>{{ picture.picwidth || '—' }} × {{ picture.picheight || '—' }}</strong>
            </div>
            <div class="manage-meta-item">
              <span>文件大小</span>
              <strong>{{ formatFileSize(picture.picsize) }}</strong>
            </div>
            <div class="manage-meta-item">
              <span>上传时间</span>
              <strong>{{ formatDate(picture.createtime) }}</strong>
            </div>
          </div>

          <div class="manage-action-cell">
            <a-button type="link" @click="openDetail(picture.id)">详情</a-button>
            <a-button type="link" @click="openEditor(picture, 'edit')">编辑</a-button>
            <a-button
              v-if="picture.pictureCheck === 2"
              type="link"
              @click="openEditor(picture, 'reupload')"
            >
              重新上传
            </a-button>
            <a-popconfirm
              title="确定删除这张图片吗？"
              description="删除后无法恢复。"
              ok-text="删除"
              cancel-text="取消"
              @confirm="deleteOne(picture.id)"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </div>
        </article>
      </div>

      <div v-if="total > pageSize" class="manage-pagination">
        <a-pagination
          v-model:current="current"
          :page-size="pageSize"
          :total="total"
          :show-size-changer="false"
          show-less-items
          @change="handlePageChange"
        />
      </div>
    </section>

    <PictureManageEditorModal
      v-model:open="editorOpen"
      :mode="editorMode"
      :picture="activePicture"
      :categories="categories"
      :tags="tags"
      @success="handleEditorSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  deletePictureUsingDelete,
  listPictureCategoryUsingGet,
  queryPicturePageUsingPost,
} from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import PictureManageEditorModal from './components/PictureManageEditorModal.vue'

type PictureStatus = 0 | 1 | 2
type PictureScope = 'public' | 'private'
type EditorMode = 'edit' | 'reupload'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(false)
const optionLoading = ref(false)
const authChecking = ref(true)
const loadError = ref('')
const pictures = ref<API.PictureVO[]>([])
const categories = ref<string[]>([])
const tags = ref<string[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10
const selectedIds = ref<string[]>([])
const editorOpen = ref(false)
const editorMode = ref<EditorMode>('edit')
const activePicture = ref<API.PictureVO | null>(null)

const filters = reactive({
  searchText: '',
  pictureCheck: 1 as PictureStatus,
  scope: 'public' as PictureScope,
  category: undefined as string | undefined,
  tags: [] as string[],
})

const categoryOptions = computed(() =>
  categories.value.map((item) => ({ label: item, value: item })),
)
const tagOptions = computed(() =>
  tags.value.map((item) => ({ label: item, value: item })),
)
const hasPrivateSpace = computed(() => {
  const spaceId = loginUserStore.loginUser?.spaceId
  return Boolean(spaceId && String(spaceId) !== '0')
})
const scopeText = computed(() =>
  filters.scope === 'private' ? '我的私人空间' : '公共图库',
)
const allSelected = computed(() =>
  pictures.value.length > 0 &&
  pictures.value.every((picture) => selectedIds.value.includes(normalizeId(picture.id))),
)
const partlySelected = computed(() =>
  selectedIds.value.length > 0 && !allSelected.value,
)
const emptyDescription = computed(() =>
  `${scopeText.value}中暂无${statusText(filters.pictureCheck)}的图片`,
)

/**
 * 查询接口依赖后端 Session；未登录时保留当前地址并跳回原型登录页。
 */
async function ensureCurrentUser() {
  authChecking.value = true
  try {
    if (!loginUserStore.loginUser) {
      await loginUserStore.fetchLoginUser()
    }
    if (!loginUserStore.loginUser) {
      await router.replace({
        path: '/prototype/user/login',
        query: { redirect: route.fullPath },
      })
      return false
    }
    return true
  } finally {
    authChecking.value = false
  }
}

/**
 * 普通用户分页上限为 10。审核状态必须显式传递，否则后端会默认只查已通过。
 */
async function loadPictures() {
  const currentUser = loginUserStore.loginUser
  if (!currentUser?.id) return

  if (filters.scope === 'private' && !hasPrivateSpace.value) {
    filters.scope = 'public'
  }

  loading.value = true
  loadError.value = ''
  selectedIds.value = []
  try {
    const res = await queryPicturePageUsingPost({
      current: current.value,
      pageSize,
      userId: currentUser.id,
      spaceId:
        filters.scope === 'private'
          ? currentUser.spaceId
          : 0,
      searchText: filters.searchText.trim() || undefined,
      category: filters.category,
      tags: filters.tags.length ? [...filters.tags] : undefined,
      pictureCheck: filters.pictureCheck,
      sortFiled: 'createtime',
      sortOrder: 'descend',
    })

    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '图片列表加载失败')
    }
    pictures.value = res.data.data?.pictureList || []
    total.value = Number(res.data.data?.total || 0)
  } catch (error: any) {
    pictures.value = []
    total.value = 0
    loadError.value =
      error?.response?.data?.message ||
      error?.message ||
      '图片列表加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  optionLoading.value = true
  try {
    const res = await listPictureCategoryUsingGet()
    if (res.data?.code === 200) {
      categories.value = res.data.data?.categorys || []
      tags.value = res.data.data?.tags || []
    }
  } catch {
    message.warning('分类和标签暂时加载失败，仍可按关键词查询')
  } finally {
    optionLoading.value = false
  }
}

function normalizeId(id?: number | string) {
  return String(id || '').trim()
}

function statusText(status?: number) {
  if (status === 0) return '待审核'
  if (status === 2) return '审核拒绝'
  return '审核通过'
}

function statusClass(status?: number) {
  return status === 0 ? 'wait' : status === 2 ? 'refuse' : 'pass'
}

function runSearch() {
  current.value = 1
  void loadPictures()
}

function handleScopeChange() {
  current.value = 1
  void loadPictures()
}

function resetFilters() {
  filters.searchText = ''
  filters.pictureCheck = 1
  filters.scope = 'public'
  filters.category = undefined
  filters.tags = []
  current.value = 1
  void loadPictures()
}

function toggleOne(id: number | string | undefined, checked: boolean) {
  const value = normalizeId(id)
  if (!value) return
  selectedIds.value = checked
    ? [...new Set([...selectedIds.value, value])]
    : selectedIds.value.filter((item) => item !== value)
}

function toggleAll(event: any) {
  selectedIds.value = event.target.checked
    ? pictures.value.map((picture) => normalizeId(picture.id)).filter(Boolean)
    : []
}

function openDetail(id?: number | string) {
  const value = normalizeId(id)
  if (value) {
    void router.push(`/prototype/gallery/detail/${encodeURIComponent(value)}`)
  }
}

function openEditor(picture: API.PictureVO, mode: EditorMode) {
  activePicture.value = picture
  editorMode.value = mode
  editorOpen.value = true
}

async function handleEditorSuccess() {
  await loadPictures()
}

async function deleteOne(id?: number | string) {
  const value = normalizeId(id)
  if (!value) return

  try {
    const res = await deletePictureUsingDelete({ id: value })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '删除失败')
    }
    message.success('图片已删除')
    if (pictures.value.length === 1 && current.value > 1) {
      current.value -= 1
    }
    await loadPictures()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

function confirmBatchDelete() {
  if (!selectedIds.value.length) return
  Modal.confirm({
    title: `删除选中的 ${selectedIds.value.length} 张图片？`,
    content: '后端目前没有批量删除接口，页面会逐张提交删除，已删除内容无法恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      let successCount = 0
      let failedCount = 0
      for (const id of [...selectedIds.value]) {
        try {
          const res = await deletePictureUsingDelete({ id })
          if (res.data?.code === 200) successCount += 1
          else failedCount += 1
        } catch {
          failedCount += 1
        }
      }

      if (successCount) message.success(`成功删除 ${successCount} 张图片`)
      if (failedCount) message.error(`${failedCount} 张图片删除失败`)
      selectedIds.value = []
      await loadPictures()
    },
  })
}

function formatFileSize(size?: number) {
  if (!size) return '大小未知'
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function formatDate(value?: string) {
  if (!value) return '时间未知'
  return value.replace('T', ' ').replace(/\.\d+.*$/, '')
}

function handlePageChange(page: number) {
  current.value = page
  void loadPictures()
}

onMounted(async () => {
  const authenticated = await ensureCurrentUser()
  if (!authenticated) return
  await Promise.all([loadOptions(), loadPictures()])
})
</script>

<style scoped>
.manage-page-head {
  padding-top: clamp(14px, 2vw, 22px);
  padding-bottom: 15px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22px;
  border-bottom: 1px solid var(--proto-line);
}

.manage-heading-line {
  display: flex;
  align-items: baseline;
  gap: 13px;
  flex-wrap: wrap;
}

.manage-heading h1 {
  margin: 0;
  color: var(--proto-ink);
  font-size: clamp(30px, 3.6vw, 44px);
  line-height: 1;
  letter-spacing: -.06em;
  font-weight: 800;
}

.manage-context {
  width: fit-content;
  margin: 10px 0 0;
  padding: 4px 9px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #8dbb22;
  border-radius: 5px;
  background: rgba(186, 255, 61, .14);
  color: var(--proto-ink);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.25;
}

.manage-context-separator { color: var(--proto-muted); }

.manage-count {
  color: var(--proto-muted);
  font-size: 13px;
}

.manage-count b {
  color: var(--proto-ink);
  font-size: 24px;
  line-height: 1;
  letter-spacing: -.05em;
}

.manage-page-actions {
  display: flex;
  align-items: center;
  gap: 15px;
  padding-bottom: 1px;
}

.manage-toolbar {
  margin-top: 14px;
  padding: 0;
  overflow: visible;
}

.manage-toolbar-line {
  padding: 11px 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  border-bottom: 1px solid var(--proto-line);
}

.manage-filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 0;
  flex: 0 0 auto;
}

.manage-toolbar-line :deep(.ant-input-search) {
  width: min(340px, 100%);
  flex: 1 1 250px;
  min-width: 0;
}

.manage-alert { margin-top: 10px; }

.manage-table {
  padding-top: 17px;
  padding-bottom: 24px;
}

.manage-table-head {
  padding-bottom: 9px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: var(--proto-muted);
  font-size: 11px;
}

.manage-loading { padding: 22px; }

/* 空状态保留明确的主操作，但把上下留白压到与列表节奏一致。 */
.manage-empty { padding: 24px 24px 32px; }
.manage-empty :deep(.ant-empty-footer) { margin-top: 16px; }

.manage-list {
  overflow: hidden;
  border-top: 1px solid var(--proto-ink);
  border-bottom: 1px solid var(--proto-line);
}

.manage-row {
  min-height: 116px;
  padding: 10px 8px;
  display: grid;
  grid-template-columns: 26px 132px minmax(260px, 1fr) minmax(230px, 270px) 172px;
  gap: 14px;
  align-items: center;
  border-bottom: 1px solid var(--proto-line);
  transition: background-color .2s ease;
}

.manage-row:last-child { border-bottom: 0; }
.manage-row:hover { background: rgba(186, 255, 61, .08); }

.manage-thumb {
  width: 132px;
  height: 86px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  color: var(--proto-muted);
  font-size: 11px;
}

.manage-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.manage-main { min-width: 0; }
.manage-main strong {
  color: var(--proto-ink);
  font-size: 15px;
  line-height: 1.25;
  letter-spacing: -.03em;
}

.manage-main p {
  max-width: 58ch;
  margin: 6px 0;
  overflow: hidden;
  color: var(--proto-muted);
  font-size: 12px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.manage-tags { line-height: 1.3; }
.reject-message { margin-top: 5px; color: #973816; font-size: 10px; line-height: 1.45; }

.manage-meta {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.manage-meta-item { min-width: 0; }
.manage-meta-item span,
.manage-meta-item strong {
  display: block;
}

.manage-meta-item span {
  margin-bottom: 3px;
  color: var(--proto-muted);
  font-size: 10px;
  line-height: 1.2;
}

.manage-meta-item strong {
  overflow-wrap: anywhere;
  color: var(--proto-ink);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
}

.manage-action-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.manage-action-cell :deep(.ant-btn-link) {
  padding-inline: 5px;
  color: var(--proto-ink);
  font-size: 11px;
}

.manage-action-cell :deep(.ant-btn-link:not([disabled]):hover) { color: var(--proto-orange); }
.manage-action-cell :deep(.ant-btn-link-danger:not([disabled])) { color: #973816; }
.manage-pagination { display: flex; justify-content: flex-end; padding-top: 14px; }
.manage-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.manage-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }

/* 桌面端把筛选条件和操作按钮固定在同一行，避免操作区掉到下一行。 */
@media (min-width: 1280px) {
  .manage-toolbar-line { flex-wrap: nowrap; }
  .manage-toolbar-line :deep(.ant-input-search) {
    width: auto;
    flex: 1 1 0;
    min-width: 240px;
  }
}

@media (max-width: 1120px) {
  .manage-row { grid-template-columns: 26px 116px minmax(190px, 1fr) minmax(210px, 240px); }
  .manage-thumb { width: 116px; }
  .manage-action-cell { grid-column: 3 / -1; justify-content: flex-start; }
}

@media (max-width: 720px) {
  /* 移动端不硬锁高度，让图片信息按顺序自然展开。 */
  .manage-page-head { align-items: flex-start; flex-direction: column; }
  .manage-page-actions { width: 100%; justify-content: space-between; }
  .manage-filter-actions { width: 100%; margin-left: 0; flex-wrap: wrap; }
  .manage-row { grid-template-columns: 24px 96px 1fr; gap: 9px; }
  .manage-thumb { width: 96px; height: 72px; }
  .manage-meta, .manage-action-cell { grid-column: 3; justify-content: flex-start; }
  .manage-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 7px 12px; }
  .manage-action-cell { padding-top: 5px; border-top: 1px solid var(--proto-line); }
  .manage-main p { white-space: normal; }
}

@media (prefers-reduced-motion: reduce) {
  .manage-row { transition: none; }
}
</style>
