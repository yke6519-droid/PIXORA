<template>
  <div class="user-admin-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">用户管理 / queryPages</span>
        <h1 class="proto-title">管理用户，<br />也管理权限边界。</h1>
        <p class="proto-copy">管理员分页请求严格使用 `current`、`size`、`queryUsername`、`queryUserAccount`、`userStatus` 和 `gender`，不与图片分页的 `pageSize` 混用。</p>
      </div>
      <a-button class="proto-button acid-button" type="primary" @click="addOpen = true">新增用户</a-button>
    </section>

    <section class="admin-filter proto-surface proto-rounded proto-section">
      <div class="admin-filter-line"><a-input v-model:value="filters.queryUserAccount" placeholder="账号 queryUserAccount" allow-clear /><a-input v-model:value="filters.queryUsername" placeholder="用户名 queryUsername" allow-clear /><a-select v-model:value="filters.userStatus" style="width: 130px"><a-select-option value="all">全部角色</a-select-option><a-select-option value="admin">管理员</a-select-option><a-select-option value="user">普通用户</a-select-option><a-select-option value="vip">VIP 用户</a-select-option></a-select><a-select v-model:value="filters.gender" style="width: 110px"><a-select-option value="all">全部性别</a-select-option><a-select-option :value="0">男士</a-select-option><a-select-option :value="1">女士</a-select-option></a-select><a-button class="proto-button ghost-button" @click="resetFilters">重置</a-button></div>
      <div class="admin-filter-foot"><span>queryPages / current={{ current }} / size={{ pageSize }}</span><span>共 {{ filteredUsers.length + 18 }} 位用户</span></div>
    </section>

    <section class="admin-users-table proto-section">
      <a-table class="proto-table" :columns="columns" :data-source="filteredUsers" :pagination="false" row-key="id" :scroll="{ x: 960 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <div class="admin-user-cell"><a-avatar :src="record.avatarurl" :size="34">{{ record.username.charAt(0) }}</a-avatar><span><strong>{{ record.username }}</strong><small>{{ record.useraccount }}</small></span></div>
          </template>
          <template v-else-if="column.key === 'role'"><a-tag class="proto-status" :class="record.userstatus === 'admin' ? 'wait' : record.userstatus === 'vip' ? 'pass' : 'refuse'">{{ roleText(record.userstatus) }}</a-tag></template>
          <template v-else-if="column.key === 'contact'"><div class="admin-contact"><span>{{ record.phone }}</span><small>{{ record.email }}</small></div></template>
          <template v-else-if="column.key === 'actions'"><a-button type="link" @click="openEdit(record)">编辑</a-button><a-button type="link" danger :disabled="record.userstatus === 'admin'" @click="deleteUser(record)">删除</a-button></template>
        </template>
      </a-table>
      <div class="admin-pagination"><a-pagination v-model:current="current" :page-size="pageSize" :total="filteredUsers.length + 18" show-less-items /></div>
    </section>

    <a-modal v-model:open="addOpen" title="新增用户" ok-text="创建用户" cancel-text="取消" @ok="submitAdd"><a-form layout="vertical" class="proto-form"><div class="admin-form-two"><a-form-item label="username"><a-input v-model:value="form.username" /></a-form-item><a-form-item label="useraccount"><a-input v-model:value="form.useraccount" /></a-form-item></div><div class="admin-form-two"><a-form-item label="gender"><a-select v-model:value="form.gender" style="width: 100%"><a-select-option :value="0">男士</a-select-option><a-select-option :value="1">女士</a-select-option></a-select></a-form-item><a-form-item label="phone"><a-input v-model:value="form.phone" /></a-form-item></div></a-form></a-modal>
    <a-modal v-model:open="editOpen" title="编辑用户" ok-text="保存修改" cancel-text="取消" @ok="submitEdit"><a-form layout="vertical" class="proto-form"><a-form-item label="username"><a-input v-model:value="form.username" /></a-form-item><a-form-item label="phone"><a-input v-model:value="form.phone" /></a-form-item><a-form-item label="email"><a-input v-model:value="form.email" /></a-form-item><a-form-item label="profile"><a-textarea v-model:value="form.profile" :rows="3" /></a-form-item><a-form-item label="userStatus"><a-select v-model:value="form.userstatus" style="width: 100%"><a-select-option value="user">普通用户</a-select-option><a-select-option value="vip">VIP 用户</a-select-option><a-select-option value="admin">管理员</a-select-option></a-select></a-form-item></a-form></a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { prototypeUsers } from '../prototypeData'

const columns = [
  { title: '用户', key: 'user', width: 210 },
  { title: '角色', key: 'role', width: 120 },
  { title: '联系方式', key: 'contact', width: 220 },
  { title: '注册时间', dataIndex: 'createtime', key: 'createtime', width: 190 },
  { title: '操作', key: 'actions', width: 160 },
]
const current = ref(1)
const pageSize = 10
const filters = reactive<{ queryUserAccount: string; queryUsername: string; userStatus: string; gender: string | number }>({ queryUserAccount: '', queryUsername: '', userStatus: 'all', gender: 'all' })
const addOpen = ref(false)
const editOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ username: '', useraccount: '', gender: 0, phone: '', email: '', profile: '', userstatus: 'user' })
const filteredUsers = computed(() => prototypeUsers.filter((user) => {
  const accountMatch = !filters.queryUserAccount || user.useraccount.includes(filters.queryUserAccount)
  const nameMatch = !filters.queryUsername || user.username.includes(filters.queryUsername)
  const roleMatch = filters.userStatus === 'all' || user.userstatus === filters.userStatus
  const genderMatch = filters.gender === 'all' || user.gender === filters.gender
  return accountMatch && nameMatch && roleMatch && genderMatch
}))

function roleText(role: string) { return role === 'admin' ? '管理员' : role === 'vip' ? 'VIP 用户' : '普通用户' }
function resetFilters() { filters.queryUserAccount = ''; filters.queryUsername = ''; filters.userStatus = 'all'; filters.gender = 'all'; current.value = 1 }
function openEdit(user: typeof prototypeUsers[number]) { editingId.value = user.id; Object.assign(form, user); editOpen.value = true }
function submitAdd() { message.success('原型已提交：addUser'); addOpen.value = false }
function submitEdit() { message.success(`原型已提交：updateUser #${editingId.value}`); editOpen.value = false }
function deleteUser(user: typeof prototypeUsers[number]) { message.warning(`原型演示：deleteById #${user.id}`) }
</script>

<style scoped>
.admin-filter { padding: 0; overflow: hidden; }
.admin-filter-line { padding: 17px; display: flex; gap: 10px; flex-wrap: wrap; }
.admin-filter-line .ant-input { width: 210px; }
.admin-filter-foot { padding: 12px 17px; display: flex; justify-content: space-between; gap: 10px; border-top: 1px solid var(--proto-line); color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
.admin-user-cell { display: flex; align-items: center; gap: 10px; }
.admin-user-cell strong, .admin-user-cell small { display: block; }
.admin-user-cell strong { font-size: 12px; }
.admin-user-cell small { margin-top: 3px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.admin-contact span, .admin-contact small { display: block; }
.admin-contact span { font-size: 11px; }
.admin-contact small { margin-top: 4px; color: var(--proto-muted); font-size: 10px; }
.admin-pagination { display: flex; justify-content: flex-end; padding-top: 25px; }
.admin-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.admin-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }
.admin-form-two { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
@media (max-width: 560px) { .admin-filter-foot { align-items: flex-start; flex-direction: column; } .admin-form-two { grid-template-columns: 1fr; gap: 0; } }
</style>
