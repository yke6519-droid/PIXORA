<template>
  <div class="user-admin-prototype">
    <section class="user-admin-heading">
      <div class="user-admin-heading-copy">
        <h1 class="user-admin-title">用户管理</h1>
        <span class="admin-total">{{ total }} 位用户</span>
      </div>
      <a-button class="proto-button acid-button" type="primary" @click="openAdd">
        新增用户
      </a-button>
    </section>

    <a-spin v-if="authChecking" class="admin-auth-loading" tip="正在确认管理员权限..." />

    <a-result
      v-else-if="!authorized"
      status="403"
      title="暂时无法进入用户管理"
      :sub-title="accessError || '只有管理员可以管理用户。'"
    >
      <template #extra>
        <a-button class="proto-button ghost-button" @click="ensureAdmin">重新检查权限</a-button>
      </template>
    </a-result>

    <template v-else>
      <a-alert
        v-if="loadError"
        class="admin-alert"
        type="error"
        show-icon
        :message="loadError"
        description="请确认后端服务和当前登录会话正常后重试。"
        closable
        @close="loadError = ''"
      />

      <section class="admin-filter proto-surface proto-rounded proto-section">
        <div class="admin-filter-line">
          <a-input
            v-model:value="filters.queryUserAccount"
            placeholder="按账号搜索"
            allow-clear
            @press-enter="searchUsers"
          />
          <a-input
            v-model:value="filters.queryUsername"
            placeholder="按用户名搜索"
            allow-clear
            @press-enter="searchUsers"
          />
          <a-select v-model:value="filters.userLevel" style="width: 130px" @change="searchUsers">
            <a-select-option value="all">全部角色</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="vip">VIP 用户</a-select-option>
          </a-select>
          <a-select v-model:value="filters.gender" style="width: 110px" @change="searchUsers">
            <a-select-option value="all">全部性别</a-select-option>
            <a-select-option :value="0">男</a-select-option>
            <a-select-option :value="1">女</a-select-option>
          </a-select>
          <a-button class="proto-button acid-button" type="primary" :loading="loading" @click="searchUsers">
            搜索
          </a-button>
          <a-button class="proto-button ghost-button" :disabled="loading" @click="resetFilters">重置</a-button>
        </div>
        <div class="admin-filter-foot">
          <span>第 {{ current }} 页 · 每页 {{ pageSize }} 位</span>
          <span>共 {{ total }} 位用户</span>
        </div>
      </section>

      <section class="admin-users-table proto-section">
        <a-table
          class="proto-table"
          :columns="columns"
          :data-source="users"
          :loading="loading"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 960 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'user'">
              <div class="admin-user-cell">
                <a-avatar :src="record.avatarurl" :size="34">
                  {{ (record.username || record.useraccount || '?').charAt(0) }}
                </a-avatar>
                <span>
                  <strong>{{ record.username || '未命名用户' }}</strong>
                  <small>{{ record.useraccount || '未填写账号' }}</small>
                </span>
              </div>
            </template>
            <template v-else-if="column.key === 'role'">
              <a-tag class="proto-status" :class="roleClass(record.userLevel)">
                {{ roleText(record.userLevel) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'contact'">
              <div class="admin-contact">
                <span>{{ record.phone || '暂无电话' }}</span>
                <small>{{ record.email || '暂无邮箱' }}</small>
              </div>
            </template>
            <template v-else-if="column.key === 'gender'">
              <span>{{ genderText(record.gender) }}</span>
            </template>
            <template v-else-if="column.key === 'createtime'">
              <span class="proto-mono">{{ formatDate(record.createtime) }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <div class="admin-actions">
                <a-button type="link" @click="openEdit(record)">编辑</a-button>
                <a-popconfirm
                  title="确认删除这个用户吗？"
                  description="删除后用户将无法继续登录，且此操作不可撤销。"
                  ok-text="确认删除"
                  cancel-text="取消"
                  @confirm="deleteUser(record)"
                >
                  <a-button type="link" danger :disabled="isProtectedUser(record)">删除</a-button>
                </a-popconfirm>
              </div>
            </template>
          </template>
        </a-table>

        <a-empty v-if="!loading && !users.length" description="没有符合条件的用户">
          <template #footer>
            <a-button class="proto-button ghost-button" @click="resetFilters">清空筛选</a-button>
          </template>
        </a-empty>

        <div v-if="!loading && total > 0" class="admin-pagination">
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
      v-model:open="addOpen"
      title="新增用户"
      ok-text="创建用户"
      cancel-text="取消"
      :confirm-loading="actionLoading"
      destroy-on-close
      @ok="submitAdd"
    >
      <a-form layout="vertical" class="proto-form">
        <div class="admin-form-two">
          <a-form-item label="用户名" required>
            <a-input v-model:value="form.username" placeholder="请输入用户名" />
          </a-form-item>
          <a-form-item label="账号" required>
            <a-input v-model:value="form.useraccount" placeholder="请输入登录账号" />
          </a-form-item>
        </div>
        <div class="admin-form-two">
          <a-form-item label="角色">
            <a-select v-model:value="form.userLevel" style="width: 100%">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="vip">VIP 用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="性别">
            <a-select v-model:value="form.gender" style="width: 100%">
              <a-select-option :value="0">男</a-select-option>
              <a-select-option :value="1">女</a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item label="电话">
          <a-input v-model:value="form.phone" placeholder="请输入电话" />
        </a-form-item>
        <p class="admin-form-hint">后端会为新用户设置初始密码，账号创建后请提醒用户尽快修改。</p>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="editOpen"
      title="编辑用户"
      ok-text="保存修改"
      cancel-text="取消"
      :confirm-loading="actionLoading"
      destroy-on-close
      @ok="submitEdit"
    >
      <a-form layout="vertical" class="proto-form">
        <a-form-item label="用户名">
          <a-input v-model:value="form.username" />
        </a-form-item>
        <div class="admin-form-two">
          <a-form-item label="角色">
            <a-select v-model:value="form.userLevel" style="width: 100%">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="vip">VIP 用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="性别">
            <a-select v-model:value="form.gender" style="width: 100%">
              <a-select-option :value="0">男</a-select-option>
              <a-select-option :value="1">女</a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div class="admin-form-two">
          <a-form-item label="电话">
            <a-input v-model:value="form.phone" />
          </a-form-item>
          <a-form-item label="邮箱">
            <a-input v-model:value="form.email" />
          </a-form-item>
        </div>
        <a-form-item label="个人简介">
          <a-textarea v-model:value="form.profile" :rows="3" :maxlength="200" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addUser,
  deleteUser as deleteUserApi,
  queryPages,
  updateUser,
} from '../../../api/userController'
import { getCurrentUser } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'

type UserLevel = 'admin' | 'user' | 'vip'
type FilterGender = 'all' | 0 | 1

const columns = [
  { title: '用户', key: 'user', width: 210 },
  { title: '角色', key: 'role', width: 120 },
  { title: '性别', key: 'gender', width: 80 },
  { title: '联系方式', key: 'contact', width: 220 },
  { title: '注册时间', key: 'createtime', width: 190 },
  { title: '操作', key: 'actions', width: 160 },
]

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const authChecking = ref(true)
const authorized = ref(false)
const accessError = ref('')
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const users = ref<API.UserVO[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = 10

const filters = reactive({
  queryUserAccount: '',
  queryUsername: '',
  userLevel: 'all' as UserLevel | 'all',
  gender: 'all' as FilterGender,
})
const addOpen = ref(false)
const editOpen = ref(false)
const editingId = ref<number | string | null>(null)
const form = reactive({
  username: '',
  useraccount: '',
  gender: 0,
  phone: '',
  email: '',
  profile: '',
  userLevel: 'user' as UserLevel,
})

const currentUserId = computed(() => normalizeId(loginUserStore.loginUser?.id))

function normalizeId(id?: number | string | null) {
  return id == null ? '' : String(id)
}

function numberValue(value?: number | string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function roleText(role?: string) {
  return role === 'admin' ? '管理员' : role === 'vip' ? 'VIP 用户' : '普通用户'
}

function roleClass(role?: string) {
  return role === 'admin' ? 'wait' : role === 'vip' ? 'pass' : 'refuse'
}

function genderText(gender?: number) {
  return gender === 0 ? '男' : gender === 1 ? '女' : '未填写'
}

function formatDate(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '未记录时间'
}

function isProtectedUser(user: API.UserVO) {
  return user.userLevel === 'admin' || normalizeId(user.id) === currentUserId.value
}

/** 管理员页面仍然通过后端 Session 确认身份，不只相信前端 Pinia 状态。 */
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
      accessError.value = '当前账号不是管理员，无法调用用户管理接口。'
      authorized.value = false
      return false
    }
    authorized.value = true
    await loadUsers()
    return true
  } catch (error: any) {
    authorized.value = false
    accessError.value = error?.response?.data?.message || error?.message || '管理员权限检查失败'
    return false
  } finally {
    authChecking.value = false
  }
}

/** 按后端 QueryPageRequest 的字段名组装请求，不把前端筛选对象直接透传。 */
async function loadUsers() {
  if (!authorized.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await queryPages({
      current: current.value,
      size: pageSize,
      queryUserAccount: filters.queryUserAccount.trim() || undefined,
      queryUsername: filters.queryUsername.trim() || undefined,
      userLevel: filters.userLevel === 'all' ? undefined : filters.userLevel,
      gender: filters.gender === 'all' ? undefined : filters.gender,
      sortField: 'createtime',
      sortOrder: 'descend',
    })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '用户列表加载失败')
    users.value = res.data.data?.userList || []
    total.value = numberValue(res.data.data?.totalSize)
  } catch (error: any) {
    users.value = []
    total.value = 0
    loadError.value = error?.response?.data?.message || error?.message || '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function searchUsers() {
  current.value = 1
  await loadUsers()
}

async function resetFilters() {
  filters.queryUserAccount = ''
  filters.queryUsername = ''
  filters.userLevel = 'all'
  filters.gender = 'all'
  current.value = 1
  await loadUsers()
}

async function handlePageChange(page: number) {
  current.value = page
  await loadUsers()
}

function resetForm() {
  Object.assign(form, {
    username: '',
    useraccount: '',
    gender: 0,
    phone: '',
    email: '',
    profile: '',
    userLevel: 'user' as UserLevel,
  })
  editingId.value = null
}

function openAdd() {
  resetForm()
  addOpen.value = true
}

function openEdit(user: API.UserVO) {
  editingId.value = user.id ?? null
  Object.assign(form, {
    username: user.username || '',
    useraccount: user.useraccount || '',
    gender: user.gender ?? 0,
    phone: user.phone || '',
    email: user.email || '',
    profile: user.profile || '',
    userLevel: (user.userLevel === 'admin' || user.userLevel === 'vip' ? user.userLevel : 'user') as UserLevel,
  })
  editOpen.value = true
}

async function submitAdd() {
  if (!form.username.trim() || !form.useraccount.trim()) {
    message.warning('用户名和账号不能为空')
    return
  }
  actionLoading.value = true
  try {
    const res = await addUser({
      username: form.username.trim(),
      useraccount: form.useraccount.trim(),
      gender: form.gender,
      phone: form.phone.trim() || undefined,
      userLevel: form.userLevel,
    })
    if (res.data?.code !== 200 || res.data.data === false) throw new Error(res.data?.message || '新增用户失败')
    message.success('用户创建成功')
    addOpen.value = false
    resetForm()
    current.value = 1
    await loadUsers()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '新增用户失败')
  } finally {
    actionLoading.value = false
  }
}

async function submitEdit() {
  if (editingId.value == null) return
  if (!form.username.trim()) {
    message.warning('用户名不能为空')
    return
  }
  actionLoading.value = true
  try {
    const res = await updateUser({
      id: editingId.value,
      username: form.username.trim(),
      gender: form.gender,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
      profile: form.profile.trim() || undefined,
      userLevel: form.userLevel,
    })
    if (res.data?.code !== 200 || res.data.data === false) throw new Error(res.data?.message || '更新用户失败')
    message.success('用户信息已更新')
    editOpen.value = false
    resetForm()
    await loadUsers()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '更新用户失败')
  } finally {
    actionLoading.value = false
  }
}

async function deleteUser(user: API.UserVO) {
  if (isProtectedUser(user) || user.id == null) return
  actionLoading.value = true
  try {
    const res = await deleteUserApi({ id: user.id })
    if (res.data?.code !== 200 || res.data.data === false) throw new Error(res.data?.message || '删除用户失败')
    message.success('用户已删除')
    if (users.value.length === 1 && current.value > 1) current.value -= 1
    await loadUsers()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '删除用户失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  void ensureAdmin()
})
</script>

<style scoped>
.user-admin-prototype {
  min-width: 0;
  color: var(--proto-ink);
  font-family: 'Geist', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.user-admin-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 0 16px;
  border-bottom: 1px solid var(--proto-line);
}

.user-admin-heading-copy {
  display: flex;
  align-items: baseline;
  gap: 14px;
  min-width: 0;
}

.user-admin-title {
  margin: 0;
  color: var(--proto-ink);
  font-size: 42px;
  line-height: 1;
  letter-spacing: -0.035em;
  font-weight: 800;
}

.admin-total {
  color: var(--proto-muted);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.admin-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.admin-alert { margin-bottom: 18px; }
.admin-filter { padding: 0; overflow: hidden; }
.admin-filter-line { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1.4fr) 150px 120px auto auto; align-items: center; gap: 10px; padding: 15px; }
.admin-filter-line .ant-input,
.admin-filter-line .ant-select { width: 100% !important; }
.admin-filter-line :deep(.ant-input),
.admin-filter-line :deep(.ant-select-selection-item),
.admin-filter-line :deep(.ant-select-selection-placeholder) { font-family: inherit; font-size: 13px; }
.admin-filter-line :deep(.ant-btn) { font-family: inherit; font-size: 13px; font-weight: 700; }
.admin-filter-line .proto-button { white-space: nowrap; }
.admin-filter-foot { display: flex; justify-content: space-between; gap: 10px; padding: 11px 15px; border-top: 1px solid var(--proto-line); color: var(--proto-muted); font-size: 12px; line-height: 1.45; }
.admin-users-table.proto-section { padding-top: 0; }
.admin-users-table :deep(.ant-table) { color: var(--proto-ink); font-family: inherit; font-size: 13px; }
.admin-users-table :deep(.ant-table-thead > tr > th) { padding: 11px 8px; font-family: inherit; font-size: 13px; font-weight: 700; letter-spacing: 0; line-height: 1.45; }
.admin-users-table :deep(.ant-table-tbody > tr > td) { padding: 13px 8px; font-size: 13px; line-height: 1.45; }
.admin-user-cell { display: flex; align-items: center; gap: 10px; }
.admin-user-cell strong, .admin-user-cell small { display: block; }
.admin-user-cell strong { color: var(--proto-ink); font-size: 14px; font-weight: 800; line-height: 1.45; }
.admin-user-cell small { margin-top: 3px; color: var(--proto-muted); font-size: 12px; line-height: 1.45; }
.admin-contact span, .admin-contact small { display: block; }
.admin-contact span { color: var(--proto-ink); font-size: 13px; line-height: 1.45; }
.admin-contact small { margin-top: 4px; color: var(--proto-muted); font-size: 12px; line-height: 1.45; }
.admin-users-table :deep(.ant-tag) { font-family: inherit; font-size: 12px; line-height: 20px; }
.admin-users-table :deep(.ant-btn-link) { font-family: inherit; font-size: 13px; line-height: 1.45; }
.admin-actions { display: flex; align-items: center; }
.admin-pagination { display: flex; justify-content: flex-end; padding-top: 18px; }
.admin-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.admin-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }
.admin-form-two { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
.admin-form-hint { margin: -2px 0 0; color: var(--proto-muted); font-size: 12px; line-height: 1.6; }

@media (max-width: 1180px) {
  .admin-filter-line { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .admin-filter-line .proto-button { width: 100%; }
}

@media (max-width: 650px) {
  .user-admin-heading { align-items: flex-start; flex-direction: column; gap: 14px; padding-top: 20px; }
  .user-admin-title { font-size: 36px; }
  .admin-filter-line { grid-template-columns: 1fr; }
  .admin-filter-foot { align-items: flex-start; flex-direction: column; }
  .admin-form-two { grid-template-columns: 1fr; gap: 0; }
}
</style>
