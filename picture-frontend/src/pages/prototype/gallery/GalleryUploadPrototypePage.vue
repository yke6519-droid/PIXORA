<template>
  <div class="upload-prototype">
    <a-alert
      v-if="pageNotice"
      class="upload-page-alert"
      type="warning"
      show-icon
      :message="pageNotice"
    />

    <!-- 去掉无实际操作价值的大标题，保留流程定位，避免用户失去当前步骤感知。 -->
    <nav class="upload-progress-row" aria-label="上传步骤">
      <div class="upload-steps">
        <div class="upload-step is-active">
          <span>1</span>
          <strong>选择与预览</strong>
        </div>
        <div class="upload-step">
          <span>2</span>
          <strong>填写信息</strong>
        </div>
        <div class="upload-step">
          <span>3</span>
          <strong>完成</strong>
        </div>
      </div>
    </nav>

    <section class="upload-layout proto-section">
      <div class="upload-source-panel proto-surface proto-rounded">
        <div class="source-heading">
          <div>
            <h2>选择图片</h2>
            <p>支持 JPG、PNG、WEBP · 单张最大 5MB</p>
          </div>
          <div class="source-mode-control">
            <span>来源</span>
            <a-radio-group
              v-model:value="sourceMode"
              button-style="solid"
              size="small"
              :disabled="submitting"
            >
              <a-radio-button value="file">本地文件</a-radio-button>
              <a-radio-button value="url">网络地址</a-radio-button>
            </a-radio-group>
          </div>
        </div>

        <div v-if="sourceMode === 'file'" class="source-control">
          <a-upload-dragger
            :file-list="fileList"
            :before-upload="beforeUpload"
            :multiple="true"
            :show-upload-list="false"
            :disabled="submitting"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
          >
            <p class="ant-upload-drag-icon"><InboxOutlined /></p>
            <p class="upload-drag-title">点击或拖入图片</p>
            <p class="upload-drag-copy">支持多选 · 单张最大 5MB · 批量合计最大 30MB</p>
          </a-upload-dragger>
        </div>

        <div v-else class="source-control url-control">
          <label for="picture-url">图片 URL</label>
          <a-input
            id="picture-url"
            v-model:value="imageUrl"
            :disabled="submitting"
            placeholder="https://example.com/picture.jpg"
            allow-clear
          >
            <template #prefix><LinkOutlined /></template>
          </a-input>
          <p>仅支持 HTTP 或 HTTPS 地址，最终格式和大小仍由后端校验。</p>
        </div>

        <div class="upload-preview" :class="{ empty: !displayPreview }">
          <img
            v-if="displayPreview"
            :src="displayPreview"
            :alt="activePreviewAlt"
            @error="handlePreviewError"
          />
          <div v-else class="preview-empty">
            <PictureOutlined />
            <strong>{{ previewEmptyTitle }}</strong>
            <span>{{ previewEmptyDescription }}</span>
          </div>
          <button
            v-if="sourceMode === 'file' && selectedFiles.length > 1"
            type="button"
            class="preview-arrow preview-arrow-prev"
            aria-label="上一张图片"
            @click="selectPreviousPreview"
          >
            <LeftOutlined />
          </button>
          <button
            v-if="sourceMode === 'file' && selectedFiles.length > 1"
            type="button"
            class="preview-arrow preview-arrow-next"
            aria-label="下一张图片"
            @click="selectNextPreview"
          >
            <RightOutlined />
          </button>
          <span v-if="sourceReady" class="preview-count">{{ previewCountLabel }}</span>
        </div>

        <!-- 快捷上传允许继续添加多张图片；点击缩略图只切换预览，不改变公共信息表单。 -->
        <div v-if="sourceMode === 'file'" class="preview-strip" aria-label="已选择的图片">
          <button
            v-if="selectedFiles.length"
            v-for="(file, index) in selectedFiles"
            :key="`${file.name}-${index}`"
            type="button"
            class="preview-thumb"
            :class="{ active: index === activeFileIndex }"
            :aria-label="`预览第 ${index + 1} 张：${file.name}`"
            :aria-current="index === activeFileIndex ? 'true' : undefined"
            @click="selectPreview(index)"
          >
            <img :src="localPreviewUrls[index]" :alt="file.name" />
            <span>{{ index + 1 }}</span>
          </button>
          <a-upload
            class="preview-add-upload"
            :before-upload="beforeUpload"
            :multiple="true"
            :show-upload-list="false"
            :disabled="submitting"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
          >
            <span class="preview-add-icon"><PlusOutlined /></span>
            <span>添加</span>
          </a-upload>
          <span class="preview-strip-hint">可继续拖入或点击添加</span>
        </div>

        <div v-if="selectedFiles.length" class="selected-source">
          <span>
            <strong>已选择 {{ selectedFiles.length }} 张图片</strong>
            <small>{{ formatFileSize(selectedFiles.reduce((total, file) => total + file.size, 0)) }}</small>
          </span>
          <a-button
            type="text"
            size="small"
            :disabled="submitting"
            aria-label="移除已选择的图片"
            @click="clearSelectedFile"
          >
            <DeleteOutlined />
          </a-button>
        </div>
      </div>

      <div class="upload-form-panel proto-surface proto-rounded">
        <div class="form-panel-heading">
          <h2>图片信息</h2>
          <div class="form-panel-status" aria-label="当前上传审核状态">
            <a-tag class="upload-mode-tag">快捷上传</a-tag>
            <a-tag class="upload-audit-tag">{{ auditStatusText }}</a-tag>
          </div>
        </div>

        <p v-if="selectedFiles.length > 1" class="batch-metadata-hint">
          当前为快捷上传，下面的信息会应用到本批 {{ selectedFiles.length }} 张图片。
        </p>

        <a-form
          :model="form"
          layout="vertical"
          class="proto-form upload-form"
          :disabled="submitting || authChecking"
          @finish="submitUpload"
        >
          <a-form-item label="上传位置" name="target">
            <a-radio-group v-model:value="form.target" class="target-choice-group">
              <a-radio-button value="public">
                <span class="target-choice-icon"><GlobalOutlined /></span>
                <span class="target-choice-copy">
                  <strong>公共图库</strong>
                  <small>所有人可见</small>
                </span>
              </a-radio-button>
              <a-radio-button value="space" :disabled="!hasPrivateSpace">
                <span class="target-choice-icon"><LockOutlined /></span>
                <span class="target-choice-copy">
                  <strong>私人空间</strong>
                  <small>仅自己可见</small>
                </span>
              </a-radio-button>
            </a-radio-group>
            <p v-if="!hasPrivateSpace" class="field-hint">当前账号还没有私人空间</p>
          </a-form-item>

          <a-form-item label="图片名称" name="name">
            <a-input
              v-model:value="form.name"
              :maxlength="50"
              show-count
              :placeholder="selectedFiles.length > 1 ? '留空时使用每张图片的文件名' : '留空时使用文件名'"
            />
          </a-form-item>

          <a-form-item label="图片分类" name="category">
            <a-select
              v-model:value="form.category"
              :options="categoryOptions"
              :loading="categoryLoading"
              allow-clear
              show-search
              placeholder="选择分类"
            />
          </a-form-item>

          <a-form-item label="图片标签" name="tags">
            <a-select
              v-model:value="form.tags"
              mode="tags"
              :options="tagOptions"
              :loading="categoryLoading"
              :max-tag-count="3"
              allow-clear
              placeholder="选择或输入标签后回车"
            />
          </a-form-item>

          <a-form-item label="图片简介" name="introduction">
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
            class="upload-submit-alert"
            type="error"
            show-icon
            :message="submitError"
          />

          <div class="upload-form-foot">
            <div class="submit-contract">
              <span>目标：{{ form.target === 'space' ? '私人空间' : '公共图库' }}</span>
              <span>状态：{{ auditStatusText }}</span>
            </div>
            <div class="upload-form-actions">
              <a-button class="upload-cancel-button" :disabled="submitting" @click="cancelUpload">
                取消
              </a-button>
              <a-button
                html-type="submit"
                class="proto-button acid-button"
                type="primary"
                :loading="submitting"
                :disabled="authChecking"
              >
                {{ submitting ? '正在上传' : '上传图片' }}
              </a-button>
            </div>
          </div>
        </a-form>
      </div>
    </section>

    <!-- 后端返回批量结果后留在当前页，便于用户核对成功与失败项。 -->
    <section v-if="hasUploadResult" class="upload-result-section proto-section">
      <div class="upload-result-panel proto-surface proto-rounded">
        <div class="upload-result-heading">
          <div>
            <span class="proto-eyebrow">upload result</span>
            <h2>本次上传结果</h2>
          </div>
          <a-button class="proto-button ghost-button" @click="openPictureManage">
            查看图片管理
          </a-button>
        </div>

        <div class="upload-result-stats" aria-label="图片上传结果统计">
          <div class="upload-result-stat">
            <span>总数</span>
            <strong>{{ uploadTotalCount }}</strong>
          </div>
          <div class="upload-result-stat is-success">
            <span>上传成功</span>
            <strong>{{ uploadSuccessCount }}</strong>
          </div>
          <div class="upload-result-stat is-failed">
            <span>上传失败</span>
            <strong>{{ uploadFailCount }}</strong>
          </div>
        </div>

        <a-alert
          v-if="uploadFailCount"
          class="upload-result-alert"
          type="warning"
          show-icon
          :message="`有 ${uploadFailCount} 张图片未上传成功，请查看失败原因`"
        />

        <section v-if="uploadedPictures.length" class="upload-success-list">
          <div class="upload-result-subheading">
            <h3>上传成功的图片</h3>
            <span>{{ uploadSuccessCount }} 张</span>
          </div>
          <div class="upload-success-grid">
            <article v-for="picture in uploadedPictures" :key="String(picture.id)" class="upload-success-item">
              <div class="upload-success-media">
                <img
                  v-if="picture.url"
                  :src="picture.url"
                  :alt="picture.name || '上传成功的图片'"
                />
                <span v-else>暂无预览</span>
              </div>
              <div class="upload-success-meta">
                <strong>{{ picture.name || '未命名图片' }}</strong>
                <small>{{ formatFileSize(Number(picture.picsize || 0)) }}</small>
              </div>
            </article>
          </div>
        </section>

        <section v-if="failedUploads.length" class="upload-failed-list">
          <div class="upload-result-subheading">
            <h3>未上传成功的图片</h3>
            <span>{{ uploadFailCount }} 张</span>
          </div>
          <ul>
            <li v-for="(failed, index) in failedUploads" :key="`${failed.fileName || 'file'}-${index}`">
              <div>
                <strong>{{ failed.fileName || '未命名文件' }}</strong>
                <small>{{ formatFileSize(Number(failed.size || 0)) }}</small>
              </div>
              <span>{{ failed.message || '上传失败，请稍后重试' }}</span>
            </li>
          </ul>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Upload, message } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import {
  DeleteOutlined,
  GlobalOutlined,
  InboxOutlined,
  LinkOutlined,
  LeftOutlined,
  LockOutlined,
  PlusOutlined,
  PictureOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  listPictureCategory,
  uploadPic,
} from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'

type UploadSourceMode = 'file' | 'url'
type UploadTarget = 'public' | 'space'

const MAX_SINGLE_FILE_SIZE = 5 * 1024 * 1024
const MAX_BATCH_TOTAL_SIZE = 30 * 1024 * 1024

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const sourceMode = ref<UploadSourceMode>('file')
const fileList = ref<UploadFile[]>([])
const selectedFiles = ref<File[]>([])
const localPreviewUrls = ref<string[]>([])
const activeFileIndex = ref(0)
const autoGeneratedName = ref('')
const imageUrl = ref('')
const urlPreviewFailed = ref(false)
const categoryLoading = ref(false)
const authChecking = ref(true)
const submitting = ref(false)
const submitError = ref('')
const pageNotice = ref('')
const uploadResult = ref<API.PictureUploadVO | null>(null)
const categories = ref<string[]>([])
const tags = ref<string[]>([])

const form = reactive({
  name: '',
  category: undefined as string | undefined,
  target: 'public' as UploadTarget,
  tags: [] as string[],
  introduction: '',
})

const categoryOptions = computed(() =>
  categories.value.map((item) => ({ label: item, value: item })),
)

const tagOptions = computed(() =>
  tags.value.map((item) => ({ label: item, value: item })),
)

const hasUploadResult = computed(() => uploadResult.value !== null)
const uploadedPictures = computed(() => uploadResult.value?.successPictureList || [])
const failedUploads = computed(() => uploadResult.value?.failPictureList || [])
const uploadTotalCount = computed(() => Number(uploadResult.value?.totalCount || 0))
const uploadSuccessCount = computed(() => Number(uploadResult.value?.successCount || uploadedPictures.value.length))
const uploadFailCount = computed(() => Number(uploadResult.value?.failCount || failedUploads.value.length))

const hasPrivateSpace = computed(() => {
  const spaceId = loginUserStore.loginUser?.spaceId
  return Boolean(spaceId && String(spaceId) !== '0')
})

const isAdmin = computed(() => loginUserStore.loginUser?.userLevel === 'admin')
const auditStatusText = computed(() => (isAdmin.value ? '自动通过' : '等待审核'))

const validUrl = computed(() => isHttpUrl(imageUrl.value.trim()))
const activeFile = computed(() => selectedFiles.value[activeFileIndex.value])
const activeLocalPreviewUrl = computed(() => localPreviewUrls.value[activeFileIndex.value] || '')
const displayPreview = computed(() => {
  if (sourceMode.value === 'file') return activeLocalPreviewUrl.value
  return validUrl.value && !urlPreviewFailed.value ? imageUrl.value.trim() : ''
})
const activePreviewAlt = computed(() => activeFile.value?.name || '待上传图片预览')
const previewCountLabel = computed(() => {
  if (sourceMode.value === 'url') return '1 / 1'
  return selectedFiles.value.length ? `${activeFileIndex.value + 1} / ${selectedFiles.value.length}` : '0 / 0'
})

const sourceReady = computed(() =>
  sourceMode.value === 'file' ? selectedFiles.value.length > 0 : validUrl.value,
)

const previewEmptyTitle = computed(() => {
  if (sourceMode.value === 'url' && imageUrl.value && !validUrl.value) return 'URL 格式不正确'
  if (sourceMode.value === 'url' && urlPreviewFailed.value) return '预览加载失败'
  return '图片预览'
})

const previewEmptyDescription = computed(() => {
  if (sourceMode.value === 'file') return '选择图片后会在这里显示'
  return '输入有效地址后会在这里显示'
})

/**
 * 上传前校验与后端保持一致：仅支持常见图片格式，文件不能超过 5MB。
 */
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  const suffix = file.name.split('.').pop()?.toLowerCase()
  const allowedSuffixes = ['jpg', 'jpeg', 'png', 'webp']

  if (!allowedTypes.includes(file.type) && !allowedSuffixes.includes(suffix || '')) {
    message.error('仅支持 JPG、PNG、WEBP 图片')
    return Upload.LIST_IGNORE
  }

  if (file.size > MAX_SINGLE_FILE_SIZE) {
    message.error('图片大小不能超过 5MB')
    return Upload.LIST_IGNORE
  }

  const selectedTotalSize = selectedFiles.value.reduce((total, item) => total + item.size, 0)
  if (selectedTotalSize + file.size > MAX_BATCH_TOTAL_SIZE) {
    message.error('批量上传图片总大小不能超过 30MB')
    return Upload.LIST_IGNORE
  }

  const isFirstFile = selectedFiles.value.length === 0
  selectedFiles.value = [...selectedFiles.value, file]
  localPreviewUrls.value = [...localPreviewUrls.value, URL.createObjectURL(file)]
  fileList.value = [...fileList.value, {
    uid: file.uid,
    name: file.name,
    size: file.size,
    type: file.type,
    status: 'done',
    originFileObj: file,
  }]

  if (isFirstFile) activeFileIndex.value = 0

  // 单张上传仍可自动填充名称；批量上传时清空这个临时名称，让后端按每个文件名生成。
  if (isFirstFile && !form.name.trim()) {
    autoGeneratedName.value = file.name.replace(/\.[^.]+$/, '')
    form.name = autoGeneratedName.value
  } else if (!isFirstFile && form.name === autoGeneratedName.value) {
    form.name = ''
    autoGeneratedName.value = ''
  }

  submitError.value = ''
  return false
}

/**
 * 加载后端维护的分类与标签，失败时不阻断上传主流程。
 */
async function loadCategoryOptions() {
  categoryLoading.value = true
  try {
    const res = await listPictureCategory()
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '分类与标签加载失败')
    }
    categories.value = res.data.data?.categorys || []
    tags.value = res.data.data?.tags || []
  } catch (error: any) {
    pageNotice.value =
      error?.response?.data?.message ||
      error?.message ||
      '分类与标签暂时无法加载，仍可上传图片'
  } finally {
    categoryLoading.value = false
  }
}

/**
 * 上传接口依赖 Session。未登录时返回登录页，并保留当前地址。
 */
async function ensureCurrentUser() {
  authChecking.value = true
  try {
    if (!loginUserStore.loginUser) {
      await loginUserStore.fetchLoginUser()
    }
    if (!loginUserStore.loginUser) {
      await router.replace({
        path: '/user/login',
        query: { redirect: route.fullPath },
      })
      return
    }

    // 从个人空间进入上传页时，自动选中私人空间，避免用户误传到公共图库。
    if (route.query.target === 'space') {
      if (hasPrivateSpace.value) {
        form.target = 'space'
      } else {
        pageNotice.value = '当前账号还没有可用的私人空间，上传位置已保留为公共图库'
      }
    }
  } finally {
    authChecking.value = false
  }
}

/**
 * 组装 multipart/form-data 并调用真实上传接口。
 * 公共图库必须省略 spaceId；传 0 会被后端当成指定空间处理。
 */
async function submitUpload() {
  submitError.value = ''
  uploadResult.value = null

  if (!loginUserStore.loginUser) {
    await ensureCurrentUser()
    return
  }

  if (!sourceReady.value) {
    submitError.value =
      sourceMode.value === 'file' ? '请先选择图片' : '请输入有效的 HTTP 或 HTTPS 图片地址'
    return
  }

  if (form.target === 'space' && !hasPrivateSpace.value) {
    submitError.value = '当前账号还没有可用的私人空间'
    return
  }

  submitting.value = true
  try {
    const metadata: Record<string, string | string[]> = {}
    const name = form.name.trim()
    const introduction = form.introduction.trim()

    if (name) metadata.name = name
    if (form.category) metadata.category = form.category
    if (form.tags.length) metadata.tags = [...new Set(form.tags.map((item) => item.trim()).filter(Boolean))]
    if (introduction) metadata.introduction = introduction
    if (sourceMode.value === 'url') metadata.url = imageUrl.value.trim()

    if (form.target === 'space') {
      metadata.spaceId = String(loginUserStore.loginUser.spaceId)
    }

    const files = sourceMode.value === 'file' ? selectedFiles.value : undefined
    const res = await uploadPic({}, metadata, files)
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '图片上传失败')
    }

    const result = res.data?.data
    if (!result) throw new Error(res.data?.message || '上传结果不完整')

    uploadResult.value = result
    const pictures = result.successPictureList || []
    const successCount = Number(result.successCount || pictures.length)
    const failCount = Number(result.failCount || result.failPictureList?.length || 0)
    const totalCount = Number(result.totalCount || successCount + failCount)

    if (successCount > 0 && failCount > 0) {
      message.warning(`上传完成：成功 ${successCount} 张，失败 ${failCount} 张，共处理 ${totalCount} 张`)
    } else if (successCount > 0) {
      message.success(
        isAdmin.value
          ? `成功上传 ${successCount} 张，图片已自动通过审核`
          : `成功上传 ${successCount} 张，图片已进入审核队列`,
      )
    } else {
      message.warning(`本次共处理 ${totalCount} 张，但没有图片上传成功`)
    }
  } catch (error: any) {
    submitError.value =
      error?.response?.data?.message ||
      error?.message ||
      '图片上传失败，请检查后端服务和对象存储配置'
  } finally {
    submitting.value = false
  }
}

function openPictureManage() {
  void router.push('/gallery/manage')
}

function revokeLocalPreview() {
  localPreviewUrls.value.forEach((previewUrl) => URL.revokeObjectURL(previewUrl))
  localPreviewUrls.value = []
  activeFileIndex.value = 0
}

function selectPreview(index: number) {
  if (index < 0 || index >= selectedFiles.value.length) return
  activeFileIndex.value = index
}

function selectPreviousPreview() {
  if (selectedFiles.value.length < 2) return
  activeFileIndex.value =
    (activeFileIndex.value - 1 + selectedFiles.value.length) % selectedFiles.value.length
}

function selectNextPreview() {
  if (selectedFiles.value.length < 2) return
  activeFileIndex.value = (activeFileIndex.value + 1) % selectedFiles.value.length
}

function cancelUpload() {
  if (submitting.value) return
  void router.push('/gallery')
}

function clearSelectedFile() {
  if (form.name === autoGeneratedName.value) {
    form.name = ''
  }
  autoGeneratedName.value = ''
  selectedFiles.value = []
  fileList.value = []
  revokeLocalPreview()
}

function handlePreviewError() {
  if (sourceMode.value === 'url') {
    urlPreviewFailed.value = true
  }
}

function isHttpUrl(value: string) {
  if (!value) return false
  try {
    const parsedUrl = new URL(value)
    return parsedUrl.protocol === 'http:' || parsedUrl.protocol === 'https:'
  } catch {
    return false
  }
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

watch(imageUrl, () => {
  urlPreviewFailed.value = false
  submitError.value = ''
})

watch(sourceMode, () => {
  submitError.value = ''
})

onMounted(() => {
  void Promise.all([ensureCurrentUser(), loadCategoryOptions()])
})

onBeforeUnmount(() => {
  revokeLocalPreview()
})
</script>

<style scoped>
.upload-prototype {
  /* 桌面端跟随外层内容区填满剩余高度，移动端由媒体查询恢复自然高度。 */
  --upload-font-ui: 'Geist', 'Manrope', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --upload-font-display: 'Abril Fatface', Georgia, serif;
  --upload-font-data: 'DM Mono', monospace;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--proto-ink);
  font-family: var(--upload-font-ui);
}

.upload-audit-tag.ant-tag {
  margin: 0;
  padding: 4px 9px;
  border: 0;
  border-radius: 999px;
  background: rgba(186, 255, 61, .22);
  color: #477200;
  font-size: .6875rem;
  line-height: 1.2;
  font-weight: 800;
}

.upload-mode-tag.ant-tag {
  margin: 0;
  padding: 3px 7px;
  border: 1px solid rgba(116, 166, 25, .22);
  border-radius: 999px;
  background: rgba(186, 255, 61, .18);
  color: #477200;
  font-size: .6875rem;
  line-height: 1.2;
  font-weight: 800;
}

.upload-page-alert {
  flex: 0 0 auto;
  margin-top: 8px;
}

.upload-layout.proto-section {
  /* 上传页按内容自然排版，避免父级剩余高度把预览框强行拉长。 */
  flex: 0 0 auto;
  min-height: 0;
  padding-top: 6px;
  overflow: visible;
}

.upload-layout {
  /* 左侧图片区占主视觉，右侧元信息保留足够宽度并与左侧等高。 */
  /* 原型入口保留固定基准；正式前端会由统一画布接管剩余高度。 */
  --upload-preview-height: 560px;
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(320px, 3fr);
  grid-template-rows: auto;
  min-height: 0;
  gap: var(--prototype-layout-gap);
  overflow: visible;
  align-items: stretch;
}

.upload-source-panel,
.upload-form-panel {
  min-height: 0;
  height: auto;
  /* 元信息面板跟随图片区的网格行高，避免右侧提前结束形成断层。 */
  align-self: stretch;
  padding: 16px;
}

.upload-source-panel {
  display: flex;
  flex-direction: column;
  background: var(--proto-ink);
  color: var(--proto-paper);
}

.source-heading,
.form-panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.source-heading,
.form-panel-heading,
.source-mode-control,
.form-panel-status {
  min-width: 0;
}

.source-heading h2,
.form-panel-heading h2 {
  margin: 0;
  color: inherit;
  font-size: 1.5rem;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.source-mode-control,
.form-panel-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-mode-control > span {
  color: var(--proto-acid);
  font-size: .75rem;
  line-height: 1.25;
  font-weight: 800;
}

.form-panel-status {
  flex: 0 0 auto;
  color: var(--proto-ink-soft);
  font-size: .75rem;
  font-weight: 700;
}

.source-heading :deep(.ant-radio-button-wrapper) {
  border-color: rgba(241, 242, 237, 0.22);
  background: transparent;
  color: rgba(241, 242, 237, 0.72);
}

.source-heading :deep(.ant-radio-button-wrapper-checked) {
  border-color: var(--proto-acid);
  background: var(--proto-acid);
  color: var(--proto-ink);
}

.source-control {
  /* 本地拖拽区和网络地址区共用同一条高度轨道，切换模式时页面不跳动。 */
  flex: 0 0 112px;
  height: 112px;
  min-height: 112px;
  margin-top: 14px;
}

.upload-source-panel :deep(.ant-upload.ant-upload-drag) {
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 12px;
  border: 1px dashed rgba(241, 242, 237, 0.32);
  border-radius: 6px;
  background: rgba(241, 242, 237, 0.05);
}

/* 让图标、主提示和容量说明作为一个整体垂直居中，避免说明文字溢出虚线框。 */
.upload-source-panel :deep(.ant-upload.ant-upload-drag .ant-upload-btn) {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 0 !important;
}

.upload-source-panel :deep(.ant-upload.ant-upload-drag:hover),
.upload-source-panel :deep(.ant-upload.ant-upload-drag:focus-within) {
  border-color: var(--proto-acid);
}

.upload-source-panel :deep(.ant-upload-drag-icon) {
  margin: 0 0 4px;
  color: var(--proto-acid);
  font-size: 24px;
}

.upload-drag-title {
  margin: 0;
  color: var(--proto-paper);
  font-size: 13px;
  font-weight: 700;
}

.upload-drag-copy {
  margin: 4px 0 0;
  color: rgba(241, 242, 237, 0.7);
  font-size: 10px;
}

.url-control label {
  display: block;
  margin-bottom: 7px;
  color: var(--proto-paper);
  font-size: 12px;
  font-weight: 700;
}

.url-control p {
  margin: 7px 0 0;
  color: rgba(241, 242, 237, 0.68);
  font-size: 10px;
  line-height: 1.45;
}

.url-control :deep(.ant-input-affix-wrapper) {
  border-color: rgba(241, 242, 237, 0.28);
  background: rgba(241, 242, 237, 0.08);
  color: var(--proto-paper);
}

.url-control :deep(.ant-input) {
  background: transparent;
  color: var(--proto-paper);
}

.url-control :deep(.ant-input::placeholder) {
  color: rgba(241, 242, 237, 0.68);
}

.upload-preview {
  /* 固定预览轨道，超大图只能在轨道内 contain，不能反向撑开整页。 */
  flex: 0 0 var(--upload-preview-height);
  width: 100%;
  height: var(--upload-preview-height);
  min-width: 0;
  min-height: 0;
  max-height: var(--upload-preview-height);
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid rgba(241, 242, 237, 0.16);
  border-radius: 6px;
  background: #1c2021;
}

.upload-preview img {
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview-strip {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.preview-thumb {
  position: relative;
  flex: 0 0 58px;
  width: 58px;
  height: 42px;
  overflow: hidden;
  padding: 0;
  border: 1px solid rgba(241, 242, 237, .22);
  border-radius: 4px;
  background: rgba(241, 242, 237, .08);
  cursor: pointer;
  opacity: .76;
  transition: border-color .16s ease, opacity .16s ease;
}

.preview-thumb:hover,
.preview-thumb:focus-visible,
.preview-thumb.active {
  border-color: var(--proto-acid);
  opacity: 1;
}

.preview-thumb:focus-visible {
  outline: 2px solid var(--proto-acid);
  outline-offset: 2px;
}

.preview-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-thumb > span {
  position: absolute;
  right: 3px;
  bottom: 2px;
  min-width: 14px;
  padding: 1px 3px;
  border-radius: 999px;
  background: rgba(16, 19, 20, .78);
  color: var(--proto-paper);
  font-size: 9px;
  line-height: 1.2;
  text-align: center;
}

.preview-strip-hint {
  min-width: 120px;
  color: rgba(241, 242, 237, .62);
  font-size: 10px;
  line-height: 1.35;
}

.preview-empty {
  width: 100%;
  height: 100%;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 5px;
  color: rgba(241, 242, 237, 0.56);
  text-align: center;
}

.preview-empty > span:first-child {
  font-size: 23px;
}

.preview-empty strong {
  color: rgba(241, 242, 237, 0.78);
  font-size: 12px;
}

.preview-empty span {
  font-size: 10px;
}

.selected-source {
  min-height: 39px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  padding: 7px 9px;
  border-radius: 5px;
  background: rgba(186, 255, 61, 0.1);
}

.selected-source span {
  min-width: 0;
}

.selected-source strong,
.selected-source small {
  display: block;
}

.selected-source strong {
  overflow: hidden;
  color: var(--proto-paper);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-source small {
  margin-top: 2px;
  color: rgba(241, 242, 237, 0.64);
  font-size: 9px;
}

.selected-source :deep(.ant-btn) {
  color: var(--proto-paper);
}

.upload-form-panel {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.58);
}

.batch-metadata-hint {
  margin: -6px 0 10px;
  color: #5b6a51;
  font-size: 10px;
  line-height: 1.45;
}

.form-panel-heading {
  flex: 0 0 auto;
  justify-content: space-between;
  margin-bottom: 14px;
}

.upload-form {
  min-height: 0;
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
}

.upload-form :deep(.ant-form-item) {
  margin-bottom: 9px;
}

.upload-form :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

.upload-form :deep(.ant-form-item-label > label) {
  height: auto;
  color: var(--proto-ink);
  font-size: 11px;
  font-weight: 700;
}

.upload-form :deep(textarea.ant-input) {
  min-height: 66px;
  resize: none;
}

.form-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.field-hint {
  margin: 4px 0 0;
  color: var(--proto-muted);
  font-size: 9px;
}

.upload-submit-alert {
  margin-top: 1px;
}

.upload-form-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--proto-line);
}

.submit-contract {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 12px;
  color: var(--proto-muted);
  font-size: 10px;
}

.upload-result-section {
  margin-top: var(--prototype-layout-gap);
  padding-top: 0;
}

.upload-result-panel {
  padding: 16px;
  background: rgba(255, 255, 255, 0.58);
}

.upload-result-heading,
.upload-result-subheading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.upload-result-heading h2,
.upload-result-subheading h3 {
  margin: 0;
  color: var(--proto-ink);
}

.upload-result-heading h2 {
  font-size: 1.35rem;
  line-height: 1.15;
}

.upload-result-subheading {
  margin-bottom: 10px;
}

.upload-result-subheading h3 {
  font-size: 1rem;
}

.upload-result-subheading > span {
  color: var(--proto-muted);
  font-size: 11px;
  font-weight: 700;
}

.upload-result-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.upload-result-stat {
  padding: 12px;
  border: 1px solid var(--proto-line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.42);
}

.upload-result-stat span,
.upload-result-stat strong {
  display: block;
}

.upload-result-stat span {
  color: var(--proto-muted);
  font-size: 10px;
  font-weight: 700;
}

.upload-result-stat strong {
  margin-top: 3px;
  color: var(--proto-ink);
  font-size: 1.35rem;
  line-height: 1;
}

.upload-result-stat.is-success strong {
  color: #4d7800;
}

.upload-result-stat.is-failed strong {
  color: #a23d28;
}

.upload-result-alert {
  margin-top: 12px;
}

.upload-success-list,
.upload-failed-list {
  margin-top: 18px;
}

.upload-success-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.upload-success-item {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--proto-line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.45);
}

.upload-success-media {
  aspect-ratio: 4 / 3;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #eceee8;
  color: var(--proto-muted);
  font-size: 10px;
}

.upload-success-media img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-success-meta {
  min-width: 0;
  padding: 8px;
}

.upload-success-meta strong,
.upload-success-meta small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-success-meta strong {
  color: var(--proto-ink);
  font-size: 11px;
}

.upload-success-meta small {
  margin-top: 3px;
  color: var(--proto-muted);
  font-size: 9px;
}

.upload-failed-list ul {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid var(--proto-line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.42);
}

.upload-failed-list li {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
}

.upload-failed-list li + li {
  border-top: 1px solid var(--proto-line);
}

.upload-failed-list li > div {
  min-width: 0;
}

.upload-failed-list li strong,
.upload-failed-list li small,
.upload-failed-list li > span {
  display: block;
}

.upload-failed-list li strong {
  overflow: hidden;
  color: var(--proto-ink);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-failed-list li small {
  margin-top: 3px;
  color: var(--proto-muted);
  font-size: 9px;
}

.upload-failed-list li > span {
  flex: 0 1 46%;
  color: #a23d28;
  font-size: 10px;
  line-height: 1.4;
  text-align: right;
}

/* 100% 缩放下常见的矮桌面视口：压缩留白，但保留完整表单和可点击尺寸。 */
@media (min-width: 921px) and (max-height: 900px) {
  .upload-layout.proto-section {
    padding-top: 4px;
  }

  .upload-source-panel,
  .upload-form-panel {
    padding: 14px;
  }

  .source-control {
    margin-top: 10px;
  }

  .upload-source-panel :deep(.ant-upload.ant-upload-drag) {
    min-height: 92px;
    padding: 9px;
  }

  .upload-preview {
    /* 短桌面视口压缩轨道，但仍保持固定高度。 */
    min-height: 0;
    flex: 0 0 var(--upload-preview-height);
    height: var(--upload-preview-height);
    max-height: var(--upload-preview-height);
    margin-top: 9px;
  }

  .form-panel-heading {
    margin-bottom: 10px;
  }

  .upload-form :deep(.ant-form-item) {
    margin-bottom: 7px;
  }

  .upload-form :deep(.ant-form-item-label) {
    padding-bottom: 3px;
  }

  .upload-form :deep(textarea.ant-input) {
    min-height: 58px;
  }

  .upload-form-foot {
    padding-top: 8px;
  }
}

@media (max-width: 980px) {
  .upload-layout.proto-section {
    flex: none;
    min-height: 0;
    padding-top: 18px;
    overflow: visible;
  }

  .upload-prototype {
    min-height: 0;
  }

  .upload-layout {
    --upload-preview-height: 280px;
    grid-template-columns: 1fr;
    grid-template-rows: auto;
    min-height: 0;
    align-items: start;
  }

  .upload-source-panel,
  .upload-form-panel {
    height: auto;
  }

  .upload-preview {
    height: var(--upload-preview-height);
    max-height: var(--upload-preview-height);
    min-height: 0;
    flex: 0 0 var(--upload-preview-height);
  }

  .upload-form {
    flex: 0 0 auto;
  }

  .upload-form-foot {
    margin-top: 4px;
  }
}

@media (max-width: 560px) {
  .source-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .source-mode-control {
    width: 100%;
    justify-content: space-between;
  }

  .form-two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .upload-form-foot {
    align-items: stretch;
    flex-direction: column;
  }

  .upload-form-foot :deep(.ant-btn) {
    width: 100%;
  }

  .upload-result-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .upload-result-heading :deep(.ant-btn) {
    width: 100%;
  }

  .upload-result-stats {
    grid-template-columns: 1fr;
  }

  .upload-failed-list li {
    flex-direction: column;
    gap: 6px;
  }

  .upload-failed-list li > span {
    flex-basis: auto;
    text-align: left;
  }
}

/* 第一阶段视觉整理：去掉 Hero 标题，只保留紧凑的流程定位。 */
.upload-progress-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 28px;
  padding: 10px 4px 0;
}

.upload-steps {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0;
}

.upload-step {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #9a9f98;
  font-size: .75rem;
  line-height: 1.25;
  white-space: nowrap;
}

.upload-step + .upload-step::before {
  content: '';
  position: absolute;
  right: calc(100% + 10px);
  width: 14px;
  height: 1px;
  background: var(--proto-line);
}

.upload-step > span {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 1px solid #d6dbd2;
  border-radius: 50%;
  font-family: var(--upload-font-display);
  font-size: 12px;
  line-height: 1;
}

.upload-step.is-active {
  color: var(--proto-ink);
}

.upload-step.is-active > span {
  border-color: var(--proto-acid);
  background: var(--proto-acid);
  color: var(--proto-ink);
  font-weight: 800;
}

.upload-prototype :deep(.ant-form-item-label > label) {
  color: var(--proto-ink);
  font-family: var(--upload-font-ui);
  font-size: .75rem;
  font-weight: 700;
}

.upload-prototype :deep(.ant-input),
.upload-prototype :deep(.ant-input-affix-wrapper),
.upload-prototype :deep(.ant-select-selector),
.upload-prototype :deep(.ant-select-selection-item),
.upload-prototype :deep(.ant-select-selection-placeholder),
.upload-prototype :deep(.ant-radio-button-wrapper),
.upload-prototype :deep(.ant-btn) {
  font-family: var(--upload-font-ui);
}

.upload-prototype :deep(.ant-input),
.upload-prototype :deep(.ant-input-affix-wrapper),
.upload-prototype :deep(.ant-select-selector) {
  font-size: .875rem;
}

.upload-mode-tag.ant-tag,
.upload-audit-tag.ant-tag {
  font-family: var(--upload-font-ui);
  padding: 3px 7px;
  font-size: .6875rem;
  line-height: 1.25;
}

.preview-count {
  font-family: var(--upload-font-display);
  font-size: .75rem;
}

.upload-layout {
  --upload-preview-height: 420px;
  grid-template-columns: minmax(0, 1.18fr) minmax(330px, .82fr);
  gap: 18px;
  align-items: start;
}

/* 上传页不再被全局剩余高度拉伸，卡片高度由真实内容决定。 */
.upload-prototype .upload-layout.proto-section {
  flex: 0 0 auto;
  min-height: 0;
  overflow: visible;
}

.upload-source-panel,
.upload-form-panel {
  border: 1px solid #e2e5df;
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(31, 39, 28, .06);
  padding: 20px;
  height: auto;
  align-self: start;
}

.upload-prototype .upload-source-panel,
.upload-prototype .upload-form-panel {
  height: auto;
  align-self: start;
}

.upload-source-panel {
  background: #fff;
  color: var(--proto-ink);
}

.upload-form-panel {
  background: #fff;
}

.source-heading {
  align-items: flex-start;
  margin-bottom: 0;
}

.source-heading > div:first-child {
  min-width: 0;
}

.source-heading h2,
.form-panel-heading h2 {
  font-size: 1.15rem;
  font-weight: 750;
  line-height: 1.25;
  letter-spacing: -0.035em;
}

.source-heading p {
  margin: 5px 0 0;
  color: var(--proto-muted);
  font-size: .75rem;
  line-height: 1.4;
}

.form-panel-heading {
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf0ea;
}

.form-panel-status {
  gap: 6px;
}

.source-mode-control {
  flex: 0 0 auto;
}

.source-mode-control > span {
  color: var(--proto-muted);
  font-size: .6875rem;
}

.source-heading :deep(.ant-radio-button-wrapper) {
  border-color: #dfe4dc;
  background: #fff;
  color: var(--proto-muted);
}

.source-heading :deep(.ant-radio-button-wrapper-checked) {
  border-color: var(--proto-acid);
  background: #f1fbdc;
  color: #426600;
}

.source-control {
  flex-basis: 72px;
  height: 72px;
  min-height: 72px;
  margin-top: 12px;
}

.upload-source-panel :deep(.ant-upload.ant-upload-drag) {
  border-color: #d9ded5;
  border-radius: 8px;
  background: #fafbf8;
}

.upload-source-panel :deep(.ant-upload.ant-upload-drag:hover),
.upload-source-panel :deep(.ant-upload.ant-upload-drag:focus-within) {
  border-color: #94c92d;
  background: #f8fdea;
}

.upload-source-panel :deep(.ant-upload.ant-upload-drag .ant-upload-btn) {
  color: var(--proto-ink);
}

.upload-source-panel :deep(.ant-upload-drag-icon) {
  display: none;
}

.upload-drag-title {
  color: var(--proto-ink);
  font-size: .75rem;
}

.upload-drag-copy {
  color: var(--proto-muted);
  font-size: .625rem;
}

.url-control label,
.url-control p {
  color: var(--proto-muted);
}

.url-control :deep(.ant-input-affix-wrapper) {
  border-color: #d9ded5;
  background: #fff;
  color: var(--proto-ink);
}

.url-control :deep(.ant-input) {
  color: var(--proto-ink);
}

.upload-preview {
  position: relative;
  display: grid;
  place-items: center;
  border-color: #dfe4dc;
  border-radius: 9px;
  background: #f5f7f3;
}

.upload-preview img {
  background: #fff;
}

.preview-empty {
  color: #9ca39a;
}

.preview-empty strong {
  color: #626b61;
}

.preview-arrow {
  position: absolute;
  top: 50%;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: rgba(28, 34, 28, .68);
  color: #fff;
  cursor: pointer;
  transform: translateY(-50%);
}

.preview-arrow:hover,
.preview-arrow:focus-visible {
  background: #1c221c;
}

.preview-arrow:focus-visible {
  outline: 2px solid var(--proto-acid);
  outline-offset: 2px;
}

.preview-arrow-prev {
  left: 12px;
}

.preview-arrow-next {
  right: 12px;
}

.preview-count {
  position: absolute;
  bottom: 10px;
  left: 50%;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(26, 30, 27, .78);
  color: #fff;
  font-size: .625rem;
  line-height: 1.2;
  transform: translateX(-50%);
}

.preview-strip {
  gap: 8px;
  min-height: 58px;
  margin-top: 10px;
  padding-bottom: 0;
}

.preview-thumb {
  flex-basis: 78px;
  width: 78px;
  height: 54px;
  border-color: #dfe4dc;
  border-radius: 7px;
  opacity: .82;
}

.preview-thumb.active {
  border: 2px solid var(--proto-acid);
}

.preview-add-upload {
  flex: 0 0 78px;
  width: 78px;
  height: 54px;
}

.preview-add-upload :deep(.ant-upload) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border: 1px dashed #cdd4c8;
  border-radius: 7px;
  background: #fafbf8;
  color: var(--proto-muted);
  cursor: pointer;
}

.preview-add-upload :deep(.ant-upload:hover) {
  border-color: #94c92d;
  color: #5a890d;
}

.preview-add-icon {
  margin-right: 4px;
  font-size: .875rem;
}

.preview-add-upload :deep(.ant-upload) > span:last-child {
  font-size: .6875rem;
}

.preview-strip-hint {
  min-width: 90px;
  color: #a0a69e;
  font-size: .625rem;
}

.selected-source {
  min-height: 34px;
  margin-top: 9px;
  padding: 6px 9px;
  border: 1px solid #e5e9e2;
  border-radius: 7px;
  background: #fbfcfa;
}

.selected-source strong {
  color: var(--proto-ink);
}

.selected-source small {
  color: var(--proto-muted);
}

.batch-metadata-hint {
  margin: -2px 0 14px;
  padding: 8px 10px;
  border-left: 3px solid var(--proto-acid);
  border-radius: 0 7px 7px 0;
  background: #f3f9e6;
  color: #58792a;
  font-size: .75rem;
  line-height: 1.5;
}

.target-choice-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.target-choice-group :deep(.ant-radio-button-wrapper) {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 54px;
  padding: 0 12px;
  border: 1px solid #e1e5df;
  border-radius: 8px;
  color: var(--proto-muted);
}

/* AntDV 会把 radio-button 的插槽内容再包一层 span，必须在这一层建立横向布局。 */
.target-choice-group :deep(.ant-radio-button-wrapper > span:last-child) {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  align-items: center;
  gap: 9px;
}

.target-choice-group :deep(.ant-radio-button-wrapper::before) {
  display: none;
}

.target-choice-group :deep(.ant-radio-button-wrapper-checked) {
  border-color: #b5e15a;
  background: #f4fbe8;
  color: #5b8d12;
  box-shadow: none;
}

.target-choice-group :deep(.ant-radio-button-wrapper-disabled) {
  background: #fafafa;
}

.target-choice-group :deep(.anticon) {
  font-size: 1rem;
}

.target-choice-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.target-choice-icon {
  display: grid;
  flex: 0 0 20px;
  width: 20px;
  height: 20px;
  place-items: center;
}

.target-choice-group strong {
  color: var(--proto-ink);
  font-size: .8125rem;
  line-height: 1.25;
}

.target-choice-group small {
  margin-top: 3px;
  color: var(--proto-muted);
  font-size: .6875rem;
  line-height: 1.3;
}

.upload-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.upload-form :deep(.ant-form-item-label) {
  padding-bottom: 5px;
}

.upload-form :deep(.ant-input),
.upload-form :deep(.ant-input-affix-wrapper),
.upload-form :deep(.ant-select-selector) {
  border-radius: 7px;
}

.upload-form :deep(textarea.ant-input) {
  min-height: 86px;
}

.upload-form-foot {
  align-items: center;
  gap: 16px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #edf0ea;
}

.upload-submit-alert {
  margin: 0;
}

.submit-contract {
  font-size: .6875rem;
  line-height: 1.4;
}

.upload-form-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.upload-cancel-button {
  min-width: 84px;
  height: 38px;
  border-color: #dfe4dc;
  border-radius: 7px;
}

.upload-form-actions .acid-button {
  min-width: 126px;
  height: 38px;
  font-size: .8125rem;
  border-radius: 7px;
}

.upload-prototype .upload-form {
  flex: 0 0 auto;
}

.upload-prototype .upload-form-foot {
  margin-top: 12px;
}

/* 第三阶段视觉收口：根据视口高度压缩留白，不让图片尺寸决定布局高度。 */
@media (min-width: 921px) and (max-height: 900px) {
  .upload-progress-row {
    padding-top: 6px;
  }

  .upload-layout {
    --upload-preview-height: 360px;
  }

  .upload-source-panel,
  .upload-form-panel {
    padding: 16px;
  }

  .source-control {
    flex-basis: 64px;
    height: 64px;
    min-height: 64px;
  }

  .upload-form :deep(.ant-form-item) {
    margin-bottom: 10px;
  }

  .upload-form :deep(textarea.ant-input) {
    min-height: 72px;
  }

  .upload-form-foot {
    padding-top: 12px;
  }
}

@media (max-width: 980px) {
  .upload-progress-row {
    justify-content: flex-start;
    padding-inline: 0;
  }

  .upload-steps {
    width: 100%;
    justify-content: space-between;
  }

  .upload-layout {
    --upload-preview-height: 280px;
    grid-template-columns: 1fr;
  }

  .upload-source-panel,
  .upload-form-panel {
    height: auto;
  }

  .upload-form {
    flex: 0 0 auto;
  }

  .upload-form-foot {
    margin-top: 12px;
  }
}

@media (max-width: 560px) {
  .upload-progress-row {
    padding-top: 8px;
  }

  .upload-steps {
    gap: 12px;
  }

  .upload-step {
    font-size: .6875rem;
  }

  .upload-step + .upload-step::before {
    display: none;
  }

  .source-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .source-mode-control {
    width: 100%;
    justify-content: space-between;
  }

  .upload-source-panel,
  .upload-form-panel {
    padding: 16px;
  }

  .target-choice-group {
    grid-template-columns: 1fr;
  }

  .upload-form-foot {
    align-items: stretch;
    flex-direction: column;
  }

  .upload-form-actions,
  .upload-form-actions :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
