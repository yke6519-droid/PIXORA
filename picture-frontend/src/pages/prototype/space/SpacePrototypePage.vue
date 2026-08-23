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

    <section v-else-if="!space" class="space-empty" aria-labelledby="space-empty-title">
      <div class="space-empty-header">
        <div class="space-empty-eyebrow"><UserOutlined aria-hidden="true" /> 个人空间</div>
        <h1 id="space-empty-title">创建你的个人图片空间</h1>
        <p>保存、整理并管理只属于你的图片。</p>
      </div>

      <article class="space-empty-card" aria-label="个人空间创建引导">
        <div class="space-empty-illustration" aria-hidden="true">
          <div class="space-illustration-grid" />
          <span class="space-illustration-folder"><FolderOpenOutlined /></span>
          <span class="space-illustration-picture"><PictureOutlined /></span>
          <span class="space-illustration-lock"><LockOutlined /></span>
        </div>

        <div class="space-empty-benefits">
          <div v-for="benefit in spaceBenefits" :key="benefit.key" class="space-empty-benefit">
            <span class="space-empty-benefit-icon"><component :is="benefit.icon" aria-hidden="true" /></span>
            <strong>{{ benefit.value }}</strong>
            <span>{{ benefit.label }}</span>
          </div>
        </div>

        <div class="space-empty-divider" />

        <a-button class="proto-button acid-button space-empty-create" type="primary" @click="createOpen = true">
          创建个人空间
        </a-button>
        <p class="space-empty-note">创建后即可上传、整理和管理你的私人图片。</p>
      </article>
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
                <a-menu-item key="tags">管理标签</a-menu-item>
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

      <div class="space-gallery-layout">
        <aside class="space-tag-sidebar proto-surface" aria-label="图片标签筛选">
          <div class="space-tag-sidebar-head">
            <div>
                <h2>{{ batchTagMode ? '选择标签' : '标签筛选' }}</h2>
                <small>{{ batchTagMode ? `${selectedBatchTagIds.length} / 3 个` : '可多选' }}</small>
            </div>
            <span>{{ tags.length }}</span>
          </div>

          <nav class="space-tag-list">
            <button
              type="button"
              class="space-tag-item"
              :class="{ 'is-active': batchTagMode ? selectedBatchTagIds.length === 0 : selectedTagIds.length === 0 }"
              @click="clearTagFilter"
            >
              <span>全部图片</span>
            </button>

            <button
              v-for="tag in tags"
              :key="String(tag.id)"
              type="button"
              class="space-tag-item"
              :class="{ 'is-active': batchTagMode ? selectedBatchTagIds.includes(normalizeId(tag.id)) : selectedTagIds.includes(normalizeId(tag.id)) }"
              @click="handleSidebarTagClick(normalizeId(tag.id))"
            >
              <span>{{ tag.tagName }}</span>
            </button>

            <span v-if="!tags.length" class="space-tag-empty">还没有可用标签</span>
          </nav>

          <a-button
            v-if="batchTagMode"
            class="space-tag-manage-button"
            @click="openBatchTagCreate"
          >
            + 新增标签
          </a-button>
          <a-button
            v-else
            class="space-tag-manage-button"
            @click="tagManageOpen = true"
          >
            管理标签
          </a-button>
        </aside>

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
                  :disabled="batchTagMode"
                  @click="changeStatus(option.value)"
                >
                  <span>{{ option.label }}</span>
                  <strong v-if="option.value === 'all'">{{ totalPictureCount }}</strong>
                </button>
              </div>
              <a-button
                v-if="!batchTagMode"
                class="proto-button acid-button"
                type="primary"
                @click="openSpaceUpload"
              >
                上传到空间
              </a-button>
              <a-button
                class="proto-button ghost-button"
                :type="batchTagMode ? 'primary' : 'default'"
                @click="toggleBatchTagMode"
              >
                {{ batchTagMode ? '退出批量绑定' : '批量绑定标签' }}
              </a-button>
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
              :class="{ 'is-batch-selected': selectedBatchPictureIds.includes(normalizeId(picture.id)) }"
              role="button"
              tabindex="0"
              :aria-pressed="batchTagMode ? selectedBatchPictureIds.includes(normalizeId(picture.id)) : undefined"
              @click="handlePictureClick(picture)"
              @keydown.enter="handlePictureClick(picture)"
            >
              <div class="space-picture-image proto-image-wrap">
                <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '空间图片'" />
                <div v-if="batchTagMode" class="space-picture-select" @click.stop>
                  <a-checkbox
                    :checked="selectedBatchPictureIds.includes(normalizeId(picture.id))"
                    :aria-label="`选择 ${picture.name || '图片'}`"
                    @change="(event: any) => toggleBatchPicture(picture.id, event.target.checked)"
                  />
                </div>
                <a-tag class="space-picture-status" :class="statusClass(picture.pictureCheck)">
                  {{ pictureStatusLabel(picture.pictureCheck) }}
                </a-tag>
                <a-button
                  v-if="!batchTagMode"
                  class="space-picture-delete"
                  size="small"
                  danger
                  aria-label="删除图片"
                  @click.stop="confirmDeletePicture(picture)"
                ><DeleteOutlined aria-hidden="true" /></a-button>
                <div class="space-picture-overlay">
                  <strong>{{ picture.name || '未命名图片' }}</strong>
                  <span>{{ picture.tags?.length ? picture.tags.join(' · ') : '暂无标签' }}</span>
                  <a-button
                    v-if="!batchTagMode"
                    class="space-picture-tag-button"
                    size="small"
                    @click.stop="openPictureTagManage(picture)"
                  >
                    标签
                  </a-button>
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
            :disabled="batchTagMode"
            show-less-items
            @change="changePage"
          />

          <div v-if="batchTagMode" class="space-batch-tag-bar">
            <div class="space-batch-tag-summary">
              <strong>批量绑定标签</strong>
              <span>已选 {{ selectedBatchPictureIds.length }} 张图片</span>
              <span>已选 {{ selectedBatchTagIds.length }} 个标签</span>
            </div>
            <div class="space-batch-tag-actions">
              <a-button @click="toggleAllBatchPictures">
                {{ allBatchPicturesSelected ? '取消全选' : '选择当前页' }}
              </a-button>
              <a-button @click="clearBatchTagSelection">清空</a-button>
              <a-button
                type="primary"
                :loading="batchTagLoading"
                :disabled="!selectedBatchPictureIds.length || !selectedBatchTagIds.length"
                @click="submitBatchTagBinding"
              >
                确认绑定
              </a-button>
            </div>
          </div>
        </section>
      </div>
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
    <SpaceTagManageModal
      v-if="space"
      v-model:open="tagManageOpen"
      :space-id="space.id!"
      :max-tag-count="maxTagCount"
      @changed="handleSpaceTagsChanged"
    />
    <a-modal
      v-model:open="batchTagCreateOpen"
      title="新增个人标签"
      ok-text="创建标签"
      cancel-text="取消"
      :confirm-loading="batchTagCreateLoading"
      @ok="submitBatchTagCreate"
    >
      <p class="space-batch-tag-create-hint">创建成功后会自动加入当前批量绑定选择。</p>
      <a-form layout="vertical" class="proto-form">
        <a-form-item label="标签名称" required>
          <a-input
            v-model:value="newBatchTagName"
            :maxlength="32"
            show-count
            placeholder="例如：待整理"
            @press-enter="submitBatchTagCreate"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <PictureTagBatchModal
      v-if="space"
      v-model:open="pictureTagManageOpen"
      :space-id="space.id!"
      :picture-ids="selectedPictureIds"
      :tags="tags"
      @changed="handlePictureTagsChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  DatabaseOutlined,
  DeleteOutlined,
  FolderOpenOutlined,
  LockOutlined,
  MoreOutlined,
  PictureOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import {
  createSpace as createSpaceApi,
  deleteById,
  queryDefaultQuota,
  querySpaceById,
  updateById,
} from '../../../api/spaceController'
import {
  deletePicture,
  queryPicturePage,
} from '../../../api/pictureController'
import { createTag, listTag } from '../../../api/tagController'
import { addPictureTags } from '../../../api/pictureTagController'
import { getCurrentUser } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { formatSpaceLevel, pictureStatusText } from '../prototypeData'
import PictureTagBatchModal from '../gallery/components/PictureTagBatchModal.vue'
import SpaceNameModal from './components/SpaceNameModal.vue'
import SpaceTagManageModal from './components/SpaceTagManageModal.vue'

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
const tagManageOpen = ref(false)
const pictureTagManageOpen = ref(false)
const batchTagMode = ref(false)
const batchTagLoading = ref(false)
const batchTagCreateOpen = ref(false)
const batchTagCreateLoading = ref(false)
const newBatchTagName = ref('')
const space = ref<API.SpaceVO | null>(null)
const spaceQuota = ref<API.SpaceLevel | null>(null)
const pictures = ref<API.PictureVO[]>([])
const tags = ref<API.Tag[]>([])
const selectedTagIds = ref<string[]>([])
const selectedPictureIds = ref<string[]>([])
const selectedBatchPictureIds = ref<string[]>([])
const selectedBatchTagIds = ref<string[]>([])
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
  if (selectedTagIds.value.length) return '没有符合当前标签筛选的图片'
  if (pictureCheck.value === 'all') return '空间中暂无图片'
  return `空间中暂无${pictureStatusText(pictureCheck.value)}图片`
})
const maxTagCount = computed(() => {
  const level = toNumber(space.value?.spaceLevel)
  if (level >= 2) return 30
  if (level === 1) return 20
  return 10
})
const allBatchPicturesSelected = computed(() => (
  pictures.value.length > 0
  && pictures.value.every((picture) => selectedBatchPictureIds.value.includes(normalizeId(picture.id)))
))
const spaceBenefits = computed(() => [
  {
    key: 'storage',
    value: formatQuotaSize(spaceQuota.value?.maxSize),
    label: '存储空间',
    icon: DatabaseOutlined,
  },
  {
    key: 'pictures',
    value: `${toNumber(spaceQuota.value?.maxCount)} 张`,
    label: '图片上限',
    icon: PictureOutlined,
  },
  {
    key: 'privacy',
    value: '仅自己可见',
    label: '私密访问',
    icon: LockOutlined,
  },
])

function toNumber(value?: number | string) {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed : 0
}

function normalizeId(value?: number | string) {
  return value == null ? '' : String(value)
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

/** 创建页展示额度时去掉无意义的 .0，保留后端真实配置。 */
function formatQuotaSize(value?: number | string) {
  return formatSize(value).replace('.0 MB', ' MB').replace('.0 KB', ' KB')
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

async function loadDefaultQuota() {
  const res = await queryDefaultQuota()
  if (res.data?.code !== 200 || !res.data.data) {
    throw new Error(res.data?.message || '空间额度加载失败')
  }
  spaceQuota.value = res.data.data
}

/** 只加载正常标签，停用标签不会出现在个人空间的筛选和绑定列表中。 */
async function loadSpaceTags() {
  if (!space.value?.id) {
    tags.value = []
    return
  }

  try {
    const res = await listTag({ spaceId: space.value.id })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '空间标签加载失败')
    }
    tags.value = res.data.data || []
  } catch (error: any) {
    tags.value = []
    message.warning(error?.response?.data?.message || error?.message || '空间标签加载失败')
  }
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
          tagIds: selectedTagIds.value.length ? [...selectedTagIds.value] : undefined,
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
      tagIds: selectedTagIds.value.length ? [...selectedTagIds.value] : undefined,
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
    if (!currentUser) return
    if (!hasSpaceId(currentUser)) {
      space.value = null
      pictures.value = []
      pictureTotal.value = 0
      allPictureTotal.value = null
      await loadDefaultQuota()
      return
    }
    await loadSpace(currentUser.spaceId!)
    selectedTagIds.value = []
    await loadSpaceTags()
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
  if (event.key === 'tags') tagManageOpen.value = true
  if (event.key === 'rename') renameOpen.value = true
  if (event.key === 'delete') confirmDeleteSpace()
}

/** 标签创建、改名或删除后，刷新侧边标签和当前图片列表。 */
async function handleSpaceTagsChanged() {
  await loadSpaceTags()
  selectedTagIds.value = selectedTagIds.value.filter((id) => (
    tags.value.some((tag) => normalizeId(tag.id) === id)
  ))
  selectedBatchTagIds.value = selectedBatchTagIds.value.filter((id) => (
    tags.value.some((tag) => normalizeId(tag.id) === id)
  ))
  await loadPictures()
}

/** 单张图片也可以直接打开标签操作弹窗，避免用户必须先进入管理列表。 */
function openPictureTagManage(picture: API.PictureVO) {
  if (batchTagMode.value) return
  if (picture.id == null) return
  selectedPictureIds.value = [String(picture.id)]
  pictureTagManageOpen.value = true
}

/** 添加或移除标签后，重新查询图片，使卡片上的标签立即更新。 */
async function handlePictureTagsChanged() {
  await loadPictures()
}

/** 进入批量绑定模式时，保留当前图片列表，等待用户选择图片和标签。 */
function toggleBatchTagMode() {
  if (batchTagMode.value) {
    if (batchTagLoading.value) return
    batchTagMode.value = false
    clearBatchTagSelection()
    return
  }
  selectedBatchPictureIds.value = []
  selectedBatchTagIds.value = []
  batchTagMode.value = true
}

function clearBatchTagSelection() {
  selectedBatchPictureIds.value = []
  selectedBatchTagIds.value = []
}

function handlePictureClick(picture: API.PictureVO) {
  if (batchTagMode.value) {
    toggleBatchPicture(picture.id, !selectedBatchPictureIds.value.includes(normalizeId(picture.id)))
    return
  }
  openPicture(picture.id)
}

function toggleBatchPicture(pictureId: number | string | undefined, checked: boolean) {
  const id = normalizeId(pictureId)
  if (!id) return
  selectedBatchPictureIds.value = checked
    ? [...new Set([...selectedBatchPictureIds.value, id])]
    : selectedBatchPictureIds.value.filter((item) => item !== id)
}

function toggleAllBatchPictures() {
  if (allBatchPicturesSelected.value) {
    const currentPageIds = pictures.value.map((picture) => normalizeId(picture.id))
    selectedBatchPictureIds.value = selectedBatchPictureIds.value.filter(
      (id) => !currentPageIds.includes(id),
    )
    return
  }
  const currentPageIds = pictures.value.map((picture) => normalizeId(picture.id)).filter(Boolean)
  selectedBatchPictureIds.value = [...new Set([...selectedBatchPictureIds.value, ...currentPageIds])]
}

function handleSidebarTagClick(tagId: string) {
  if (!tagId) return
  if (batchTagMode.value) {
    toggleBatchTag(tagId)
    return
  }
  toggleTag(tagId)
}

function toggleBatchTag(tagId: string) {
  if (selectedBatchTagIds.value.includes(tagId)) {
    selectedBatchTagIds.value = selectedBatchTagIds.value.filter((id) => id !== tagId)
    return
  }
  if (selectedBatchTagIds.value.length >= 3) {
    message.info('一张图片最多绑定 3 个标签')
    return
  }
  selectedBatchTagIds.value = [...selectedBatchTagIds.value, tagId]
}

function openBatchTagCreate() {
  newBatchTagName.value = ''
  batchTagCreateOpen.value = true
}

async function submitBatchTagCreate() {
  const tagName = newBatchTagName.value.trim()
  if (!tagName) {
    message.warning('请输入标签名称')
    return
  }
  if (!space.value?.id) {
    message.error('当前没有可用的个人空间')
    return
  }

  batchTagCreateLoading.value = true
  try {
    const res = await createTag({ spaceId: space.value.id, tagName })
    if (res.data?.code !== 200 || !res.data.data) {
      throw new Error(res.data?.message || '标签创建失败')
    }
    const createdTagId = normalizeId(res.data.data.id)
    batchTagCreateOpen.value = false
    newBatchTagName.value = ''
    await loadSpaceTags()
    if (createdTagId && selectedBatchTagIds.value.length < 3) {
      selectedBatchTagIds.value = [...selectedBatchTagIds.value, createdTagId]
      message.success('标签已创建并选中')
    } else {
      message.success('标签已创建，请先取消一个标签再选择')
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '标签创建失败')
  } finally {
    batchTagCreateLoading.value = false
  }
}

async function submitBatchTagBinding() {
  if (!selectedBatchPictureIds.value.length || !selectedBatchTagIds.value.length) {
    message.warning('请至少选择一张图片和一个标签')
    return
  }
  if (!space.value?.id) {
    message.error('当前没有可用的个人空间')
    return
  }

  batchTagLoading.value = true
  try {
    const pictureCount = selectedBatchPictureIds.value.length
    const tagCount = selectedBatchTagIds.value.length
    const res = await addPictureTags({
      spaceId: space.value.id,
      pictureIds: [...selectedBatchPictureIds.value],
      tagIds: [...selectedBatchTagIds.value],
    })
    if (res.data?.code !== 200 || res.data.data === false) {
      throw new Error(res.data?.message || '批量绑定标签失败')
    }
    batchTagMode.value = false
    clearBatchTagSelection()
    message.success(`已为 ${pictureCount} 张图片绑定 ${tagCount} 个标签`)
    await Promise.all([loadSpaceTags(), loadPictures()])
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '批量绑定标签失败')
  } finally {
    batchTagLoading.value = false
  }
}

function toggleTag(tagId: string) {
  if (!tagId) return
  if (selectedTagIds.value.includes(tagId)) {
    selectedTagIds.value = selectedTagIds.value.filter((id) => id !== tagId)
  } else {
    selectedTagIds.value = [...selectedTagIds.value, tagId]
  }
  current.value = 1
  void loadPictures()
}

function clearTagFilter() {
  if (batchTagMode.value) {
    selectedBatchTagIds.value = []
    return
  }
  if (!selectedTagIds.value.length) return
  selectedTagIds.value = []
  current.value = 1
  void loadPictures()
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
  if (batchTagMode.value) return
  if (pictureCheck.value === value) return
  pictureCheck.value = value
  current.value = 1
  void loadPictures()
}

function changePage(page: number) {
  if (batchTagMode.value) return
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
  max-width: 720px;
  margin: 0 auto;
  padding: 58px 24px 72px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
}
.space-empty-header { display: flex; flex-direction: column; align-items: center; text-align: center; }
.space-empty-eyebrow {
  min-height: 30px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  background: rgba(186,255,61,.18);
  color: #4d7e13;
  font-size: 13px;
  font-weight: 700;
}
.space-empty-eyebrow :deep(.anticon) { font-size: 14px; }
.space-empty h1 {
  margin: 24px 0 0;
  color: var(--proto-ink);
  font-size: clamp(34px, 4vw, 42px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -.055em;
  text-align: center;
}
.space-empty-header p { margin: 12px 0 0; color: var(--proto-muted); font-size: 16px; line-height: 1.5; text-align: center; }
.space-empty-card {
  width: min(100%, 620px);
  padding: 30px 34px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid rgba(17,20,22,.1);
  border-radius: 16px;
  background: rgba(255,255,255,.9);
  box-shadow: 0 16px 42px rgba(18,23,23,.08);
}
.space-empty-illustration { position: relative; width: 230px; height: 154px; margin-bottom: 20px; color: var(--proto-ink-soft); }
.space-illustration-grid {
  position: absolute;
  inset: 14px 18px 4px;
  opacity: .26;
  background-image: linear-gradient(rgba(17,20,22,.12) 1px, transparent 1px), linear-gradient(90deg, rgba(17,20,22,.12) 1px, transparent 1px);
  background-size: 20px 20px;
  mask-image: radial-gradient(ellipse at center, #000 20%, transparent 76%);
}
.space-illustration-folder { position: absolute; top: 24px; left: 45px; color: #818880; font-size: 116px; line-height: 1; }
.space-illustration-picture { position: absolute; top: 69px; left: 94px; display: inline-flex; color: var(--proto-ink); font-size: 52px; line-height: 1; }
.space-illustration-picture :deep(.anticon) { filter: drop-shadow(0 2px 0 rgba(255,255,255,.8)); }
.space-illustration-lock {
  position: absolute;
  top: 17px;
  left: 42px;
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(17,20,22,.55);
  border-radius: 50%;
  background: rgba(255,255,255,.92);
  color: #79b51c;
  font-size: 18px;
}
.space-empty-benefits { width: 100%; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.space-empty-benefit { min-width: 0; padding: 0 16px; display: flex; flex-direction: column; align-items: center; gap: 7px; text-align: center; }
.space-empty-benefit:not(:last-child) { border-right: 1px solid rgba(17,20,22,.1); }
.space-empty-benefit-icon { height: 26px; display: inline-flex; align-items: center; color: #76b31b; font-size: 23px; }
.space-empty-benefit-icon :deep(.anticon) { font-size: inherit; }
.space-empty-benefit strong { color: var(--proto-ink); font-family: 'Abril Fatface', Georgia, serif; font-size: 24px; font-weight: 400; line-height: 1.15; white-space: nowrap; }
.space-empty-benefit span:last-child { color: var(--proto-muted); font-size: 12px; line-height: 1.2; }
.space-empty-divider { width: 100%; margin: 26px 0 0; border-top: 1px solid rgba(17,20,22,.1); }
.space-empty-create { width: min(100%, 320px); height: 48px; margin-top: 26px; border-radius: 9px; font-size: 15px; font-weight: 700; }
.space-empty-note { margin: 14px 0 0; color: var(--proto-muted); font-size: 13px; line-height: 1.45; text-align: center; }
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

/* 个人空间的标签筛选和公共图库的主题侧栏保持相同的使用方式。 */
.space-gallery-layout { display: grid; grid-template-columns: minmax(190px, .18fr) minmax(0, .82fr); gap: 24px; padding-top: 18px; align-items: start; }
.space-tag-sidebar { position: sticky; top: calc(var(--prototype-topbar-height, 52px) + 18px); min-width: 0; padding: 16px 12px; border: 1px solid rgba(17,20,22,.05); border-radius: 14px; background: rgba(255,255,255,.66); box-shadow: 0 12px 30px rgba(18,23,23,.035); }
.space-tag-sidebar-head { min-height: 38px; padding: 0 10px 10px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(17,20,22,.08); }
.space-tag-sidebar-head h2 { margin: 0; font-size: 14px; font-weight: 800; letter-spacing: -.03em; }
.space-tag-sidebar-head small { display: block; margin-top: 4px; color: var(--proto-muted); font-size: 10px; }
.space-tag-sidebar-head > span { color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
.space-tag-list { padding-top: 10px; display: flex; flex-direction: column; gap: 3px; }
.space-tag-item { min-height: 42px; padding: 0 10px; display: flex; align-items: center; border: 0; border-radius: 9px; background: transparent; color: var(--proto-muted); cursor: pointer; font-family: inherit; font-size: 12px; text-align: left; transition: background .2s ease, color .2s ease; }
.space-tag-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.space-tag-item:hover { background: var(--proto-paper-deep); color: var(--proto-ink); }
.space-tag-item.is-active { background: var(--proto-ink); color: var(--proto-paper); font-weight: 700; }
.space-tag-item.is-active:hover { background: var(--proto-ink); color: var(--proto-paper); }
.space-tag-empty { padding: 12px 10px; color: var(--proto-muted); font-size: 11px; }
.space-tag-manage-button { width: 100%; margin-top: 12px; }

/* 图片标题与筛选操作保持同一层级，先浏览，再进行状态筛选。 */
.space-gallery.proto-section { min-width: 0; padding-top: 0; }
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
.space-picture-card.is-batch-selected { box-shadow: 0 0 0 3px rgba(186,255,61,.8), 0 10px 26px rgba(18,23,23,.12); }
.space-picture-image { position: relative; aspect-ratio: 4 / 3; overflow: hidden; border: 1px solid rgba(17,20,22,.08); border-radius: 12px; background: var(--proto-paper-deep); }
.space-picture-image::after { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(17,20,22,0) 45%, rgba(17,20,22,.78) 100%); content: ''; pointer-events: none; }
.space-picture-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.space-picture-select { position: absolute; z-index: 4; top: 9px; right: 9px; width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(246,247,242,.92); }
.space-picture-select :deep(.ant-checkbox-wrapper) { line-height: 1; }
.space-picture-status { position: absolute; z-index: 2; top: 10px; left: 10px; margin: 0; padding: 4px 8px; border: 0; border-radius: 999px; background: rgba(246,247,242,.9); color: var(--proto-ink); font-size: 10px; font-weight: 700; line-height: 1.2; }
.space-picture-status.pass { background: rgba(224,244,186,.94); }
.space-picture-status.wait { background: rgba(255,195,77,.94); }
.space-picture-status.refuse { background: rgba(255,152,125,.94); }
.space-picture-delete { position: absolute; z-index: 3; top: 9px; right: 9px; width: 30px; height: 30px; padding: 0; display: inline-flex; align-items: center; justify-content: center; border: 0; border-radius: 50%; background: rgba(246,247,242,.92); color: var(--proto-ink); opacity: 0; transition: opacity .2s ease, background-color .2s ease; }
.space-picture-delete:hover { background: #ff987d; color: var(--proto-ink); }
.space-picture-card:hover .space-picture-delete, .space-picture-card:focus-within .space-picture-delete { opacity: 1; }
.space-picture-overlay { position: absolute; z-index: 2; right: 12px; bottom: 10px; left: 12px; display: flex; align-items: center; justify-content: space-between; gap: 8px; color: var(--proto-paper); }
.space-picture-overlay strong { min-width: 0; overflow: hidden; flex: 1 1 auto; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.space-picture-overlay span { min-width: 0; overflow: hidden; flex: 0 1 auto; color: rgba(241,242,237,.75); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.space-picture-tag-button { flex: 0 0 auto; height: 24px; padding-inline: 8px; border: 0; border-radius: 6px; background: rgba(246,247,242,.9); color: var(--proto-ink); font-size: 10px; }
.space-picture-tag-button:hover { background: var(--proto-acid); color: var(--proto-ink); }
.space-picture-empty { min-height: 240px; padding: 30px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.42); }
.space-pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.space-batch-tag-bar { position: sticky; z-index: 5; bottom: 16px; margin-top: 18px; padding: 12px 14px; display: flex; align-items: center; justify-content: space-between; gap: 14px; border: 1px solid rgba(17,20,22,.12); border-radius: 10px; background: rgba(246,247,242,.96); box-shadow: 0 8px 24px rgba(17,20,22,.12); }
.space-batch-tag-summary, .space-batch-tag-actions { display: flex; align-items: center; gap: 10px; }
.space-batch-tag-summary { color: var(--proto-muted); font-size: 12px; }
.space-batch-tag-summary strong { color: var(--proto-ink); }
.space-batch-tag-actions :deep(.ant-btn) { border-radius: 7px; font-size: 12px; }
.space-batch-tag-create-hint { margin: 0 0 14px; color: var(--proto-muted); font-size: 13px; }

@media (max-width: 1120px) {
  .space-gallery-layout { grid-template-columns: minmax(170px, .22fr) minmax(0, .78fr); gap: 18px; }
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
  .space-gallery-layout { grid-template-columns: 1fr; }
  .space-tag-sidebar { position: static; }
  .space-tag-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
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
  .space-gallery-actions .ghost-button { width: 100%; }
  .space-batch-tag-bar { align-items: stretch; flex-direction: column; }
  .space-batch-tag-summary, .space-batch-tag-actions { justify-content: space-between; flex-wrap: wrap; }
}
@media (max-width: 580px) {
  .space-empty { padding: 42px 16px 54px; gap: 22px; }
  .space-empty h1 { margin-top: 18px; font-size: 32px; }
  .space-empty-header p { margin-top: 10px; font-size: 14px; }
  .space-empty-card { padding: 24px 20px 24px; }
  .space-empty-illustration { margin-bottom: 16px; transform: scale(.9); transform-origin: center top; }
  .space-empty-benefits { grid-template-columns: 1fr; gap: 0; }
  .space-empty-benefit { min-height: 82px; padding: 16px 0; }
  .space-empty-benefit:not(:last-child) { border-right: 0; border-bottom: 1px solid rgba(17,20,22,.1); }
  .space-empty-divider { margin-top: 20px; }
  .space-empty-create { margin-top: 22px; }
  .space-heading-row { gap: 12px; }
  .space-heading-main { gap: 10px; }
  .space-heading-meta { gap: 5px; }
  .space-meta-tag { padding-inline: 7px; font-size: 10px; }
  .space-rename-button { padding-inline: 9px; }
  .space-usage-strip { grid-template-columns: 1fr; padding: 15px; }
  .usage-strip-intro, .usage-strip-meter, .usage-strip-stat, .usage-strip-privacy { grid-column: auto; }
  .usage-strip-stat { padding-top: 0; padding-bottom: 0; padding-left: 0; border-left: 0; }
  .space-picture-grid { grid-template-columns: 1fr; }
  .space-tag-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .space-status-tabs { overflow-x: auto; }
  .space-status-tab { flex: 0 0 auto; padding-inline: 9px; }
}
</style>
