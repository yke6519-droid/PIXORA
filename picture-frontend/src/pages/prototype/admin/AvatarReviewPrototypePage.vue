<template>
  <div class="avatar-review-prototype">
    <section class="avatar-review-heading">
      <div class="avatar-review-heading-copy">
        <h1 class="avatar-review-title">管理员头像审核</h1>
        <p class="avatar-review-description">
          按头像审核状态查看用户提交的新头像，待审核记录优先处理。
        </p>
      </div>

      <div class="avatar-review-heading-actions">
        <div class="avatar-review-counter" aria-label="待审核头像数量">
          <span>待处理头像</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <a-button class="proto-button ghost-button" :loading="loading" :disabled="!authorized" @click="loadReviews">
          刷新数据
        </a-button>
      </div>
    </section>

    <a-spin v-if="authChecking" class="avatar-review-auth-loading" tip="正在确认管理员权限并加载审核数据..." />

    <a-result
      v-else-if="!authorized"
      status="403"
      title="暂时无法进入头像审核"
      :sub-title="accessError || '只有管理员可以审核头像。'"
    >
      <template #extra>
        <a-button class="proto-button ghost-button" @click="ensureAdmin">
          重新检查权限
        </a-button>
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
        <section class="avatar-review-board" aria-label="头像审核状态分区">
          <article
            v-for="lane in lanes"
            :key="lane.status"
            class="avatar-review-lane proto-surface proto-rounded"
            :class="`lane-${lane.tone}`"
          >
            <header class="avatar-review-lane-header">
              <div class="avatar-review-lane-title">
                <span class="avatar-review-lane-marker" aria-hidden="true"></span>
                <div>
                  <h2>{{ lane.label }}</h2>
                  <p>{{ lane.description }}</p>
                </div>
              </div>
              <strong class="avatar-review-lane-count">
                {{ itemsByStatus(lane.status).length }}
              </strong>
            </header>

            <div v-if="itemsByStatus(lane.status).length" class="avatar-review-list">
              <article
                v-for="item in itemsByStatus(lane.status)"
                :key="item.id"
                class="avatar-review-item"
              >
                <div class="avatar-review-item-heading">
                  <div class="avatar-review-avatar">
                    <img :src="item.avatarUrl" :alt="`${item.username}的新头像`" loading="lazy" />
                  </div>
                  <div class="avatar-review-user">
                    <strong>{{ item.username }}</strong>
                    <span>@{{ item.useraccount }}</span>
                  </div>
                  <a-tag class="proto-status" :class="statusClass(item.status)">
                    {{ statusText(item.status) }}
                  </a-tag>
                </div>

                <div class="avatar-review-new-avatar">
                  <span>待审核头像</span>
                  <span class="proto-mono">#{{ item.userId }}</span>
                </div>

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
                  {{ item.checkMessage || '管理员未填写原因' }}
                </div>

                <div v-if="item.status === 0" class="avatar-review-actions">
                  <a-button
                    class="proto-button acid-button"
                    type="primary"
                    :loading="actionLoading"
                    @click="approve(item)"
                  >
                    通过
                  </a-button>
                  <a-button
                    class="proto-button ghost-button danger-button"
                    :disabled="actionLoading"
                    @click="openReject(item)"
                  >
                    拒绝
                  </a-button>
                </div>

                <div v-else class="avatar-review-result">
                  <span>{{ item.status === 1 ? '审核结果已生效' : '保留原头像' }}</span>
                  <span class="proto-mono">{{ item.reviewedAt || '时间未知' }}</span>
                </div>
              </article>
            </div>

            <a-empty v-else class="avatar-review-empty" :description="`暂无${lane.label}记录`">
              <template #image>
                <div class="avatar-review-empty-mark">0</div>
              </template>
            </a-empty>
          </article>
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

const lanes: Array<{
  status: AvatarReviewStatus
  label: string
  description: string
  tone: 'pending' | 'pass' | 'refuse'
}> = [
  { status: 0, label: '待审核', description: '需要管理员处理', tone: 'pending' },
  { status: 1, label: '审核通过', description: '已允许使用新头像', tone: 'pass' },
  { status: 2, label: '审核失败', description: '保留用户原头像', tone: 'refuse' },
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
const rejectOpen = ref(false)
const rejectTarget = ref<AvatarReviewItem | null>(null)
const rejectReason = ref('')

const pendingCount = computed(() => itemsByStatus(0).length)

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
    status: item.status === 1 ? 1 : item.status === 2 ? 2 : 0,
    submittedAt,
    ...(item.reviewedAt ? { reviewedAt: formatDate(item.reviewedAt) } : {}),
    ...(item.checkMessage ? { checkMessage: item.checkMessage } : {}),
  }
}

function statusText(status: AvatarReviewStatus) {
  return status === 0 ? '待审核' : status === 1 ? '审核通过' : '审核失败'
}

function statusClass(status: AvatarReviewStatus) {
  return status === 0 ? 'wait' : status === 1 ? 'pass' : 'refuse'
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
.avatar-review-prototype {
  color: var(--proto-ink);
  font-family: 'Geist', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.avatar-review-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-top: clamp(14px, 2vw, 22px);
  padding-bottom: 17px;
  border-bottom: 1px solid var(--proto-line);
}

.avatar-review-heading-copy { min-width: 0; }
.avatar-review-title {
  margin: 0 0 9px;
  color: var(--proto-ink);
  font-size: clamp(30px, 3.6vw, 44px);
  font-weight: 800;
  letter-spacing: -.06em;
  line-height: 1;
  text-wrap: balance;
}

.avatar-review-description {
  max-width: 58ch;
  color: var(--proto-muted);
  font-size: 12px;
  line-height: 1.6;
}

.avatar-review-heading-actions {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.avatar-review-counter {
  min-width: 142px;
  padding: 12px 14px;
  background: var(--proto-ink);
  color: var(--proto-paper);
}

.avatar-review-counter span,
.avatar-review-counter strong { display: block; }
.avatar-review-counter span { color: var(--proto-orange); font-size: 11px; font-weight: 700; }
.avatar-review-counter strong {
  margin-top: 7px;
  color: var(--proto-acid);
  font-size: 34px;
  line-height: 1;
  letter-spacing: -.06em;
}
.avatar-review-auth-loading {
  display: block;
  min-height: 180px;
  padding-top: 70px;
  text-align: center;
}

.avatar-review-board {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
  padding-top: 17px;
  padding-bottom: 24px;
}

.avatar-review-lane {
  min-width: 0;
  /* 三个状态框保持同高，避免某一列因数据更多而把页面拉长。 */
  height: 610px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-top: 2px solid var(--proto-ink);
  box-shadow: none;
}

.avatar-review-lane.lane-pending { border-top-color: var(--proto-orange); }
.avatar-review-lane.lane-pass { border-top-color: #8dbb22; }
.avatar-review-lane.lane-refuse { border-top-color: #697171; }

.avatar-review-lane-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 15px 13px;
  border-bottom: 1px solid var(--proto-line);
}

/* 用低饱和状态底色强化三栏语义，同时保留文字和圆点，避免只依赖颜色识别。 */
.lane-pending .avatar-review-lane-header { background: rgba(255,137,106,.13); }
.lane-pass .avatar-review-lane-header { background: rgba(186,255,61,.18); }
.lane-refuse .avatar-review-lane-header { background: rgba(17,20,22,.08); }

.avatar-review-lane-title { display: flex; align-items: flex-start; gap: 9px; min-width: 0; }
.avatar-review-lane-marker {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  flex: 0 0 8px;
  border-radius: 50%;
  background: var(--proto-orange);
  box-shadow: 0 0 0 4px rgba(255,137,106,.12);
}
.lane-pass .avatar-review-lane-marker { background: #8dbb22; box-shadow: 0 0 0 4px rgba(141,187,34,.14); }
.lane-refuse .avatar-review-lane-marker { background: #697171; box-shadow: 0 0 0 4px rgba(105,113,113,.13); }

.avatar-review-lane-header h2 { margin: 0; font-size: 20px; font-weight: 800; letter-spacing: -.05em; }
.avatar-review-lane-header p { margin-top: 5px; color: var(--proto-muted); font-size: 11px; }
.avatar-review-lane-count { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 22px; line-height: 1; }
.lane-pass .avatar-review-lane-count { color: #668e11; }
.lane-refuse .avatar-review-lane-count { color: #697171; }

.avatar-review-list {
  display: grid;
  gap: 10px;
  align-content: start;
  align-items: start;
  /* 列表区域填满固定状态框，约可看到两张半卡片，超出后只在本列内滚动。 */
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 10px;
  scrollbar-gutter: stable;
}
.avatar-review-item {
  padding: 12px;
  /* 让不同状态的卡片保持统一行高，避免短卡片挤出第五条记录。 */
  min-height: 200px;
  border: 1px solid var(--proto-line);
  background: rgba(255,255,255,.42);
  transition: background-color .2s ease, border-color .2s ease;
}
.avatar-review-item:hover { border-color: rgba(17,20,22,.34); background: rgba(186,255,61,.08); }

.avatar-review-item-heading { display: flex; align-items: center; gap: 10px; min-width: 0; }
.avatar-review-avatar {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  overflow: hidden;
  border: 2px solid var(--proto-paper);
  border-radius: 50%;
  background: var(--proto-paper-deep);
  box-shadow: 0 0 0 1px var(--proto-line);
}
.avatar-review-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; }
.avatar-review-user { min-width: 0; flex: 1; }
.avatar-review-user strong,
.avatar-review-user span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.avatar-review-user strong { color: var(--proto-ink); font-size: 14px; font-weight: 800; }
.avatar-review-user span { margin-top: 5px; color: var(--proto-muted); font-size: 11px; }

.avatar-review-new-avatar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--proto-line);
  color: var(--proto-muted);
  font-size: 10px;
}

.avatar-review-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 12px 0 0;
}
.avatar-review-meta div { min-width: 0; }
.avatar-review-meta dt { color: var(--proto-muted); font-size: 10px; }
.avatar-review-meta dd { margin: 4px 0 0; color: var(--proto-ink); font-size: 11px; font-weight: 700; overflow-wrap: anywhere; }

.avatar-review-message {
  margin-top: 12px;
  padding: 8px 10px;
  border: 1px solid rgba(255,137,106,.35);
  background: rgba(255,137,106,.12);
  color: var(--proto-muted);
  font-size: 11px;
  line-height: 1.5;
}
.avatar-review-message span { margin-right: 5px; color: #973816; font-weight: 700; }

.avatar-review-actions { display: flex; gap: 8px; margin-top: 14px; }
.avatar-review-actions .proto-button { flex: 1; }
.danger-button:not(:disabled) { color: #973816; border-color: #973816; }
.danger-button:not(:disabled):hover { color: #973816 !important; border-color: var(--proto-orange) !important; }

.avatar-review-result {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 13px;
  padding-top: 10px;
  border-top: 1px solid var(--proto-line);
  color: var(--proto-muted);
  font-size: 10px;
}

.avatar-review-empty {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 20px 12px;
}
.avatar-review-empty-mark {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 1px solid var(--proto-line);
  color: var(--proto-orange);
  font-size: 18px;
  font-weight: 700;
}

.avatar-review-modal-user { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.avatar-review-modal-user img { width: 52px; height: 52px; border-radius: 50%; object-fit: cover; }
.avatar-review-modal-user strong,
.avatar-review-modal-user span { display: block; }
.avatar-review-modal-user strong { font-size: 15px; }
.avatar-review-modal-user span { margin-top: 5px; color: var(--proto-muted); font-size: 11px; }

@media (max-width: 920px) {
  .avatar-review-board { grid-template-columns: 1fr; }
}

@media (max-width: 620px) {
  .avatar-review-heading { align-items: flex-start; flex-direction: column; }
  .avatar-review-heading-actions { width: 100%; align-items: stretch; justify-content: space-between; }
  .avatar-review-counter { flex: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .avatar-review-item { transition: none; }
}
</style>
