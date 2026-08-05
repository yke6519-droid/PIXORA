<template>
  <div class="space-admin-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">空间运营 / querySpacePage + alterLevelById</span>
        <h1 class="proto-title">空间不是黑盒，<br />容量与等级都可读。</h1>
        <p class="proto-copy">
          管理员通过真实空间分页接口查看持有人、容量和图片数量，并使用 <span class="proto-mono">alterLevelById</span> 调整空间等级。
        </p>
      </div>
      <div class="space-admin-summary"><span>空间总数</span><strong>{{ total }}</strong><small>querySpacePage</small></div>
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
          <span>当前页图片数量</span>
          <strong>{{ currentPictureCount }}</strong>
          <small>usedCount</small>
        </div>
        <div class="admin-metric proto-surface">
          <span>当前页容量占用</span>
          <strong>{{ formatSize(currentUsedSize) }}</strong>
          <small>usedSize</small>
        </div>
        <div class="admin-metric proto-surface">
          <span>需要关注</span>
          <strong>{{ attentionCount }}</strong>
          <small>容量使用率 ≥ 75%</small>
        </div>
      </section>

      <section class="space-admin-table proto-section">
        <div class="space-admin-table-head">
          <div>
            <span class="proto-eyebrow">spaces / real data</span>
            <h2 class="proto-subtitle">私人空间清单</h2>
          </div>
          <a-select v-model:value="levelFilter" style="width: 140px" @change="changeLevel">
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
              <span class="space-id">#{{ normalizeId(space.id) }}</span>
              <strong>{{ space.spaceName || '未命名空间' }}</strong>
              <small>{{ holderName(space) }} · {{ formatDate(space.createTime) }}</small>
            </div>
            <div class="space-admin-usage">
              <div class="usage-bar"><i :style="{ width: `${usagePercent(space.usedSize, space.maxSize)}%` }"></i></div>
              <span>{{ formatSize(space.usedSize) }} / {{ formatSize(space.maxSize) }}</span>
            </div>
            <div class="space-admin-level">
              <a-tag class="proto-tag acid-tag">{{ levelText(space.spaceLevel) }}</a-tag>
              <small>level {{ space.spaceLevel ?? '—' }}</small>
            </div>
            <div class="space-admin-count">
              <strong>{{ toNumber(space.usedCount) }}</strong>
              <span>/ {{ toNumber(space.maxCount) }} 张</span>
            </div>
            <a-button class="proto-button ghost-button" @click="openLevel(space)">调整等级</a-button>
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
        <a-form-item label="spaceId">
          <a-input :value="normalizeId(selectedSpace?.id)" disabled />
        </a-form-item>
        <a-form-item label="alterLevel">
          <a-select v-model:value="selectedLevel" style="width: 100%">
            <a-select-option :value="0">0 / 基础空间 · 100MB · 50张</a-select-option>
            <a-select-option :value="1">1 / 专业空间 · 500MB · 100张</a-select-option>
            <a-select-option :value="2">2 / 专家空间 · 1000MB · 200张</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { alterLevelByIdUsingPut, querySpacePageUsingGet } from '../../../api/spaceController'
import { getCurrentUserUsingGet } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import { formatSpaceLevel } from '../prototypeData'

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
const total = ref(0)
const current = ref(1)
const pageSize = 10
const levelFilter = ref<LevelFilter>('all')
const levelOpen = ref(false)
const selectedSpace = ref<API.SpaceVO | null>(null)
const selectedLevel = ref<SpaceLevel>(0)

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
  return space.createdUser?.username || space.createdUser?.useraccount || `用户 #${normalizeId(space.userId) || '未知'}`
}

function levelText(level?: number) {
  return formatSpaceLevel(level ?? 0)
}

async function ensureAdmin() {
  authChecking.value = true
  accessError.value = ''
  try {
    const res = await getCurrentUserUsingGet()
    if (res.data?.code === 40100 || !res.data?.data) {
      loginUserStore.clearLoginUser()
      await router.replace({ path: '/prototype/user/login', query: { redirect: route.fullPath } })
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
    const res = await querySpacePageUsingGet({
      current: current.value,
      pageSize,
      spaceLevel: levelFilter.value === 'all' ? undefined : levelFilter.value,
      sortFiled: 'createTime',
      sortOrder: 'descend',
    })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '空间列表加载失败')
    spaces.value = res.data.data?.spaceVOList || []
    total.value = toNumber(res.data.data?.total)
  } catch (error: any) {
    spaces.value = []
    total.value = 0
    loadError.value = error?.response?.data?.message || error?.message || '空间列表加载失败'
  } finally {
    loading.value = false
  }
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

async function saveLevel() {
  if (!selectedSpace.value?.id) return
  actionLoading.value = true
  try {
    const res = await alterLevelByIdUsingPut({
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
.space-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.space-alert { margin-bottom: 18px; }
.space-admin-summary { min-width: 160px; padding: 18px; background: var(--proto-blue); }
.space-admin-summary span, .space-admin-summary strong, .space-admin-summary small { display: block; }
.space-admin-summary span { font-family: 'DM Mono', monospace; font-size: 10px; }
.space-admin-summary strong { margin-top: 20px; font-size: 55px; line-height: .8; letter-spacing: -.1em; }
.space-admin-summary small { margin-top: 15px; font-family: 'DM Mono', monospace; font-size: 9px; opacity: .52; }
.space-admin-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.admin-metric { min-height: 122px; padding: 18px; display: flex; flex-direction: column; justify-content: space-between; }
.admin-metric span { color: var(--proto-muted); font-size: 11px; }
.admin-metric strong { font-size: 35px; letter-spacing: -.08em; }
.admin-metric small { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 9px; }
.space-admin-table-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 15px; margin-bottom: 25px; }
.space-admin-table-head h2 { margin-top: 9px; }
.space-admin-loading { padding: 22px; }
.space-admin-list { border-top: 2px solid var(--proto-ink); }
.space-admin-row { min-height: 111px; padding: 17px 0; display: grid; grid-template-columns: 1.2fr 1fr .7fr .6fr 118px; gap: 18px; align-items: center; border-bottom: 1px solid var(--proto-line); }
.space-id { display: block; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; }
.space-admin-name strong, .space-admin-name small { display: block; }
.space-admin-name strong { margin-top: 7px; font-size: 16px; letter-spacing: -.04em; }
.space-admin-name small { margin-top: 6px; color: var(--proto-muted); font-size: 10px; }
.space-admin-usage span { display: block; margin-top: 7px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.usage-bar { height: 5px; overflow: hidden; background: var(--proto-paper-deep); }
.usage-bar i { display: block; height: 100%; background: var(--proto-acid); }
.space-admin-level small { display: block; margin-top: 5px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.space-admin-count strong { font-size: 25px; letter-spacing: -.08em; }
.space-admin-count span { color: var(--proto-muted); font-size: 10px; }
.space-admin-pagination { display: flex; justify-content: flex-end; padding-top: 25px; }
.space-admin-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.space-admin-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }
@media (max-width: 950px) { .space-admin-row { grid-template-columns: 1fr 1fr .7fr 95px; } .space-admin-row > .proto-button { grid-column: 4; grid-row: 1 / span 2; } }
@media (max-width: 650px) { .space-admin-metrics { grid-template-columns: 1fr; } .space-admin-table-head { align-items: flex-start; flex-direction: column; } .space-admin-row { grid-template-columns: 1fr 1fr; gap: 12px; } .space-admin-row > .proto-button { grid-column: 2; grid-row: auto; } }
</style>
