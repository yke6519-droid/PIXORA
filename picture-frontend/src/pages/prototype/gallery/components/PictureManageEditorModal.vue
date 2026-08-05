<template>
  <a-modal
    :open="open"
    :title="mode === 'reupload' ? '重新上传图片' : '编辑图片信息'"
    :width="720"
    :confirm-loading="submitting"
    :ok-text="mode === 'reupload' ? '提交重新审核' : '保存修改'"
    cancel-text="取消"
    destroy-on-close
    @ok="submit"
    @cancel="close"
  >
    <a-alert
      v-if="mode === 'reupload'"
      class="editor-notice"
      type="info"
      show-icon
      message="重新上传会替换原图，并按当前用户身份重新进入审核流程。"
    />

    <section v-if="mode === 'reupload'" class="editor-source">
      <div class="editor-source-head">
        <strong>选择新图片</strong>
        <a-radio-group v-model:value="sourceMode" button-style="solid" size="small">
          <a-radio-button value="file">本地文件</a-radio-button>
          <a-radio-button value="url">网络地址</a-radio-button>
        </a-radio-group>
      </div>

      <a-upload-dragger
        v-if="sourceMode === 'file'"
        :file-list="fileList"
        :before-upload="beforeUpload"
        :max-count="1"
        :show-upload-list="true"
        accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
        @remove="removeFile"
      >
        <p class="editor-upload-title">点击或拖入替换图片</p>
        <p class="editor-upload-copy">JPG、PNG、WEBP · 最大 5MB</p>
      </a-upload-dragger>

      <a-input
        v-else
        v-model:value="imageUrl"
        allow-clear
        placeholder="https://example.com/picture.jpg"
      />

      <div v-if="previewUrl" class="editor-preview">
        <img :src="previewUrl" alt="重新上传图片预览" />
      </div>
    </section>

    <a-form :model="form" layout="vertical" class="proto-form editor-form">
      <a-form-item label="图片名称" required>
        <a-input
          v-model:value="form.name"
          :maxlength="50"
          show-count
          placeholder="请输入图片名称"
        />
      </a-form-item>

      <div class="editor-two-col">
        <a-form-item label="图片分类">
          <a-select
            v-model:value="form.category"
            :options="categoryOptions"
            allow-clear
            show-search
            placeholder="选择分类"
          />
        </a-form-item>

        <a-form-item label="图片标签">
          <a-select
            v-model:value="form.tags"
            mode="tags"
            :options="tagOptions"
            :max-tag-count="3"
            allow-clear
            placeholder="选择或输入标签"
          />
        </a-form-item>
      </div>

      <a-form-item label="图片简介">
        <a-textarea
          v-model:value="form.introduction"
          :maxlength="200"
          :rows="3"
          show-count
          placeholder="简单说明图片内容或来源"
        />
      </a-form-item>

      <a-alert
        v-if="submitError"
        type="error"
        show-icon
        :message="submitError"
      />
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Upload, message } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import {
  editPictureUsingPost,
  uploadPicUsingPost,
} from '../../../../api/pictureController'

type EditorMode = 'edit' | 'reupload'
type SourceMode = 'file' | 'url'

const props = defineProps<{
  open: boolean
  mode: EditorMode
  picture: API.PictureVO | null
  categories: string[]
  tags: string[]
}>()

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'success'): void
}>()

const submitting = ref(false)
const submitError = ref('')
const sourceMode = ref<SourceMode>('file')
const selectedFile = ref<File | null>(null)
const fileList = ref<UploadFile[]>([])
const imageUrl = ref('')
const localPreviewUrl = ref('')

const form = reactive({
  name: '',
  category: undefined as string | undefined,
  tags: [] as string[],
  introduction: '',
})

const categoryOptions = computed(() =>
  props.categories.map((item) => ({ label: item, value: item })),
)
const tagOptions = computed(() =>
  props.tags.map((item) => ({ label: item, value: item })),
)
const previewUrl = computed(() =>
  sourceMode.value === 'file'
    ? localPreviewUrl.value || props.picture?.thumbnailUrl || props.picture?.url || ''
    : imageUrl.value.trim() || props.picture?.thumbnailUrl || props.picture?.url || '',
)

/**
 * 大小和格式校验与后端上传入口保持一致，大写扩展名也会被正常识别。
 */
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const suffix = file.name.split('.').pop()?.toLowerCase()
  const allowedSuffixes = ['jpg', 'jpeg', 'png', 'webp']
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']

  if (!allowedTypes.includes(file.type) && !allowedSuffixes.includes(suffix || '')) {
    message.error('仅支持 JPG、PNG、WEBP 图片')
    return Upload.LIST_IGNORE
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return Upload.LIST_IGNORE
  }

  selectedFile.value = file
  fileList.value = [{
    uid: file.uid,
    name: file.name,
    size: file.size,
    type: file.type,
    status: 'done',
    originFileObj: file,
  }]
  createPreview(file)
  submitError.value = ''
  return false
}

function resetForm() {
  revokePreview()
  selectedFile.value = null
  fileList.value = []
  imageUrl.value = ''
  sourceMode.value = 'file'
  submitError.value = ''
  form.name = props.picture?.name || ''
  form.category = props.picture?.category || undefined
  form.tags = [...(props.picture?.tags || [])]
  form.introduction = props.picture?.introduction || ''
}

function createPreview(file: File) {
  revokePreview()
  localPreviewUrl.value = URL.createObjectURL(file)
}

function revokePreview() {
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
    localPreviewUrl.value = ''
  }
}

function removeFile() {
  selectedFile.value = null
  fileList.value = []
  revokePreview()
  return true
}

function isHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

function close() {
  emit('update:open', false)
}

async function submit() {
  const pictureId = props.picture?.id
  if (!pictureId) {
    submitError.value = '图片 ID 缺失，请刷新列表后重试'
    return
  }
  if (!form.name.trim()) {
    submitError.value = '请输入图片名称'
    return
  }
  if (
    props.mode === 'reupload' &&
    ((sourceMode.value === 'file' && !selectedFile.value) ||
      (sourceMode.value === 'url' && !isHttpUrl(imageUrl.value.trim())))
  ) {
    submitError.value =
      sourceMode.value === 'file'
        ? '请选择要重新上传的图片'
        : '请输入有效的 HTTP 或 HTTPS 图片地址'
    return
  }

  submitting.value = true
  submitError.value = ''
  const metadata = {
    id: pictureId,
    name: form.name.trim(),
    category: form.category,
    tags: [...new Set(form.tags.map((item) => item.trim()).filter(Boolean))],
    introduction: form.introduction.trim(),
  }

  try {
    const res: any = props.mode === 'edit'
      ? await editPictureUsingPost(metadata)
      : await uploadPicUsingPost(
          {},
          {
            ...metadata,
            url: sourceMode.value === 'url' ? imageUrl.value.trim() : undefined,
          },
          sourceMode.value === 'file' ? selectedFile.value || undefined : undefined,
        )

    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '保存失败')
    }

    message.success(
      props.mode === 'reupload'
        ? '重新上传成功，图片已进入审核流程'
        : '图片信息已更新',
    )
    emit('success')
    close()
  } catch (error: any) {
    submitError.value =
      error?.response?.data?.message || error?.message || '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

watch(
  () => [props.open, props.picture?.id, props.mode],
  ([open]) => {
    if (open) resetForm()
  },
)

onBeforeUnmount(revokePreview)
</script>

<style scoped>
.editor-notice { margin-bottom: 16px; }
.editor-source { margin-bottom: 18px; padding: 16px; border: 1px solid var(--proto-line); border-radius: 8px; background: var(--proto-paper-deep); }
.editor-source-head { margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.editor-source-head strong { font-size: 14px; }
.editor-upload-title { margin: 0 0 4px; color: var(--proto-ink); font-weight: 700; }
.editor-upload-copy { margin: 0; color: var(--proto-muted); font-size: 11px; }
.editor-preview { height: 150px; margin-top: 12px; overflow: hidden; border-radius: 6px; background: var(--proto-paper); }
.editor-preview img { width: 100%; height: 100%; display: block; object-fit: contain; }
.editor-form { padding-top: 2px; }
.editor-two-col { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
@media (max-width: 620px) {
  .editor-two-col { grid-template-columns: 1fr; gap: 0; }
  .editor-source-head { align-items: flex-start; flex-direction: column; }
}
</style>
