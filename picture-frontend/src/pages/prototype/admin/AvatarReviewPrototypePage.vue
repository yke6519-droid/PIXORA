<template>
  <div class="avatar-review-prototype">
    <section class="avatar-review-heading">
      <div class="avatar-review-heading-copy">
        <h1 class="avatar-review-title">管理员头像审核</h1>
        <p class="avatar-review-description">
          审核用户提交的新头像，及时处理待审核请求并查看历史记录。
        </p>
      </div>
      <a-button
        class="proto-button ghost-button avatar-review-refresh"
        :loading="loading"
        :disabled="!authorized"
        @click="loadReviews"
      >
        刷新
      </a-button>
    </section>

    <a-spin v-if="authChecking" class="avatar-review-auth-loading" tip="正在确认管理员权限并加载审核数据..." />

    <a-result
      v-else-if="!authorized"
      status="403"
      title="暂时无法进入头像审核"
      :sub-title="accessError || '只有管理员可以审核头像。'"
    >
      <template #extra>
        <a-button class="proto-button ghost-button" @click="ensureAdmin">重新检查权限</a-button>
      </template>
    </a-result>

    <template v-else>
      <a-spin v-if="loading" class="avatar-review-auth-loading" tip="正在加载头像审核数据..." />

      <a-alert v-else-if="loadError" type="error" show-icon :message="loadError">
        <template #action>
          <a-button class="proto-button ghost-button" @click="loadReviews">重新加载</a-button>
        </template>
      </a-alert>

      <template v-else>
        <section class="avatar-review-kpis" aria-label="头像审核统计">
          <article
            v-for="tab in statusTabs"
            :key="`kpi-${tab.status}`"
            class="avatar-review-kpi proto-surface proto-rounded"
            :class="`kpi-${tab.tone}`"
          >
            <span class="avatar-review-kpi-icon" aria-hidden="true">{{ tab.icon }}</span>
            <div>
              <strong>{{ itemsByStatus(tab.status).length }}</strong>
              <h2>{{ tab.label }}</h2>
              <p>{{ tab.description }}</p>
            </div>
          </article>
        </section>

        <section class="avatar-review-workspace proto-surface proto-rounded" aria-label="头像审核列表">
          <nav class="avatar-review-tabs" aria-label="头像审核状态" role="tablist">
            <button
              v-for="tab in statusTabs"
              :key="tab.status"
              type="button"
              class="avatar-review-tab"
              :class="{ active: activeStatus === tab.status }"
              role="tab"
              :aria-selected="activeStatus === tab.status"
              @click="activeStatus = tab.status"
            >
              <span>{{ tab.label }}</span>
              <strong>{{ itemsByStatus(tab.status).length }}</strong>
            </button>
          </nav>

          <div class="avatar-review-workspace-heading">
            <div>
              <h2>{{ activeTab.label }}记录</h2>
              <p>{{ activeTab.description }}</p>
            </div>
            <span class="avatar-review-total">共 {{ activeReviews.length }} 条</span>
          </div>

          <div v-if="activeReviews.length" class="avatar-review-grid">
            <article
              v-for="item in activeReviews"
              :key="item.id"
              class="avatar-review-item"
              :class="`item-${statusClass(item.status)}`"
            >
              <header class="avatar-review-item-heading">
                <div class="avatar-review-avatar">
                  <img :src="item.currentAvatarUrl || item.avatarUrl" :alt="`${item.username}的当前头像`" loading="lazy" />
                </div>
                <div class="avatar-review-user">
                  <strong>{{ item.username }}</strong>
                  <span>@{{ item.useraccount }}</span>
                </div>
                <a-tag class="proto-status" :class="statusClass(item.status)">
                  {{ statusText(item.status) }}
                </a-tag>
              </header>

              <section class="avatar-review-visual" :aria-label="`${item.username}的${avatarLabel(item.status)}`">
                <div class="avatar-review-visual-heading">
                  <span>{{ avatarLabel(item.status) }}</span>
                  <span v-if="item.status === 0" class="avatar-review-visual-note">等待审核</span>
                </div>
                <div class="avatar-review-large-avatar">
                  <img :src="item.avatarUrl" :alt="`${item.username}提交的头像`" />
                </div>
              </section>

              <dl class="avatar-review-meta">
                <div>
                  <dt>提交时间</dt>
                  <dd>{{ item.submittedAt }}</dd>
                </div>
                <div v-if="item.reviewedAt">
                  <dt>审核时间</dt>
                  <dd>{{ item.reviewedAt }}</dd>
                </div>
              </dl>

              <div v-if="item.status === 2" class="avatar-review-message">
                <span>审核意见</span>
                <p>{{ item.checkMessage || '管理员未填写原因' }}</p>
              </div>

              <div v-if="item.status === 0" class="avatar-review-actions">
                <a-button
                  class="proto-button ghost-button danger-button"
                  :disabled="actionLoading"
                  @click="openReject(item)"
                >
                  拒绝
                </a-button>
                <a-button
                  class="proto-button acid-button"
                  type="primary"
                  :loading="actionLoading"
                  @click="approve(item)"
                >
                  通过审核
                </a-button>
              </div>

              <div v-else class="avatar-review-result" :class="`result-${statusClass(item.status)}`">
                <span>{{ item.status === 1 ? '审核结果已生效' : '已保留原头像' }}</span>
              </div>
            </article>
          </div>

          <a-empty v-else class="avatar-review-empty" :description="activeTab.emptyText">
            <template #image>
              <div class="avatar-review-empty-mark" aria-hidden="true">✓</div>
            </template>
          </a-empty>
        </section>
      </template>
    </template>

    <a-modal
      v-model:open="rejectOpen"
      title="拒绝头像审核"
      ok-text="确认拒绝"
      cancel-text="取消"
      :confirm-loading="actionLoading"
      @ok="submitReject"
    >
      <div v-if="rejectTarget" class="avatar-review-modal-user">
        <img :src="rejectTarget.avatarUrl" :alt="`${rejectTarget.username}的新头像`" />
        <div>
          <strong>{{ rejectTarget.username }}</strong>
          <span>@{{ rejectTarget.useraccount }} · 用户 #{{ rejectTarget.userId }}</span>
        </div>
      </div>
      <a-form layout="vertical" class="proto-form">
        <a-form-item label="拒绝原因" required>
          <a-textarea
            v-model:value="rejectReason"
            :rows="4"
            :maxlength="200"
            show-count
            placeholder="请输入清晰、可执行的审核拒绝原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { adminCheckAvatar, getCurrentUser, queryAvatarReviews } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import type { AvatarReviewItem, AvatarReviewStatus } from './avatarReviewModel'

/** 只转换页面文案，后端仍然沿用 0/1/2 三个状态值。 */
const statusTabs: Array<{
  status: AvatarReviewStatus
  label: string
  description: string
  emptyText: string
  icon: string
  tone: 'pending' | 'pass' | 'refuse'
}> = [
  { status: 0, label: '待审核', description: '需要处理', emptyText: '暂无待审核头像', icon: '!', tone: 'pending' },
  { status: 1, label: '已通过', description: '头像已生效', emptyText: '暂无已通过记录', icon: '✓', tone: 'pass' },
  { status: 2, label: '已拒绝', description: '保留原头像', emptyText: '暂无拒绝记录', icon: '—', tone: 'refuse' },
]

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const authChecking = ref(true)
const authorized = ref(false)
const accessError = ref('')
const loading = ref(false)
const loadError = ref('')
const actionLoading = ref(false)
const reviews = ref<AvatarReviewItem[]>([])
const activeStatus = ref<AvatarReviewStatus>(0)
const rejectOpen = ref(false)
const rejectTarget = ref<AvatarReviewItem | null>(null)
const rejectReason = ref('')

// KPI、Tabs 和当前列表共用同一份真实接口数据，避免三个区域的数量出现偏差。
const activeReviews = computed(() => itemsByStatus(activeStatus.value))
const activeTab = computed(() => statusTabs.find((tab) => tab.status === activeStatus.value) || statusTabs[0])

function itemsByStatus(status: AvatarReviewStatus) {
  return reviews.value.filter((item) => item.status === status)
}

function normalizeId(id?: number | string) {
  return id == null ? '' : String(id)
}

function formatDate(value?: string | number) {
  if (value == null || value === '') return '未记录时间'
  return String(value).replace('T', ' ').slice(0, 16)
}

/** 将真实接口字段转换为页面内部模型，同时兼容历史数据缺字段的情况。 */
function normalizeAvatarReview(item: API.AvatarReviewVO): AvatarReviewItem {
  const userId = normalizeId(item.userId) || 'unknown'
  const submittedAt = formatDate(item.submittedAt)
  return {
    id: normalizeId(item.id) || `${userId}-${submittedAt}`,
    userId,
    username: item.username || '未知用户',
    useraccount: item.useraccount || userId,
    avatarUrl: item.avatarUrl || '',
    ...(item.currentAvatarUrl ? { currentAvatarUrl: item.currentAvatarUrl } : {}),
    status: item.status === 1 ? 1 : item.status === 2 ? 2 : 0,
    submittedAt,
    ...(item.reviewedAt ? { reviewedAt: formatDate(item.reviewedAt) } : {}),
    ...(item.checkMessage ? { checkMessage: item.checkMessage } : {}),
  }
}

function statusText(status: AvatarReviewStatus) {
  return statusTabs.find((tab) => tab.status === status)?.label || '待审核'
}

function statusClass(status: AvatarReviewStatus) {
  return status === 0 ? 'wait' : status === 1 ? 'pass' : 'refuse'
}

function avatarLabel(status: AvatarReviewStatus) {
  return status === 1 ? '审核头像' : '申请头像'
}

/** 管理页面仍然以后端 Session 做管理员校验，列表和审核操作均走真实接口。 */
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
      accessError.value = '当前账号不是管理员，无法审核头像。'
      authorized.value = false
      return false
    }
    authorized.value = true
    await loadReviews()
    return true
  } catch (error: any) {
    authorized.value = false
    accessError.value = error?.response?.data?.message || error?.message || '管理员权限检查失败'
    return false
  } finally {
    authChecking.value = false
  }
}

async function loadReviews() {
  if (!authorized.value) return false
  loading.value = true
  loadError.value = ''
  try {
    const res = await queryAvatarReviews()
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '头像审核列表加载失败')
    }
    reviews.value = (res.data.data || []).map(normalizeAvatarReview)
    return true
  } catch (error: any) {
    reviews.value = []
    loadError.value = error?.response?.data?.message || error?.message || '头像审核列表加载失败'
    return false
  } finally {
    loading.value = false
  }
}

async function submitDecision(item: AvatarReviewItem, decision: 1 | 2, reason = '') {
  if (decision === 2 && !reason.trim()) {
    message.warning('请填写审核拒绝原因')
    return false
  }

  actionLoading.value = true
  try {
    const res = await adminCheckAvatar({
      userId: item.userId,
      checkResult: decision,
      ...(decision === 2 ? { checkMessage: reason.trim() } : {}),
    })
    if (res.data?.code !== 200 || res.data.data === false) {
      throw new Error(res.data?.message || '头像审核提交失败')
    }
    message.success(decision === 1 ? `${item.username} 的头像已通过` : `${item.username} 的头像已拒绝`)
    await loadReviews()
    return true
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '头像审核提交失败')
    return false
  } finally {
    actionLoading.value = false
  }
}

function approve(item: AvatarReviewItem) {
  Modal.confirm({
    title: '确认通过这个头像？',
    content: `通过后，${item.username} 的新头像会进入审核通过区域。`,
    okText: '确认通过',
    cancelText: '取消',
    onOk: () => submitDecision(item, 1),
  })
}

function openReject(item: AvatarReviewItem) {
  rejectTarget.value = item
  rejectReason.value = ''
  rejectOpen.value = true
}

function closeReject() {
  rejectOpen.value = false
  rejectTarget.value = null
  rejectReason.value = ''
}

async function submitReject() {
  if (!rejectTarget.value) return
  if (!rejectReason.value.trim()) {
    message.warning('请填写审核拒绝原因')
    return
  }
  const success = await submitDecision(rejectTarget.value, 2, rejectReason.value)
  if (success) closeReject()
}

onMounted(() => {
  void ensureAdmin()
})
</script>

<style scoped>
.avatar-review-prototype { color: var(--proto-ink); font-family: 'Manrope', 'Geist', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.avatar-review-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding-top: clamp(16px, 2vw, 24px); padding-bottom: 18px; border-bottom: 1px solid var(--proto-line); }
.avatar-review-heading-copy { min-width: 0; }
.avatar-review-title { margin: 0 0 8px; color: var(--proto-ink); font-size: clamp(26px, 3vw, 34px); font-weight: 800; letter-spacing: -.055em; line-height: 1.08; }
.avatar-review-description { max-width: 58ch; color: var(--proto-muted); font-size: 13px; line-height: 1.6; }
.avatar-review-refresh { min-width: 72px; }
.avatar-review-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }

.avatar-review-kpis { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; padding: 18px 0; }
.avatar-review-kpi { display: flex; align-items: flex-start; gap: 14px; min-width: 0; min-height: 126px; padding: 18px; background: rgba(255, 255, 255, .72); box-shadow: 0 8px 26px rgba(18, 23, 23, .06); }
.avatar-review-kpi-icon { display: grid; width: 42px; height: 42px; flex: 0 0 42px; place-items: center; border-radius: 11px; font-family: 'DM Mono', monospace; font-size: 21px; font-weight: 700; }
.kpi-pending .avatar-review-kpi-icon { background: rgba(255, 137, 106, .15); color: #b55435; }
.kpi-pass .avatar-review-kpi-icon { background: rgba(186, 255, 61, .2); color: #5f8d0b; }
.kpi-refuse .avatar-review-kpi-icon { background: rgba(17, 20, 22, .07); color: var(--proto-muted); }
.avatar-review-kpi strong { display: block; font-family: 'DM Mono', monospace; font-size: 30px; line-height: 1; letter-spacing: -.07em; }
.avatar-review-kpi h2 { margin: 8px 0 3px; font-size: 14px; font-weight: 800; letter-spacing: -.03em; }
.avatar-review-kpi p { color: var(--proto-muted); font-size: 11px; line-height: 1.5; }

.avatar-review-workspace { overflow: hidden; background: rgba(255, 255, 255, .68); box-shadow: var(--proto-shadow); }
.avatar-review-tabs { display: flex; gap: 4px; padding: 8px; border-bottom: 1px solid var(--proto-line); background: rgba(241, 242, 237, .62); }
.avatar-review-tab { display: inline-flex; align-items: center; gap: 8px; min-height: 38px; padding: 0 14px; border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--proto-muted); cursor: pointer; font: inherit; font-size: 13px; font-weight: 700; transition: background-color .2s ease, color .2s ease, border-color .2s ease; }
.avatar-review-tab:hover { color: var(--proto-ink); background: rgba(255, 255, 255, .7); }
.avatar-review-tab.active { border-color: rgba(95, 141, 11, .18); background: rgba(186, 255, 61, .22); color: var(--proto-ink); }
.avatar-review-tab strong { color: currentColor; font-family: 'DM Mono', monospace; font-size: 12px; }
.avatar-review-workspace-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; padding: 20px 20px 14px; }
.avatar-review-workspace-heading h2 { margin: 0; font-size: 20px; font-weight: 800; letter-spacing: -.05em; }
.avatar-review-workspace-heading p { margin-top: 5px; color: var(--proto-muted); font-size: 12px; }
.avatar-review-total { color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 11px; }

.avatar-review-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; padding: 0 20px 20px; }
.avatar-review-item { min-width: 0; padding: 16px; border: 1px solid var(--proto-line); border-top: 2px solid var(--proto-ink); border-radius: 10px; background: rgba(255, 255, 255, .78); transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.avatar-review-item.item-wait { border-top-color: var(--proto-orange); }
.avatar-review-item.item-pass { border-top-color: #8dbb22; }
.avatar-review-item.item-refuse { border-top-color: #8b9494; }
.avatar-review-item:hover { border-color: rgba(17, 20, 22, .28); box-shadow: 0 10px 26px rgba(18, 23, 23, .07); transform: translateY(-1px); }
.avatar-review-item-heading { display: flex; align-items: center; gap: 11px; min-width: 0; }
.avatar-review-avatar { width: 46px; height: 46px; flex: 0 0 46px; overflow: hidden; border: 1px solid var(--proto-line); border-radius: 50%; background: var(--proto-paper-deep); }
.avatar-review-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; }
.avatar-review-user { min-width: 0; flex: 1; }
.avatar-review-user strong, .avatar-review-user span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.avatar-review-user strong { color: var(--proto-ink); font-size: 14px; font-weight: 800; }
.avatar-review-user span { margin-top: 4px; color: var(--proto-muted); font-size: 11px; }
.avatar-review-item-heading :deep(.proto-status) { flex: 0 0 auto; }

.avatar-review-visual { margin-top: 16px; padding: 12px; border: 1px solid var(--proto-line); border-radius: 8px; background: var(--proto-paper); }
.avatar-review-visual-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: var(--proto-ink); font-size: 11px; font-weight: 800; }
.avatar-review-visual-note { color: var(--proto-muted); font-size: 10px; font-weight: 500; }
.avatar-review-large-avatar { width: 112px; height: 112px; margin: 12px auto 2px; overflow: hidden; border: 2px solid rgba(255, 255, 255, .9); border-radius: 50%; background: var(--proto-paper-deep); box-shadow: 0 6px 16px rgba(18, 23, 23, .12); }
.avatar-review-large-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; }
.avatar-review-meta { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin: 14px 0 0; }
.avatar-review-meta div { min-width: 0; }
.avatar-review-meta dt { color: var(--proto-muted); font-size: 10px; }
.avatar-review-meta dd { margin: 4px 0 0; color: var(--proto-ink); font-size: 11px; font-weight: 700; overflow-wrap: anywhere; }
.avatar-review-message { margin-top: 14px; padding: 10px 11px; border: 1px solid rgba(255, 137, 106, .28); border-radius: 7px; background: rgba(255, 137, 106, .09); }
.avatar-review-message span { display: block; color: #973816; font-size: 11px; font-weight: 800; }
.avatar-review-message p { margin-top: 5px; color: var(--proto-muted); font-size: 11px; line-height: 1.5; overflow-wrap: anywhere; }
.avatar-review-actions { display: flex; gap: 9px; margin-top: 16px; }
.avatar-review-actions .proto-button { flex: 1; }
.danger-button:not(:disabled) { color: #973816; border-color: rgba(151, 56, 22, .45); }
.danger-button:not(:disabled):hover { color: #973816 !important; border-color: var(--proto-orange) !important; }
.avatar-review-result { display: flex; align-items: center; gap: 8px; margin-top: 15px; padding-top: 11px; border-top: 1px solid var(--proto-line); color: var(--proto-muted); font-size: 11px; font-weight: 700; }
.result-pass { color: #5f8d0b; }
.result-refuse { color: #697171; }
.avatar-review-empty { margin: 0; padding: 32px 16px 38px; }
.avatar-review-empty-mark { display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid var(--proto-line); border-radius: 50%; color: #5f8d0b; font-size: 18px; font-weight: 700; }
.avatar-review-modal-user { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.avatar-review-modal-user img { width: 52px; height: 52px; border-radius: 50%; object-fit: cover; }
.avatar-review-modal-user strong, .avatar-review-modal-user span { display: block; }
.avatar-review-modal-user strong { font-size: 15px; }
.avatar-review-modal-user span { margin-top: 5px; color: var(--proto-muted); font-size: 11px; }

@media (max-width: 820px) {
  .avatar-review-kpis, .avatar-review-grid { grid-template-columns: 1fr; }
}

@media (max-width: 620px) {
  .avatar-review-heading { align-items: flex-start; flex-direction: column; }
  .avatar-review-refresh { align-self: flex-end; }
  .avatar-review-tabs { overflow-x: auto; }
  .avatar-review-tab { flex: 0 0 auto; }
  .avatar-review-workspace-heading { align-items: flex-start; flex-direction: column; }
  .avatar-review-grid { padding-inline: 12px; }
}

@media (prefers-reduced-motion: reduce) {
  .avatar-review-tab, .avatar-review-item { transition: none; }
}
</style>
