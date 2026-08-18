<template>
  <a-modal
    :open="open"
    title="更新头像"
    cancel-text="取消"
    :confirm-loading="uploading"
    :mask-closable="!uploading"
    @cancel="closeModal"
  >
    <div class="avatar-update-content">
      <div class="avatar-update-preview">
        <span class="avatar-preview-label">当前头像</span>
        <a-avatar :size="96" :src="user.avatarurl">
          {{ avatarFallback }}
        </a-avatar>
        <strong>{{ user.username || user.useraccount || '图库用户' }}</strong>
      </div>

      <div class="avatar-update-form">
        <a-upload
          :before-upload="selectAvatar"
          :show-upload-list="false"
          accept=".jpg,.jpeg,.png,.webp"
        >
          <a-button class="proto-button ghost-button">选择新头像</a-button>
        </a-upload>
        <span class="avatar-file-name">{{ avatarFile?.name || '支持 JPG、PNG、WEBP，最大 5MB' }}</span>
        <div class="avatar-new-preview" aria-live="polite">
          <span class="avatar-preview-label">新头像预览</span>
          <a-avatar v-if="avatarPreview" :size="96" :src="avatarPreview">
            {{ avatarFallback }}
          </a-avatar>
          <span v-else class="avatar-preview-empty">—</span>
        </div>
      </div>
    </div>

    <template #footer>
      <a-button class="proto-button ghost-button" @click="closeModal">取消</a-button>
      <a-button
        class="proto-button acid-button"
        type="primary"
        :loading="uploading"
        :disabled="!avatarFile"
        @click="submitAvatar"
      >
        提交头像
      </a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { avatarUpload } from '../../../../api/fileController'
import { resolveAvatarUploadOutcome } from '../avatarUpdateModel'

const props = defineProps<{
  open: boolean
  user: API.UserVO
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const uploading = ref(false)
const avatarFile = ref<File>()
const avatarPreview = ref('')
const avatarFallback = computed(() => (props.user.username || props.user.useraccount || '用').charAt(0))

function clearAvatarSelection() {
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
  avatarPreview.value = ''
  avatarFile.value = undefined
}

/**
 * 前端只做即时格式和大小提示，最终校验仍以后端为准。
 */
const selectAvatar: UploadProps['beforeUpload'] = (file) => {
  const suffix = file.name.toLowerCase().split('.').pop()
  if (!['jpg', 'jpeg', 'png', 'webp'].includes(suffix || '')) {
    message.error('头像仅支持 JPG、PNG、WEBP 格式')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('头像大小不能超过 5MB')
    return false
  }

  clearAvatarSelection()
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
  return false
}

function closeModal() {
  if (uploading.value) return
  clearAvatarSelection()
  emit('update:open', false)
}

async function submitAvatar() {
  if (!avatarFile.value || uploading.value) return

  uploading.value = true
  try {
    const response = await avatarUpload(avatarFile.value)
    const payload = response.data?.data
    if (response.data?.code !== 200 || !payload) {
      throw new Error(response.data?.message || '头像上传失败')
    }

    const outcome = resolveAvatarUploadOutcome(payload)
    if (outcome.status === 'invalid') throw new Error(outcome.message)

    clearAvatarSelection()
    emit('update:open', false)
    emit('saved')
    message.success(outcome.message)
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '头像上传失败')
  } finally {
    uploading.value = false
  }
}

watch(
  () => props.open,
  () => clearAvatarSelection(),
)

onBeforeUnmount(clearAvatarSelection)
</script>

<style scoped>
.avatar-update-content { display: grid; grid-template-columns: 140px minmax(0, 1fr); gap: 22px; align-items: center; padding-top: 8px; }
.avatar-update-preview { display: flex; flex-direction: column; align-items: center; gap: 10px; text-align: center; }
.avatar-update-preview :deep(.ant-avatar) { border: 1px solid var(--proto-line); background: rgba(255,255,255,.7); color: var(--proto-ink); font-size: 32px; }
.avatar-update-preview strong { max-width: 14ch; overflow-wrap: anywhere; font-size: 13px; }
.avatar-update-form { min-height: 210px; display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
.avatar-file-name { width: 100%; overflow: hidden; color: var(--proto-muted); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.avatar-preview-label { color: var(--proto-muted); font-size: 11px; }
.avatar-new-preview { width: 100%; min-height: 128px; display: flex; flex: 1; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 1px dashed var(--proto-line); background: rgba(255,255,255,.28); }
.avatar-new-preview :deep(.ant-avatar) { border: 1px solid var(--proto-line); background: rgba(255,255,255,.7); color: var(--proto-ink); font-size: 32px; }
.avatar-preview-empty { color: var(--proto-muted); font-size: 24px; line-height: 1; }
@media (max-width: 520px) {
  .avatar-update-content { grid-template-columns: 1fr; }
  .avatar-update-form { min-height: 180px; }
}
</style>
