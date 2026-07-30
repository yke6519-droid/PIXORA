<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    :confirm-loading="saving"
    @ok="handleSave"
    @cancel="handleCancel"
    width="600px"
  >
    <a-form
      :model="editForm"
      :label-col="{ span: isAdmin ? 6 : 4 }"
      :wrapper-col="{ span: isAdmin ? 16 : 20 }"
      class="edit-form"
    >
      <!-- 头像上传 -->
      <a-form-item label="头像">
        <div class="avatar-upload-section">
          <a-upload
            name="avatar"
            list-type="picture-card"
            :show-upload-list="false"
            :custom-request="customUpload"
            :before-upload="beforeUpload"
            accept="image/*"
          >
            <div class="avatar-preview" v-if="editForm.avatarurl">
              <img :src="editForm.avatarurl" alt="avatar" />
              <div class="avatar-mask">
                <CameraOutlined />
                <span>更换头像</span>
              </div>
            </div>
            <div v-else class="upload-placeholder">
              <PlusOutlined />
              <div class="upload-text">上传头像</div>
            </div>
          </a-upload>
        </div>
      </a-form-item>

      <!-- 用户昵称 -->
      <a-form-item
        label="昵称"
        :rules="[{ required: true, message: '昵称不得为空' }, { max: 20, message: '昵称长度不得大于20位' }]"
      >
        <a-input
          v-model:value="editForm.username"
          placeholder="请输入昵称"
          :maxLength="20"
          show-count
        />
      </a-form-item>

      <!-- 性别 -->
      <a-form-item label="性别">
        <a-radio-group v-model:value="editForm.gender">
          <a-radio :value="0">
            <ManOutlined /> 男
          </a-radio>
          <a-radio :value="1">
            <WomanOutlined /> 女
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 手机号 -->
      <a-form-item
        label="手机号"
        :rules="[
          { required: isAdmin, message: '手机号不得为空' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入 11 位有效手机号' }
        ]"
      >
        <a-input v-model:value="editForm.phone" placeholder="请输入手机号">
          <template #prefix>
            <PhoneOutlined />
          </template>
        </a-input>
      </a-form-item>

      <!-- 邮箱 -->
      <a-form-item
        label="邮箱"
        :rules="[
          { type: 'email', message: '请输入有效的邮箱地址' },
          { max: 50, message: '邮箱长度不得大于50位' }
        ]"
      >
        <a-input v-model:value="editForm.email" placeholder="请输入邮箱">
          <template #prefix>
            <MailOutlined />
          </template>
        </a-input>
      </a-form-item>

      <!-- 个人简介 -->
      <a-form-item
        label="个人简介"
        :rules="[{ max: 200, message: '个人简介字数上限为200字' }]"
      >
        <a-textarea
          v-model:value="editForm.profile"
          placeholder="介绍一下你自己..."
          :rows="4"
          :maxLength="200"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import {
  CameraOutlined,
  PlusOutlined,
  ManOutlined,
  WomanOutlined,
  PhoneOutlined,
  MailOutlined
} from '@ant-design/icons-vue'
import { avatarUploadUsingPost } from '../api/fileController'

// 定义 Props
interface Props {
  open: boolean
  title?: string
  userData?: API.User
  isAdmin?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '编辑资料',
  userData: undefined,
  isAdmin: false
})

// 定义 Emits
const emit = defineEmits<{
  'update:open': [value: boolean]
  save: [data: API.UpdateSelfRequest | API.UserUpdateRequest]
  cancel: []
}>()

// 弹窗显示状态
const visible = ref(props.open)

// 监听 props.open 变化
watch(() => props.open, (newVal) => {
  visible.value = newVal
})

// 监听 visible 变化，同步回父组件
watch(() => visible.value, (newVal) => {
  emit('update:open', newVal)
})

// 保存状态
const saving = ref(false)
const uploading = ref(false)

// 编辑表单
const editForm = ref<API.UpdateSelfRequest>({
  id: undefined,
  username: '',
  avatarurl: '',
  gender: 0,
  phone: '',
  email: '',
  profile: ''
})

/**
 * 初始化表单
 */
const initForm = (userData: API.UserVO) => {
  editForm.value = {
    id: userData.id,
    username: userData.username || '',
    avatarurl: userData.avatarurl || '',
    gender: userData.gender ?? 0,
    phone: userData.phone || '',
    email: userData.email || '',
    profile: userData.profile || ''
  }
}

// 监听 userData 变化，初始化表单
watch(() => props.userData, (newVal) => {
  if (newVal) {
    initForm(newVal)
  }
}, { immediate: true })

/**
 * 头像上传前校验
 */
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  // 限制大小 5MB
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB！')
    return false
  }
  return true
}

/**
 * 自定义头像上传
 */
const customUpload = async (options: any) => {
  const { file, onSuccess, onError } = options
  uploading.value = true
  try {
    const res = await avatarUploadUsingPost({}, file)
    const data = res.data as any
    if (data.code === 200) {
      message.success('头像上传成功！')
      editForm.value.avatarurl = data.data
      onSuccess?.(res)
    } else {
      message.error('上传失败：' + data.message)
      onError?.('上传失败')
    }
  } catch (err) {
    message.error('上传异常')
    onError?.(err)
  } finally {
    uploading.value = false
  }
}

/**
 * 保存
 */
const handleSave = () => {
  // 表单验证
  if (!editForm.value.username?.trim()) {
    message.error('请输入昵称')
    return
  }

  // 管理员模式下验证手机号
  if (props.isAdmin && !editForm.value.phone?.trim()) {
    message.error('请输入手机号')
    return
  }

  // 手机号格式验证
  if (editForm.value.phone && !/^1[3-9]\d{9}$/.test(editForm.value.phone)) {
    message.error('请输入 11 位有效手机号')
    return
  }

  // 邮箱格式验证
  if (editForm.value.email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(editForm.value.email)) {
      message.error('请输入有效的邮箱地址')
      return
    }
  }

  saving.value = true
  try {
    emit('save', { ...editForm.value })
  } finally {
    saving.value = false
  }
}

/**
 * 取消
 */
const handleCancel = () => {
  visible.value = false
  emit('cancel')
}

// 暴露方法给父组件
defineExpose({
  initForm
})
</script>

<style scoped>
.edit-form {
  padding: 16px 0;
}

.avatar-upload-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-upload-section :deep(.ant-upload) {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  padding: 0;
  border: 2px dashed #d9d9d9;
}

.avatar-upload-section :deep(.ant-upload:hover) {
  border-color: #1890ff;
}

.avatar-preview {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-preview:hover .avatar-mask {
  opacity: 1;
}

.avatar-mask :deep(.anticon) {
  font-size: 24px;
  margin-bottom: 4px;
}

.upload-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.upload-placeholder :deep(.anticon) {
  font-size: 32px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 12px;
}

.avatar-url-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.url-label {
  color: #999;
  font-size: 13px;
  white-space: nowrap;
}
</style>
