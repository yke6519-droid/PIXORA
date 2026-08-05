<template>
  <a-modal
    v-model:open="visible"
    :title="props.mode === 'reupload' ? '重新上传图片' : '上传图片'"
    width="700px"
    :confirm-loading="uploading"
    @ok="handleUpload"
    @cancel="handleCancelUpload"
    :ok-text="props.mode === 'reupload' ? '确认重新上传' : '确认上传'"
    cancel-text="取消"
  >
    <a-form
      :model="uploadForm"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 20 }"
      class="upload-form"
    >
      <!-- 上传方式选择 -->
      <a-form-item label="上传方式">
        <a-radio-group v-model:value="uploadType">
          <a-radio value="file">本地上传</a-radio>
          <a-radio value="url">URL 上传</a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 上传目标选择：公共图库 / 私有空间 -->
      <a-form-item label="上传到">
        <a-radio-group v-model:value="targetSpace">
          <a-radio value="public">
            <FolderOpenOutlined />
            公共图库
          </a-radio>
          <a-radio value="space" :disabled="!hasSpace">
            <LockOutlined />
            私有空间
            <span v-if="!hasSpace" style="color: #999; font-size: 12px; margin-left: 4px">
              （未创建空间）
            </span>
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="图片名称">
        <a-input
          v-model:value="uploadForm.name"
          placeholder="请输入图片名称（可选，默认使用文件名）"
          :maxLength="50"
          show-count
        />
      </a-form-item>

      <!-- 图片简介 -->
      <a-form-item label="图片简介">
        <a-textarea
          v-model:value="uploadForm.introduction"
          placeholder="请输入图片简介（可选）"
          :rows="3"
          :maxLength="200"
          show-count
        />
      </a-form-item>

      <!-- 分类 -->
      <a-form-item label="分类">
        <a-select
          v-model:value="uploadForm.category"
          placeholder="请选择分类（可选）"
          allow-clear
        >
          <a-select-option v-for="cat in categoryList" :key="cat" :value="cat">
            {{ cat }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <!-- 标签 -->
      <a-form-item label="标签">
        <a-select
          v-model:value="uploadForm.tags"
          mode="tags"
          placeholder="输入标签后回车添加（可选）"
          allow-clear
        >
          <a-select-option v-for="tag in tagList" :key="tag" :value="tag">
            {{ tag }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <!-- 原图片预览（重新上传模式） -->
      <a-form-item v-if="props.mode === 'reupload' && props.originalPictureUrl" label="原图片">
        <div class="original-image-preview">
          <img :src="props.originalPictureUrl" alt="原图片" />
          <p class="preview-tip">不上传则保持原图，仅修改信息</p>
        </div>
      </a-form-item>

      <!-- 本地上传区域 -->
      <a-form-item v-if="uploadType === 'file'" :label="props.mode === 'reupload' ? '上传新图片（可选）' : '选择图片'" :required="props.mode !== 'reupload'">
        <a-upload-dragger
          v-model:fileList="fileList"
          :custom-request="customRequest"
          :before-upload="beforeUpload"
          :multiple="false"
          :max-count="1"
          accept="image/*"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">
            {{ props.mode === 'reupload' ? '点击或拖拽新图片到此区域上传（可选）' : '点击或拖拽图片到此区域上传' }}
          </p>
          <p class="ant-upload-hint">
            {{ props.mode === 'reupload' ? '不上传则保持原图，仅修改信息；上传则用新图替换原图' : '支持 JPG、PNG、WEBP，最大 5MB' }}
          </p>
        </a-upload-dragger>
      </a-form-item>

      <!-- URL 上传区域 -->
      <a-form-item v-else label="图片 URL" :required="props.mode !== 'reupload'">
        <a-input
          v-model:value="imageUrl"
          placeholder="请输入图片 URL（如：https://example.com/image.jpg ）"
          allow-clear
        />
        <p class="url-hint">支持 HTTP 或 HTTPS 协议的图片链接</p>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined, FolderOpenOutlined, LockOutlined } from '@ant-design/icons-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import { uploadPicUsingPost } from '../api/pictureController'
import { useLoginUserStore } from '../stores/useLoginUserStore'

// 定义 Props
interface Props {
  open: boolean
  categoryList?: string[]
  tagList?: string[]
  initialData?: {
    id?: number
    name?: string
    introduction?: string
    category?: string
    tags?: string[]
    spaceId?: number
  }
  originalPictureUrl?: string
  mode?: 'upload' | 'reupload'
  /** 默认上传到哪个空间（由父组件传入） */
  defaultSpaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  categoryList: () => [],
  tagList: () => [],
  initialData: undefined,
  originalPictureUrl: undefined,
  mode: 'upload',
  defaultSpaceId: undefined
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
  cancel: []
}>()

const loginUserStore = useLoginUserStore()

// 弹窗显示状态
const visible = ref(props.open)

// 监听 props.open 变化
watch(() => props.open, (newVal) => {
  visible.value = newVal
  if (newVal && props.initialData) {
    uploadForm.value = {
      name: props.initialData.name || '',
      introduction: props.initialData.introduction || '',
      category: props.initialData.category,
      tags: props.initialData.tags || []
    }
    // 如果原图片在空间中，默认选中私有空间
    if (props.initialData.spaceId && props.initialData.spaceId > 0) {
      targetSpace.value = 'space'
    } else {
      targetSpace.value = 'public'
    }
  } else if (newVal) {
    // 新上传时，如果有 defaultSpaceId 则默认选空间
    targetSpace.value = props.defaultSpaceId ? 'space' : 'public'
  }
})

watch(() => visible.value, (newVal) => {
  emit('update:open', newVal)
})

// 上传状态
const uploading = ref(false)

// 上传方式
const uploadType = ref<'file' | 'url'>('file')

// 上传目标：public（公共图库）或 space（私有空间）
const targetSpace = ref<'public' | 'space'>('public')

// 当前用户是否有空间
const hasSpace = computed(() => {
  return !!loginUserStore.loginUser?.spaceId
})

// 文件列表
const fileList = ref<UploadFile[]>([])

// 图片 URL
const imageUrl = ref('')

// 上传表单
const uploadForm = ref({
  name: '',
  introduction: '',
  category: undefined as string | undefined,
  tags: [] as string[]
})

/** 自定义上传请求 */
const customRequest = (options: any) => {
  options.onSuccess?.()
}

/** 上传前校验 */
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  const isWithinLimit = file.size <= 5 * 1024 * 1024
  if (!isWithinLimit) {
    message.error('图片大小不能超过 5MB！')
    return false
  }
  if (!uploadForm.value.name) {
    const fileName = file.name.replace(/\.[^.]+$/, '')
    uploadForm.value.name = fileName
  }
  return true
}

/** 确认上传 */
const handleUpload = async () => {
  let file: File | undefined
  let url: string | undefined
  let hasInput = false

  if (uploadType.value === 'file') {
    const hasNewFile = fileList.value.length > 0 && fileList.value[0]?.originFileObj
    file = hasNewFile ? fileList.value[0].originFileObj as File : undefined
    hasInput = !!file
    if (props.mode !== 'reupload' && !hasInput) {
      message.warning('请先选择要上传的图片')
      return
    }
  } else {
    const urlValue = imageUrl.value.trim()
    url = urlValue
    hasInput = !!url
    if (props.mode !== 'reupload' && !hasInput) {
      message.warning('请输入图片 URL')
      return
    }
    if (hasInput) {
      const urlPattern = /^https?:\/\/.+/
      if (!urlPattern.test(urlValue)) {
        message.error('URL 格式不正确，必须以 http:// 或 https:// 开头')
        return
      }
    }
  }

  if (props.mode === 'reupload' && !props.initialData?.id) {
    message.error('缺少原图片信息')
    return
  }

  if (props.mode === 'reupload' && !hasInput) {
    file = undefined
    url = undefined
  }

  uploading.value = true
  try {
    // 确定 spaceId：传到私有空间则用用户的 spaceId，否则不传（后端默认 0）
    const spaceId = targetSpace.value === 'space' ? loginUserStore.loginUser?.spaceId : undefined

    const params = {
      id: props.initialData?.id,
      name: uploadForm.value.name,
      introduction: uploadForm.value.introduction,
      tags: uploadForm.value.tags,
      category: uploadForm.value.category,
      url: url,
      spaceId: targetSpace.value === 'space' ? loginUserStore.loginUser?.spaceId : null,
    }

    const res: any = await uploadPicUsingPost({},params,file)
    if (res.data.code === 200) {
      let successMsg = '上传成功！'
      if (props.mode === 'reupload') {
        successMsg = !hasInput ? '修改成功！' : '重新上传成功！'
      } else {
        successMsg = targetSpace.value === 'space' ? '已上传到私有空间！' : '已上传到公共图库！'
      }
      message.success(successMsg)
      visible.value = false
      resetUploadForm()
      emit('success')
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

/** 取消上传 */
const handleCancelUpload = () => {
  resetUploadForm()
  emit('cancel')
}

/** 重置上传表单 */
const resetUploadForm = () => {
  uploadType.value = 'file'
  fileList.value = []
  imageUrl.value = ''
  targetSpace.value = 'public'
  uploadForm.value = {
    name: '',
    introduction: '',
    category: undefined,
    tags: []
  }
}

defineExpose({
  resetUploadForm
})
</script>

<style scoped>
.upload-form {
  padding: 16px 0 0;
}

.original-image-preview {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
  text-align: center;
}

.original-image-preview img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.preview-tip {
  color: #ff4d4f;
  font-size: 13px;
  margin: 0;
}

.url-hint {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
  margin-bottom: 0;
}
</style>
