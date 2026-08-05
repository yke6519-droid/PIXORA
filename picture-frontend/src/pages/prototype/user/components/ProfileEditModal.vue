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

      <a-form-item label="头像">
        <div class="avatar-edit-row">
          <a-avatar :size="56" :src="avatarPreview || editForm.avatarurl">
            {{ avatarFallback }}
          </a-avatar>
          <div class="avatar-edit-action">
            <a-upload
              :before-upload="selectAvatar"
              :show-upload-list="false"
              accept=".jpg,.jpeg,.png,.webp"
            >
              <a-button class="proto-button ghost-button">选择新头像</a-button>
            </a-upload>
            <span>{{ avatarFile?.name || '支持 JPG、PNG、WEBP，最大 5MB' }}</span>
          </div>
        </div>
        <p class="avatar-permission-note">选择后会在保存资料时上传，头像无需审核并立即生效。</p>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, UploadProps } from 'ant-design-vue'
import { avatarUploadUsingPost } from '../../../../api/fileController'
import { updateSelfUsingPost } from '../../../../api/userController'

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
const avatarFile = ref<File>()
const avatarPreview = ref('')
const editForm = reactive<API.UpdateSelfRequest>({
  username: '',
  avatarurl: '',
  gender: 0,
  phone: '',
  email: '',
  profile: '',
})

const avatarFallback = computed(() => (editForm.username || props.user.username || '用').charAt(0))

/**
 * 每次打开弹窗都以服务端最新用户资料为准，避免上次取消的内容残留。
 */
function resetForm() {
  editForm.username = props.user.username || ''
  editForm.avatarurl = props.user.avatarurl || ''
  editForm.gender = props.user.gender ?? 0
  editForm.phone = props.user.phone || ''
  editForm.email = props.user.email || ''
  editForm.profile = props.user.profile || ''
  clearAvatarSelection()
}

function clearAvatarSelection() {
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
  avatarPreview.value = ''
  avatarFile.value = undefined
}

/**
 * 文件格式和大小由后端统一校验；前端这里只负责暂存文件并提供即时预览。
 */
const selectAvatar: UploadProps['beforeUpload'] = (file) => {
  clearAvatarSelection()
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
  return false
}

function closeModal() {
  if (saving.value) return
  clearAvatarSelection()
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
    let avatarurl = editForm.avatarurl?.trim() || undefined

    // 现有后端采用“先上传文件、再更新用户资料”的两步接口。
    if (avatarFile.value) {
      const uploadRes = await avatarUploadUsingPost({}, avatarFile.value)
      if (uploadRes.data?.code !== 200 || !uploadRes.data.data) {
        throw new Error(uploadRes.data?.message || '头像上传失败')
      }
      avatarurl = uploadRes.data.data
    }

    const updateRes = await updateSelfUsingPost({
      username: editForm.username?.trim(),
      avatarurl,
      gender: editForm.gender,
      phone: editForm.phone?.trim(),
      email: editForm.email?.trim(),
      profile: editForm.profile?.trim(),
    })
    if (updateRes.data?.code !== 200) {
      throw new Error(updateRes.data?.message || '个人资料更新失败')
    }

    clearAvatarSelection()
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
    else clearAvatarSelection()
  },
)
</script>

<style scoped>
.profile-edit-form { padding-top: 6px; }
.profile-edit-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.avatar-edit-row { display: flex; align-items: center; gap: 14px; }
.avatar-edit-action { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; }
.avatar-edit-action span {
  max-width: 310px;
  overflow: hidden;
  color: var(--proto-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.avatar-permission-note {
  margin: 8px 0 0;
  color: var(--proto-orange);
  font-family: 'DM Mono', monospace;
  font-size: 9px;
  line-height: 1.5;
}
@media (max-width: 520px) {
  .profile-edit-fields { grid-template-columns: 1fr; gap: 0; }
  .avatar-edit-row { align-items: flex-start; }
}
</style>
