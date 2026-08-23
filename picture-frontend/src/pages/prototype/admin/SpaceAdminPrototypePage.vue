<template>
  <div class="space-admin-prototype">
    <section class="proto-page-head space-admin-page-head">
      <div>
        <h1 class="proto-title">空间运营</h1>
      </div>
      <div class="space-admin-summary">
        <span>空间总数</span>
        <strong>{{ total }}</strong>
        <small>私人空间</small>
      </div>
    </section>

    <a-spin v-if="authChecking" class="space-auth-loading" tip="正在确认管理员权限..." />

    <a-result
      v-else-if="!authorized"
      status="403"
      title="暂时无法进入空间运营"
      :sub-title="accessError || '只有管理员可以查看空间运营数据。'"
    >
      <template #extra>
        <a-button class="proto-button ghost-button" @click="ensureAdmin">重新检查权限</a-button>
      </template>
    </a-result>

    <template v-else>
      <a-alert
        v-if="loadError"
        class="space-alert"
        type="error"
        show-icon
        :message="loadError"
        description="请确认后端服务和当前登录会话正常后重试。"
        closable
        @close="loadError = ''"
      />

      <section class="space-admin-metrics proto-section">
        <div class="admin-metric proto-surface">
          <span>本页图片数</span>
          <strong>{{ currentPictureCount }}</strong>
          <small>分页合计</small>
        </div>
        <div class="admin-metric proto-surface">
          <span>本页占用空间</span>
          <strong>{{ formatSize(currentUsedSize) }}</strong>
          <small>容量合计</small>
        </div>
        <div class="admin-metric proto-surface">
          <span>高使用率空间</span>
          <strong>{{ attentionCount }}</strong>
          <small>使用率 ≥ 75%</small>
        </div>
      </section>

      <section class="space-admin-table proto-section">
        <div class="space-admin-table-head">
          <div>
            <h2 class="proto-subtitle">私人空间</h2>
            <span class="space-admin-table-count">共 {{ total }} 个空间</span>
          </div>
          <a-select v-model:value="levelFilter" class="space-admin-filter" @change="changeLevel">
            <a-select-option value="all">全部等级</a-select-option>
            <a-select-option :value="0">基础空间</a-select-option>
            <a-select-option :value="1">专业空间</a-select-option>
            <a-select-option :value="2">专家空间</a-select-option>
          </a-select>
        </div>

        <div v-if="loading" class="space-admin-loading proto-surface">
          <a-skeleton active :paragraph="{ rows: 7 }" />
        </div>

        <div v-else-if="spaces.length" class="space-admin-list">
          <article v-for="space in spaces" :key="normalizeId(space.id)" class="space-admin-row">
            <div class="space-admin-name">
              <strong>{{ space.spaceName || '未命名空间' }}</strong>
              <small>{{ holderName(space) }} · {{ formatDate(space.createTime) }}</small>
            </div>
            <div class="space-admin-usage">
              <span class="space-admin-field-label">容量使用</span>
              <div class="usage-bar"><i :style="{ width: `${usagePercent(space.usedSize, space.maxSize)}%` }"></i></div>
              <span>{{ formatSize(space.usedSize) }} / {{ formatSize(space.maxSize) }}</span>
            </div>
            <div class="space-admin-level">
              <a-tag class="proto-tag acid-tag">{{ levelText(space.spaceLevel) }}</a-tag>
              <small>空间等级</small>
            </div>
            <div class="space-admin-count">
              <strong>{{ toNumber(space.usedCount) }}</strong>
              <span>/ {{ toNumber(space.maxCount) }} 张</span>
            </div>
            <div class="space-admin-actions">
              <a-button class="proto-button ghost-button" @click="openTagManage(space)">管理标签</a-button>
              <a-button class="proto-button ghost-button" @click="openLevel(space)">调整等级</a-button>
            </div>
            <div class="space-admin-tags" aria-label="空间标签">
              <span class="space-admin-tags-label">空间标签</span>
              <a-tag
                v-for="tag in getSpaceTags(space.id)"
                :key="normalizeId(tag.id)"
                class="space-admin-tag"
                :class="tag.status === 1 ? 'is-active' : 'is-disabled'"
              >
                {{ tag.tagName }}
              </a-tag>
              <span v-if="!getSpaceTags(space.id).length" class="space-admin-no-tags">暂无标签</span>
            </div>
          </article>
        </div>

        <a-empty v-else description="当前筛选下没有空间">
          <template #footer>
            <a-button class="proto-button ghost-button" @click="resetLevel">查看全部空间</a-button>
          </template>
        </a-empty>

        <div v-if="!loading && total > 0" class="space-admin-pagination">
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
    </template>

    <a-modal
      v-model:open="levelOpen"
      title="调整空间等级"
      ok-text="保存等级"
      cancel-text="取消"
      :confirm-loading="actionLoading"
      @ok="saveLevel"
    >
      <a-form layout="vertical" class="proto-form">
        <a-form-item label="空间编号">
          <a-input :value="normalizeId(selectedSpace?.id)" disabled />
        </a-form-item>
        <a-form-item label="空间等级">
          <a-select v-model:value="selectedLevel" style="width: 100%">
            <a-select-option :value="0">基础空间 · 100MB · 50 张</a-select-option>
            <a-select-option :value="1">专业空间 · 500MB · 100 张</a-select-option>
            <a-select-option :value="2">专家空间 · 1000MB · 200 张</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <SpaceTagManageModal
      v-if="selectedTagSpace"
      v-model:open="tagManageOpen"
      :space-id="selectedTagSpace.id!"
      :max-tag-count="maxTagCount(selectedTagSpace.spaceLevel)"
      admin-mode
      @changed="handleTagManageChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { alterLevelById, querySpacePage } from '../../../api/spaceController'
import { listManageTag } from '../../../api/tagController'
import { getCurrentUser } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { formatSpaceLevel } from '../prototypeData'
import SpaceTagManageModal from '../space/components/SpaceTagManageModal.vue'

type SpaceLevel = 0 | 1 | 2
type LevelFilter = 'all' | SpaceLevel

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const authChecking = ref(true)
const authorized = ref(false)
const accessError = ref('')
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const spaces = ref<API.SpaceVO[]>([])
const tagsBySpaceId = ref<Record<string, API.Tag[]>>({})
const total = ref(0)
const current = ref(1)
const pageSize = 10
const levelFilter = ref<LevelFilter>('all')
const levelOpen = ref(false)
const selectedSpace = ref<API.SpaceVO | null>(null)
const selectedLevel = ref<SpaceLevel>(0)
const tagManageOpen = ref(false)
const selectedTagSpace = ref<API.SpaceVO | null>(null)

const currentPictureCount = computed(() => spaces.value.reduce((sum, space) => sum + toNumber(space.usedCount), 0))
const currentUsedSize = computed(() => spaces.value.reduce((sum, space) => sum + toNumber(space.usedSize), 0))
const attentionCount = computed(() => spaces.value.filter((space) => usagePercent(space.usedSize, space.maxSize) >= 75).length)

function normalizeId(id?: number | string | null) {
  return id == null ? '' : String(id)
}

function toNumber(value?: number | string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatSize(value?: number | string) {
  const bytes = toNumber(value)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function usagePercent(used?: number | string, max?: number | string) {
  const maximum = toNumber(max)
  if (!maximum) return 0
  return Math.min(100, Math.round((toNumber(used) / maximum) * 100))
}

function formatDate(value?: string) {
  return value ? String(value).replace('T', ' ').replace(/\.\d+.*$/, '') : '未记录时间'
}

function holderName(space: API.SpaceVO) {
  return space.createdUser?.username || `用户 #${normalizeId(space.userId) || '未知'}`
}

function levelText(level?: number) {
  return formatSpaceLevel(level ?? 0)
}

function maxTagCount(level?: number) {
  if ((level ?? 0) >= 2) return 30
  if (level === 1) return 20
  return 10
}

function getSpaceTags(spaceId?: number | string) {
  return tagsBySpaceId.value[normalizeId(spaceId)] || []
}

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
      accessError.value = '当前账号不是管理员，无法查看空间运营数据。'
      authorized.value = false
      return false
    }
    authorized.value = true
    await loadSpaces()
    return true
  } catch (error: any) {
    authorized.value = false
    accessError.value = error?.response?.data?.message || error?.message || '管理员权限检查失败'
    return false
  } finally {
    authChecking.value = false
  }
}

async function loadSpaces() {
  if (!authorized.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await querySpacePage({
      spaceQueryRequest: {
        current: current.value,
        pageSize,
        spaceLevel: levelFilter.value === 'all' ? undefined : levelFilter.value,
        sortFiled: 'createTime',
        sortOrder: 'descend',
      },
    })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '空间列表加载失败')
    spaces.value = res.data.data?.spaceVOList || []
    total.value = toNumber(res.data.data?.total)
    await loadSpaceTags(spaces.value)
  } catch (error: any) {
    spaces.value = []
    total.value = 0
    loadError.value = error?.response?.data?.message || error?.message || '空间列表加载失败'
  } finally {
    loading.value = false
  }
}

/** 管理员列表直接展示当前页空间的全部标签，包括已停用标签。 */
async function loadSpaceTags(spaceList: API.SpaceVO[]) {
  const nextTags: Record<string, API.Tag[]> = {}
  await Promise.all(
    spaceList.map(async (space) => {
      const spaceId = normalizeId(space.id)
      if (!spaceId) return
      try {
        const res = await listManageTag({ spaceId })
        if (res.data?.code === 200) {
          nextTags[spaceId] = res.data.data || []
        }
      } catch {
        // 标签加载失败不影响管理员查看空间基础运营数据。
        nextTags[spaceId] = []
      }
    }),
  )
  tagsBySpaceId.value = nextTags
}

async function changeLevel() {
  current.value = 1
  await loadSpaces()
}

async function resetLevel() {
  levelFilter.value = 'all'
  current.value = 1
  await loadSpaces()
}

async function handlePageChange(page: number) {
  current.value = page
  await loadSpaces()
}

function openLevel(space: API.SpaceVO) {
  selectedSpace.value = space
  selectedLevel.value = (space.spaceLevel ?? 0) as SpaceLevel
  levelOpen.value = true
}

function openTagManage(space: API.SpaceVO) {
  selectedTagSpace.value = space
  tagManageOpen.value = true
}

/** 标签状态变化后，只刷新当前空间的标签，列表中的颜色会立即同步。 */
async function handleTagManageChanged() {
  if (!selectedTagSpace.value) return
  await loadSpaceTags([selectedTagSpace.value])
}

async function saveLevel() {
  if (!selectedSpace.value?.id) return
  actionLoading.value = true
  try {
    const res = await alterLevelById({
      spaceId: selectedSpace.value.id,
      alterLevel: selectedLevel.value,
    })
    if (res.data?.code !== 200 || res.data.data === false) throw new Error(res.data?.message || '空间等级调整失败')
    message.success('空间等级已更新')
    levelOpen.value = false
    await loadSpaces()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '空间等级调整失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  void ensureAdmin()
})
</script>

<style scoped>
.space-admin-page-head { padding-top: 14px; }
.space-admin-page-head .proto-title { margin: 0; font-size: clamp(36px, 4vw, 54px); }
.space-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.space-alert { margin-bottom: 12px; }
.space-admin-summary { min-width: 154px; padding: 14px 17px; border-radius: 7px; background: var(--proto-ink); color: var(--proto-paper); }
.space-admin-summary span, .space-admin-summary strong, .space-admin-summary small { display: block; }
.space-admin-summary span { color: rgba(255,255,255,.72); font-size: 11px; }
.space-admin-summary strong { margin-top: 10px; font-size: 43px; line-height: .9; letter-spacing: -.08em; }
.space-admin-summary small { margin-top: 9px; color: rgba(255,255,255,.56); font-size: 10px; }
.space-admin-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding-top: 18px; }
.admin-metric { min-height: 102px; padding: 15px 17px; display: flex; flex-direction: column; justify-content: space-between; border-radius: 7px; }
.admin-metric span { color: var(--proto-muted); font-size: 12px; }
.admin-metric strong { font-size: 34px; line-height: 1; letter-spacing: -.08em; }
.admin-metric small { color: var(--proto-orange); font-size: 10px; }
.space-admin-table-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 15px; margin-bottom: 14px; }
.space-admin-table-head h2 { margin: 0 0 5px; font-size: clamp(24px, 3vw, 36px); }
.space-admin-table-count { color: var(--proto-muted); font-size: 12px; }
.space-admin-filter { width: 148px; }
.space-admin-loading { padding: 22px; }
.space-admin-list { border-top: 2px solid var(--proto-ink); }
.space-admin-row { min-height: 96px; padding: 14px 0; display: grid; grid-template-columns: 1.2fr 1.05fr .75fr .55fr 190px; gap: 16px; align-items: center; border-bottom: 1px solid var(--proto-line); }
.space-admin-name strong, .space-admin-name small { display: block; }
.space-admin-name strong { font-size: 16px; letter-spacing: -.04em; }
.space-admin-name small { margin-top: 6px; color: var(--proto-muted); font-size: 11px; }
.space-admin-field-label { display: block; margin-bottom: 7px; color: var(--proto-muted); font-size: 11px; }
.space-admin-usage > span:last-child { display: block; margin-top: 7px; color: var(--proto-muted); font-size: 10px; }
.usage-bar { height: 6px; overflow: hidden; background: var(--proto-paper-deep); border-radius: 99px; }
.usage-bar i { display: block; height: 100%; background: var(--proto-acid); }
.space-admin-level small { display: block; margin-top: 4px; color: var(--proto-muted); font-size: 10px; }
.space-admin-count strong { font-size: 24px; line-height: 1; letter-spacing: -.08em; }
.space-admin-count span { color: var(--proto-muted); font-size: 11px; }
.space-admin-actions { display: flex; flex-wrap: wrap; gap: 6px; justify-content: flex-end; }
.space-admin-tags { grid-column: 1 / -1; min-width: 0; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; padding-top: 10px; border-top: 1px solid rgba(17,20,22,.08); }
.space-admin-tags-label { margin-right: 3px; color: var(--proto-muted); font-size: 11px; }
.space-admin-tag.ant-tag { margin: 0; border: 1px solid transparent; border-radius: 4px; font-size: 11px; line-height: 22px; }
.space-admin-tag.is-active.ant-tag { border-color: rgba(76,126,171,.34); background: var(--proto-blue); color: #173754; }
.space-admin-tag.is-disabled.ant-tag { border-color: rgba(199,55,61,.45); background: rgba(199,55,61,.13); color: #a52e35; }
.space-admin-no-tags { color: var(--proto-muted); font-size: 11px; }
.space-admin-pagination { display: flex; justify-content: flex-end; padding-top: 18px; }
.space-admin-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.space-admin-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }
@media (max-width: 950px) { .space-admin-row { grid-template-columns: 1.15fr 1fr .75fr 180px; } .space-admin-actions { grid-column: 4; grid-row: 1 / span 2; } }
@media (max-width: 650px) { .space-admin-metrics { grid-template-columns: 1fr; } .space-admin-table-head { align-items: flex-start; flex-direction: column; } .space-admin-filter { width: 100%; } .space-admin-row { grid-template-columns: 1fr 1fr; gap: 12px; } .space-admin-actions { grid-column: 2; grid-row: auto; justify-content: flex-start; } }
</style>
