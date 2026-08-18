<template>
  <a-modal
    :open="open"
    title="编辑个人资料"
    ok-text="保存并生效"
    cancel-text="取消"
    :confirm-loading="saving"
    :mask-closable="!saving"
    @ok="saveProfile"
    @cancel="closeModal"
  >
    <a-form ref="formRef" :model="editForm" layout="vertical" class="proto-form profile-edit-form">
      <a-form-item
        label="用户名"
        name="username"
        :rules="[{ required: true, whitespace: true, message: '请输入用户名' }]"
      >
        <a-input v-model:value="editForm.username" placeholder="请输入用户名" />
      </a-form-item>

      <a-form-item label="性别" name="gender">
        <a-radio-group v-model:value="editForm.gender">
          <a-radio :value="0">男</a-radio>
          <a-radio :value="1">女</a-radio>
        </a-radio-group>
      </a-form-item>

      <div class="profile-edit-fields">
        <a-form-item
          label="手机号"
          name="phone"
          :rules="[{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的 11 位手机号' }]"
        >
          <a-input v-model:value="editForm.phone" placeholder="选填" />
        </a-form-item>

        <a-form-item
          label="邮箱"
          name="email"
          :rules="[{ type: 'email', message: '请输入有效的邮箱地址' }]"
        >
          <a-input v-model:value="editForm.email" placeholder="选填" />
        </a-form-item>
      </div>

      <a-form-item label="个人简介" name="profile">
        <a-textarea
          v-model:value="editForm.profile"
          :rows="3"
          placeholder="简单介绍一下自己"
        />
      </a-form-item>

    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { updateSelf } from '../../../../api/userController'

const props = defineProps<{
  open: boolean
  user: API.UserVO
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const editForm = reactive<API.UpdateSelfRequest>({
  username: '',
  gender: 0,
  phone: '',
  email: '',
  profile: '',
})

/**
 * 每次打开弹窗都以服务端最新用户资料为准，避免上次取消的内容残留。
 */
function resetForm() {
  editForm.username = props.user.username || ''
  editForm.gender = props.user.gender ?? 0
  editForm.phone = props.user.phone || ''
  editForm.email = props.user.email || ''
  editForm.profile = props.user.profile || ''
}

function closeModal() {
  if (saving.value) return
  emit('update:open', false)
}

async function saveProfile() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const updateRes = await updateSelf({
      username: editForm.username?.trim(),
      gender: editForm.gender,
      phone: editForm.phone?.trim(),
      email: editForm.email?.trim(),
      profile: editForm.profile?.trim(),
    })
    if (updateRes.data?.code !== 200) {
      throw new Error(updateRes.data?.message || '个人资料更新失败')
    }

    emit('update:open', false)
    emit('saved')
    message.success('个人资料已更新')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '个人资料更新失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) resetForm()
  },
)
</script>

<style scoped>
.profile-edit-form { padding-top: 6px; }
.profile-edit-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 520px) {
  .profile-edit-fields { grid-template-columns: 1fr; gap: 0; }
}
</style>
