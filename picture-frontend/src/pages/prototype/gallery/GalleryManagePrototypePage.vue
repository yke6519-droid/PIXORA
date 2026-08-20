<template>
  <div class="manage-prototype">
    <section class="manage-page-head">
      <div class="manage-heading">
        <h1 class="manage-visually-hidden">我的图片</h1>
        <div class="manage-status-tabs" role="tablist" aria-label="图片审核状态">
          <button
            v-for="statusTab in statusTabs"
            :key="statusTab.value"
            type="button"
            class="manage-status-tab"
            :class="{ 'is-active': filters.pictureCheck === statusTab.value }"
            role="tab"
            :aria-selected="filters.pictureCheck === statusTab.value"
            @click="handleStatusTabClick(statusTab.value)"
          >
            <span>{{ statusTab.label }}</span>
            <strong :class="{ 'is-loading': countLoading }">{{ formatCount(statusTab.count) }}</strong>
          </button>
        </div>
      </div>
      <div class="manage-page-actions">
        <a-button
          class="proto-button acid-button"
          type="primary"
          @click="router.push('/gallery/upload')"
        >
          <UploadOutlined />
          上传图片
        </a-button>
      </div>
    </section>

    <section class="manage-toolbar proto-section">
      <div class="manage-search-row">
        <a-input-search
          v-model:value="filters.searchText"
          placeholder="搜索图片名称、标签..."
          allow-clear
          enter-button="搜索"
          :loading="loading"
          @search="runSearch"
        />
      </div>
      <div class="manage-filter-row">
        <label class="manage-filter-item">
          <span>图库:</span>
          <a-select
            v-model:value="filters.scope"
            aria-label="图库范围"
            @change="handleScopeChange"
          >
            <a-select-option value="public">公共图库</a-select-option>
            <a-select-option value="private" :disabled="!hasPrivateSpace">
              我的私人空间
            </a-select-option>
          </a-select>
        </label>
        <label class="manage-filter-item">
          <span>分类:</span>
          <a-select
            v-model:value="filters.category"
            :options="categoryOptions"
            :loading="optionLoading"
            allow-clear
            show-search
            placeholder="全部分类"
            @change="runSearch"
          />
        </label>
        <label class="manage-filter-item manage-tag-filter">
          <span>标签:</span>
          <a-select
            v-model:value="filters.tags"
            mode="multiple"
            :options="tagOptions"
            :loading="optionLoading"
            :max-tag-count="1"
            allow-clear
            placeholder="全部标签"
            @change="runSearch"
          />
        </label>
        <label class="manage-filter-item manage-sort-item">
          <span>排序:</span>
          <a-select v-model:value="filters.sortOrder" aria-label="排序方式" @change="runSearch">
            <a-select-option value="descend">最新上传</a-select-option>
            <a-select-option value="ascend">最早上传</a-select-option>
          </a-select>
        </label>
        <div class="manage-view-switch" role="group" aria-label="展示方式">
          <button
            type="button"
            class="manage-view-button"
            :class="{ 'is-active': viewMode === 'grid' }"
            aria-label="网格视图"
            :aria-pressed="viewMode === 'grid'"
            @click="viewMode = 'grid'"
          >
            <AppstoreOutlined />
          </button>
          <button
            type="button"
            class="manage-view-button"
            :class="{ 'is-active': viewMode === 'list' }"
            aria-label="列表视图"
            :aria-pressed="viewMode === 'list'"
            @click="viewMode = 'list'"
          >
            <MenuOutlined />
          </button>
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
        <div class="manage-batch-actions">
          <a-button class="proto-button ghost-button" @click="handleBatchManage">
            批量管理
          </a-button>
          <a-button class="proto-button ghost-button" @click="resetFilters">
            重置
          </a-button>
          <a-button
            class="proto-button ghost-button"
            danger
            :disabled="!selectedIds.length"
            @click="confirmBatchDelete"
          >
            删除已选
          </a-button>
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
          @click="router.push('/gallery/upload')"
        >
          上传第一张图片
        </a-button>
      </a-empty>

      <div v-else class="manage-list" :class="{ 'is-grid': viewMode === 'grid' }">
        <template v-if="viewMode === 'grid'">
          <PictureGalleryCard
            v-for="picture in pictures"
            :key="String(picture.id)"
            :picture="picture"
            :show-manage-controls="true"
            :selected="selectedIds.includes(normalizeId(picture.id))"
            :status-text="statusText(picture.pictureCheck)"
            :status-class="statusClass(picture.pictureCheck)"
            :reupload-enabled="picture.pictureCheck === 2"
            @open="openDetail(picture.id)"
            @select="(checked) => toggleOne(picture.id, checked)"
            @menu="(key) => handlePictureCardMenu(key, picture)"
          />
        </template>

        <template v-else>
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
                <strong>{{ pictureDimensions(picture) }}</strong>
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
        </template>
      </div>

      <div v-if="total > pageSize" class="manage-pagination">
        <a-pagination
          v-model:current="current"
          :page-size="pageSize"
          :total="total"
          :show-size-changer="false"
          show-less-items
          :show-total="(value: number, range: [number, number]) => `${range[0]}–${range[1]} / ${value}`"
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
import {
  AppstoreOutlined,
  MenuOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  deletePicture,
  listPictureCategory,
  queryPicturePage,
} from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import PictureManageEditorModal from './components/PictureManageEditorModal.vue'
import PictureGalleryCard from './components/PictureGalleryCard.vue'

type PictureStatus = 0 | 1 | 2
type PictureScope = 'public' | 'private'
type EditorMode = 'edit' | 'reupload'
type ViewMode = 'grid' | 'list'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(false)
const optionLoading = ref(false)
const countLoading = ref(false)
const authChecking = ref(true)
const loadError = ref('')
const pictures = ref<API.PictureVO[]>([])
const categories = ref<string[]>([])
const tags = ref<string[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 15
const selectedIds = ref<string[]>([])
const editorOpen = ref(false)
const editorMode = ref<EditorMode>('edit')
const activePicture = ref<API.PictureVO | null>(null)
const viewMode = ref<ViewMode>('grid')
const statusTotals = reactive<Record<PictureStatus, number | null>>({
  0: null,
  1: null,
  2: null,
})

const filters = reactive({
  searchText: '',
  pictureCheck: 1 as PictureStatus,
  scope: 'public' as PictureScope,
  category: undefined as string | undefined,
  tags: [] as string[],
  sortOrder: 'descend' as 'ascend' | 'descend',
})

const categoryOptions = computed(() =>
  categories.value.map((item) => ({ label: item, value: item })),
)
const tagOptions = computed(() =>
  tags.value.map((item) => ({ label: item, value: item })),
)
const statusTabs = computed(() => [
  { value: 1 as PictureStatus, label: '已通过', count: statusTotals[1] },
  { value: 0 as PictureStatus, label: '待审核', count: statusTotals[0] },
  { value: 2 as PictureStatus, label: '未通过', count: statusTotals[2] },
])
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
        path: '/user/login',
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
 * 统一生成“我的图片”查询参数，列表和顶部状态统计共用同一套空间范围。
 * 统计故意不带搜索、分类和标签条件，只随公共图库/私人空间切换。
 */
function buildPictureQuery(
  pictureCheck: PictureStatus,
  size: number,
  includeListFilters = true,
): API.PictureQueryRequest {
  const currentUser = loginUserStore.loginUser
  const query: API.PictureQueryRequest = {
    current: includeListFilters ? current.value : 1,
    pageSize: size,
    userId: currentUser?.id,
    spaceId: filters.scope === 'private' ? currentUser?.spaceId : 0,
    pictureCheck,
    sortFiled: 'createtime',
    sortOrder: filters.sortOrder,
  }

  if (includeListFilters) {
    query.searchText = filters.searchText.trim() || undefined
    query.category = filters.category
    query.tags = filters.tags.length ? [...filters.tags] : undefined
  }

  return query
}

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
    const res = await queryPicturePage(
      buildPictureQuery(filters.pictureCheck, pageSize),
    )

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

/**
 * 顶部统计使用现有分页接口的 total 字段，不伪造数量，也不额外增加后端接口。
 */
async function loadStatusCounts() {
  const currentUser = loginUserStore.loginUser
  if (!currentUser?.id) return

  countLoading.value = true
  try {
    const statuses: PictureStatus[] = [1, 0, 2]
    const responses = await Promise.all(
      statuses.map((status) => queryPicturePage(buildPictureQuery(status, 1, false))),
    )

    statuses.forEach((status, index) => {
      const response = responses[index]
      statusTotals[status] = response.data?.code === 200
        ? Number(response.data.data?.total || 0)
        : null
    })
  } catch {
    ;([0, 1, 2] as PictureStatus[]).forEach((status) => {
      statusTotals[status] = null
    })
  } finally {
    countLoading.value = false
  }
}

async function loadOptions() {
  optionLoading.value = true
  try {
    const res = await listPictureCategory()
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

function handleStatusTabClick(status: PictureStatus) {
  if (filters.pictureCheck === status) return
  filters.pictureCheck = status
  current.value = 1
  void loadPictures()
}

function handleScopeChange() {
  if (filters.scope === 'private' && !hasPrivateSpace.value) {
    filters.scope = 'public'
  }
  current.value = 1
  void Promise.all([loadPictures(), loadStatusCounts()])
}

function resetFilters() {
  filters.searchText = ''
  filters.pictureCheck = 1
  filters.scope = 'public'
  filters.category = undefined
  filters.tags = []
  filters.sortOrder = 'descend'
  current.value = 1
  void Promise.all([loadPictures(), loadStatusCounts()])
}

function handleBatchManage() {
  if (!selectedIds.value.length) {
    message.info('请先选择要管理的图片')
    return
  }
  confirmBatchDelete()
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
    void router.push(`/gallery/detail/${encodeURIComponent(value)}`)
  }
}

function openEditor(picture: API.PictureVO, mode: EditorMode) {
  activePicture.value = picture
  editorMode.value = mode
  editorOpen.value = true
}

function handleCardMenuClick(event: { key: string }, picture: API.PictureVO) {
  if (event.key === 'detail') {
    openDetail(picture.id)
    return
  }
  if (event.key === 'edit') {
    openEditor(picture, 'edit')
    return
  }
  if (event.key === 'reupload') {
    openEditor(picture, 'reupload')
    return
  }
  if (event.key === 'delete') {
    Modal.confirm({
      title: '确定删除这张图片吗？',
      content: '删除后无法恢复。',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => deleteOne(picture.id),
    })
  }
}

// 共享卡片只抛出菜单 key，页面在这里继续复用原有的编辑、重传和删除逻辑。
function handlePictureCardMenu(key: string, picture: API.PictureVO) {
  handleCardMenuClick({ key }, picture)
}

async function handleEditorSuccess() {
  await Promise.all([loadPictures(), loadStatusCounts()])
}

async function deleteOne(id?: number | string) {
  const value = normalizeId(id)
  if (!value) return

  try {
    const res = await deletePicture({ id: value })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '删除失败')
    }
    message.success('图片已删除')
    if (pictures.value.length === 1 && current.value > 1) {
      current.value -= 1
    }
    await Promise.all([loadPictures(), loadStatusCounts()])
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
          const res = await deletePicture({ id })
          if (res.data?.code === 200) successCount += 1
          else failedCount += 1
        } catch {
          failedCount += 1
        }
      }

      if (successCount) message.success(`成功删除 ${successCount} 张图片`)
      if (failedCount) message.error(`${failedCount} 张图片删除失败`)
      selectedIds.value = []
      await Promise.all([loadPictures(), loadStatusCounts()])
    },
  })
}

function formatFileSize(size?: number | string) {
  const normalizedSize = Number(size)
  if (!Number.isFinite(normalizedSize) || normalizedSize <= 0) return '大小未知'
  if (normalizedSize < 1024 * 1024) return `${(normalizedSize / 1024).toFixed(1)} KB`
  return `${(normalizedSize / 1024 / 1024).toFixed(2)} MB`
}

function pictureDimensions(picture: API.PictureVO) {
  if (picture.picwidth && picture.picheight) {
    return `${picture.picwidth} × ${picture.picheight}`
  }
  return '尺寸未知'
}

function formatCount(value: number | null) {
  return value === null ? '—' : value.toLocaleString()
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
  await Promise.all([loadOptions(), loadPictures(), loadStatusCounts()])
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

/*
 * 原型式管理视图：默认让图片成为主角，列表视图继续保留给需要密集信息的场景。
 * 数量、尺寸和时间使用品牌数字字体，操作文字保持清晰的产品字体。
 */
.manage-visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.manage-page-head {
  min-height: 82px;
  padding-top: 19px;
  padding-bottom: 16px;
  align-items: flex-end;
  border-bottom-color: rgba(17, 20, 22, .10);
}

.manage-heading {
  min-width: 0;
}

.manage-status-tabs {
  display: flex;
  align-items: flex-end;
  gap: 25px;
  min-height: 42px;
}

.manage-status-tab {
  position: relative;
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  gap: 13px;
  padding: 0 11px 11px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  color: var(--proto-muted);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.manage-status-tab strong {
  color: var(--proto-ink);
  font-family: 'Abril Fatface', Georgia, serif;
  font-size: 18px;
  font-weight: 400;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.manage-status-tab strong.is-loading {
  color: var(--proto-muted);
}

.manage-status-tab:hover,
.manage-status-tab.is-active {
  color: var(--proto-ink);
}

.manage-status-tab.is-active {
  border-bottom-color: var(--proto-acid);
}

.manage-status-tab:focus-visible,
.manage-view-button:focus-visible {
  outline: 2px solid var(--proto-orange);
  outline-offset: 2px;
}

.manage-page-actions {
  padding-bottom: 1px;
}

.manage-page-actions :deep(.ant-btn) {
  min-width: 120px;
  height: 40px;
  border-radius: 8px;
  font-size: 13px;
}

.manage-page-actions :deep(.anticon) {
  margin-inline-end: 6px;
}

.manage-toolbar {
  margin-top: 0;
  padding-top: 20px;
  overflow: visible;
}

.manage-search-row {
  display: flex;
  width: min(920px, 100%);
}

.manage-search-row :deep(.ant-input-search) {
  width: 100%;
}

.manage-search-row :deep(.ant-input-affix-wrapper),
.manage-search-row :deep(.ant-input-search-button) {
  height: 44px;
}

.manage-search-row :deep(.ant-input-affix-wrapper) {
  border-radius: 8px 0 0 8px;
  border-color: rgba(17, 20, 22, .14);
  background: rgba(255, 255, 255, .74);
}

.manage-search-row :deep(.ant-input-search-button) {
  min-width: 72px;
  border-radius: 0 8px 8px 0;
  background: var(--proto-ink);
  color: var(--proto-paper);
  font-weight: 700;
}

.manage-filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  margin-top: 16px;
  padding-bottom: 17px;
  border-bottom: 1px solid rgba(17, 20, 22, .10);
}

.manage-filter-item {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  color: var(--proto-muted);
  font-size: 12px;
  white-space: nowrap;
}

.manage-filter-item :deep(.ant-select) {
  width: 140px;
  min-width: 0;
}

.manage-filter-item :deep(.ant-select-selector) {
  min-height: 40px;
  align-items: center;
  border-radius: 8px !important;
  border-color: rgba(17, 20, 22, .13) !important;
  background: rgba(255, 255, 255, .60) !important;
}

.manage-tag-filter :deep(.ant-select) {
  width: 174px;
}

.manage-sort-item {
  margin-left: auto;
}

.manage-view-switch {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  padding: 2px;
  border: 1px solid rgba(17, 20, 22, .12);
  border-radius: 9px;
  background: rgba(255, 255, 255, .62);
}

.manage-view-button {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--proto-muted);
  cursor: pointer;
  font-size: 16px;
}

.manage-view-button:hover,
.manage-view-button.is-active {
  background: var(--proto-acid);
  color: var(--proto-ink);
}

.manage-table {
  padding-top: 21px;
  padding-bottom: 28px;
}

.manage-table-head {
  min-height: 42px;
  padding-bottom: 13px;
  justify-content: space-between;
  border-bottom: 1px solid rgba(17, 20, 22, .10);
  font-size: 12px;
}

.manage-batch-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.manage-batch-actions :deep(.ant-btn) {
  min-height: 36px;
  border-radius: 8px;
  font-size: 12px;
}

.manage-list.is-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  overflow: visible;
  border: 0;
}

.manage-pagination {
  justify-content: center;
  padding-top: 25px;
}

@media (max-width: 1240px) {
  .manage-list.is-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .manage-filter-row {
    flex-wrap: wrap;
  }

  .manage-sort-item {
    margin-left: 0;
  }

  .manage-view-switch {
    margin-left: auto;
  }
}

@media (max-width: 920px) {
  .manage-list.is-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .manage-status-tabs {
    gap: 8px;
  }

  .manage-status-tab {
    gap: 8px;
    padding-inline: 7px;
  }
}

@media (max-width: 720px) {
  .manage-page-head {
    align-items: stretch;
    flex-direction: column;
    gap: 15px;
  }

  .manage-status-tabs {
    justify-content: space-between;
  }

  .manage-status-tab {
    flex: 1 1 0;
    justify-content: center;
  }

  .manage-page-actions {
    justify-content: flex-end;
  }

  .manage-search-row {
    width: 100%;
  }

  .manage-filter-row {
    align-items: stretch;
    gap: 10px;
  }

  .manage-filter-item {
    flex: 1 1 calc(50% - 10px);
    justify-content: space-between;
  }

  .manage-filter-item :deep(.ant-select),
  .manage-tag-filter :deep(.ant-select) {
    flex: 1 1 auto;
    width: auto;
  }

  .manage-view-switch {
    margin-left: 0;
  }

  .manage-list.is-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .manage-table-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 11px;
  }

  .manage-batch-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}

@media (max-width: 520px) {
  .manage-status-tab {
    gap: 5px;
    padding-inline: 3px;
    font-size: 12px;
  }

  .manage-status-tab strong {
    font-size: 16px;
  }

  .manage-filter-item {
    flex-basis: 100%;
  }

  .manage-list.is-grid {
    grid-template-columns: 1fr;
  }
}

</style>
