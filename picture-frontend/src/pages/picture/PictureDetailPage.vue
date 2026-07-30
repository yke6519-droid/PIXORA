<template>
  <div id="pictureDetailPage">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <a-spin size="large" tip="加载中..." />
    </div>

    <!-- 错误状态 -->
    <a-result
      v-else-if="error"
      status="error"
      title="加载失败"
      :sub-title="errorMessage"
    >
      <template #extra>
        <a-button type="primary" @click="fetchPictureDetail">重试</a-button>
        <a-button @click="router.push('/picture')">返回图库</a-button>
      </template>
    </a-result>

    <!-- 图片详情内容 -->
    <template v-else-if="picture">
      <!-- 顶部导航 -->
      <div class="page-header">
        <a-button @click="router.back()">
          <ArrowLeftOutlined /> 返回
        </a-button>
        <div class="header-actions">
          <a-button @click="copyLink">
            <LinkOutlined /> 复制链接
          </a-button>
          <a-button type="primary" @click="downloadImage">
            <DownloadOutlined /> 下载图片
          </a-button>
        </div>
      </div>

      <!-- 主要内容区 -->
      <div class="detail-container">
        <!-- 左侧图片展示 -->
        <div class="image-section">
          <div class="image-wrapper">
            <img
              :src="picture.url"
              :alt="picture.name"
              class="main-image"
              @click="showPreview = true"
            />
            <div class="image-overlay" @click="showPreview = true">
              <EyeOutlined class="preview-icon" />
              <span>点击预览</span>
            </div>
          </div>
        </div>

        <!-- 右侧信息面板 -->
        <div class="info-section">
          <a-card :bordered="false" class="info-card">
            <!-- 图片名称 -->
            <h1 class="picture-name">{{ picture.name }}</h1>

            <!-- 简介 -->
            <p v-if="picture.introduction" class="picture-intro">
              {{ picture.introduction }}
            </p>

            <!-- 标签信息 -->
            <div class="tags-row">
              <a-tag v-if="picture.category" color="blue" class="category-tag">
                <FolderOutlined /> {{ picture.category }}
              </a-tag>
              <a-tag
                v-for="tag in picture.tags"
                :key="tag"
                color="green"
                class="item-tag"
              >
                <TagOutlined /> {{ tag }}
              </a-tag>
            </div>

            <a-divider />

            <!-- 图片属性 -->
            <div class="properties-section">
              <h3><FileImageOutlined /> 图片属性</h3>
              <div class="property-grid">
                <div class="property-item">
                  <span class="label">格式</span>
                  <span class="value">{{ picture.picformat?.toUpperCase() || '-' }}</span>
                </div>
                <div class="property-item">
                  <span class="label">大小</span>
                  <span class="value">{{ formatSize(picture.picsize) }}</span>
                </div>
                <div class="property-item">
                  <span class="label">宽度</span>
                  <span class="value">{{ picture.picwidth ? picture.picwidth + ' px' : '-' }}</span>
                </div>
                <div class="property-item">
                  <span class="label">高度</span>
                  <span class="value">{{ picture.picheight ? picture.picheight + ' px' : '-' }}</span>
                </div>
                <div class="property-item">
                  <span class="label">宽高比</span>
                  <span class="value">{{ picture.picscale || '-' }}</span>
                </div>
              </div>
            </div>

            <a-divider />

            <!-- 上传者信息 -->
            <div v-if="picture.createdUser" class="uploader-section">
              <h3><UserOutlined /> 上传者信息</h3>
              <div class="uploader-info">
                <a-avatar
                  :src="picture.createdUser.avatarurl"
                  :size="48"
                  class="uploader-avatar"
                >
                  {{ picture.createdUser.username?.charAt(0).toUpperCase() }}
                </a-avatar>
                <div class="uploader-detail">
                  <div class="uploader-name">{{ picture.createdUser.username }}</div>
                  <div class="upload-time">
                    <ClockCircleOutlined />
                    上传于 {{ formatTime(picture.createtime) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 管理操作（仅管理员或上传者可见） -->
            <template v-if="canEdit">
              <a-divider />
              <div class="admin-actions">
                <h3><SettingOutlined /> 管理操作</h3>
                <div class="action-buttons">
                  <a-button type="primary" @click="showEditModal">
                    <EditOutlined /> 编辑信息
                  </a-button>
                  <a-button danger @click="handleDelete">
                    <DeleteOutlined /> 删除图片
                  </a-button>
                </div>
              </div>
            </template>
          </a-card>
        </div>
      </div>
    </template>

    <!-- 编辑弹窗（使用上传接口） -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑图片信息"
      :confirm-loading="editLoading"
      @ok="handleSaveEdit"
      @cancel="closeEditModal"
      width="700px"
    >
      <a-form
        :model="editForm"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
        class="edit-form"
      >
        <!-- 原图片预览 -->
        <a-form-item v-if="editForm.originalUrl" label="原图片">
          <div class="original-image-preview">
            <img :src="editForm.originalUrl" alt="原图片" />
            <p class="preview-tip">请上传新图片替换以上内容</p>
          </div>
        </a-form-item>

        <a-form-item label="图片名称" required>
          <a-input v-model:value="editForm.name" placeholder="请输入图片名称" :maxLength="50" show-count />
        </a-form-item>

        <a-form-item label="图片简介">
          <a-textarea
            v-model:value="editForm.introduction"
            placeholder="请输入图片简介"
            :rows="3"
            :maxLength="200"
            show-count
          />
        </a-form-item>

        <a-form-item label="分类">
          <a-select v-model:value="editForm.category" placeholder="请选择分类" allow-clear>
            <a-select-option v-for="cat in categoryList" :key="cat" :value="cat">
              {{ cat }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="标签">
          <a-select
            v-model:value="editForm.tags"
            mode="tags"
            placeholder="输入标签后回车添加"
            allow-clear
          >
            <a-select-option v-for="tag in tagList" :key="tag" :value="tag">
              {{ tag }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- 上传新图片（可选） -->
        <a-form-item label="上传图片">
          <a-upload-dragger
            v-model:fileList="editFileList"
            :custom-request="customEditRequest"
            :before-upload="beforeEditUpload"
            :multiple="false"
            :max-count="1"
            accept="image/*"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">点击或拖拽新图片到此区域上传（可选）</p>
            <p class="ant-upload-hint">
              不上传则保持原图，仅修改信息；上传则用新图替换原图
            </p>
          </a-upload-dragger>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 图片预览 -->
    <a-image
      :style="{ display: 'none' }"
      :preview="{
        visible: showPreview,
        onVisibleChange: (visible: boolean) => showPreview = visible,
      }"
      :src="picture?.url"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '../../stores/useLoginUserStore'
import {
  getPictureByIdUsingGet,
  deletePictureUsingDelete,
  listPictureCategoryUsingGet,
} from '../../api/pictureController'
import {
  ArrowLeftOutlined,
  LinkOutlined,
  DownloadOutlined,
  EyeOutlined,
  FolderOutlined,
  TagOutlined,
  FileImageOutlined,
  UserOutlined,
  ClockCircleOutlined,
  SettingOutlined,
  EditOutlined,
  DeleteOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 状态
const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const picture = ref<API.PictureVO | null>(null)
const showPreview = ref(false)

// 编辑相关
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = ref({
  id: undefined as number | undefined,
  name: '',
  introduction: '',
  category: undefined as string | undefined,
  tags: [] as string[],
  originalUrl: undefined as string | undefined  // 原图片URL
})

// 编辑弹窗的文件列表
const editFileList = ref<any[]>([])

// 分类和标签列表
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])

// 判断是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser?.userstatus === 'admin'
})

// 判断是否可编辑（管理员或上传者本人）
const canEdit = computed(() => {
  if (!picture.value || !loginUserStore.loginUser) return false
  return isAdmin.value || picture.value.createdUser?.id === loginUserStore.loginUser.id
})

/**
 * 获取图片详情
 */
const fetchPictureDetail = async () => {
  const PicId = route.params.id
  if (!PicId) {
    error.value = true
    errorMessage.value = '图片ID不存在'
    return
  }

  loading.value = true
  error.value = false

  try {
    // 根据用户权限选择不同的接口
    const res = await getPictureByIdUsingGet({ id: PicId })
    if (res.data.code === 200) {
      picture.value = res.data.data || null
      if (!picture.value) {
        error.value = true
        errorMessage.value = '图片不存在或已被删除'
      }
    } else {
      error.value = true
      errorMessage.value = res.data.message || '获取图片详情失败'
    }
  } catch (err) {
    error.value = true
    errorMessage.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

/**
 * 格式化文件大小
 */
const formatSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

/**
 * 格式化时间
 */
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/**
 * 复制图片链接
 */
const copyLink = () => {
  if (!picture.value?.url) return
  navigator.clipboard.writeText(picture.value.url).then(() => {
    message.success('链接已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

/**
 * 下载图片
 */
const downloadImage = () => {
  if (!picture.value?.url) return
  const link = document.createElement('a')
  link.href = picture.value.url
  link.download = picture.value.name || 'image'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  message.success('开始下载')
}

/**
 * 获取分类和标签
 */
const fetchCategoryAndTags = async () => {
  try {
    const res = await listPictureCategoryUsingGet()
    if (res.data.code === 200) {
      categoryList.value = res.data.data?.categorys || []
      tagList.value = res.data.data?.tags || []
    }
  } catch (error) {
    console.error('获取分类标签失败', error)
  }
}

/**
 * 打开编辑弹窗
 */
const showEditModal = () => {
  if (!picture.value) return
  editForm.value = {
    id: picture.value.id,
    name: picture.value.name || '',
    introduction: picture.value.introduction || '',
    category: picture.value.category,
    tags: picture.value.tags || [],
    originalUrl: picture.value.url  // 保存原图片URL
  }
  editFileList.value = []  // 清空文件列表
  editModalVisible.value = true
}

/**
 * 关闭编辑弹窗
 */
const closeEditModal = () => {
  editModalVisible.value = false
  editFileList.value = []
  editForm.value.originalUrl = undefined
}

/**
 * 保存编辑
 */
const handleSaveEdit = async () => {
  if (!editForm.value.name?.trim()) {
    message.error('请输入图片名称')
    return
  }

  // 判断是否上传了新图片
  const hasNewFile = editFileList.value.length > 0 && editFileList.value[0]?.originFileObj
  const file = hasNewFile ? editFileList.value[0].originFileObj as File : undefined

  editLoading.value = true
  try {
    if (loginUserStore.loginUser?.userstatus !== 'admin') {
      // 用户上传接口
      const res = await uploadPicUserUsingPost(
        {
          id: editForm.value.id,  // 传入图片ID，更新模式
          name: editForm.value.name,
          introduction: editForm.value.introduction,
          category: editForm.value.category,
          tags: editForm.value.tags
        },
        {},
        file  // 如果没有新文件，传入 undefined
      )

      if (res.data.code === 200) {
        message.success(hasNewFile ? '更新成功！图片将重新进入待审核状态' : '更新成功！')
        editModalVisible.value = false
        editFileList.value = []
        // 刷新图片详情
        await fetchPictureDetail()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      // 管理员上传接口
      const res = await uploadPicAdminUsingPost(
        {
          id: editForm.value.id,  // 传入图片ID，更新模式
          name: editForm.value.name,
          introduction: editForm.value.introduction,
          category: editForm.value.category,
          tags: editForm.value.tags
        },
        {},
        file  // 如果没有新文件，传入 undefined
      )

      if (res.data.code === 200) {
        message.success('更新成功！')
        editModalVisible.value = false
        editFileList.value = []
        // 刷新图片详情
        await fetchPictureDetail()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    }
  } catch (error) {
    message.error('更新失败')
  } finally {
    editLoading.value = false
  }
}

/**
 * 自定义上传请求（编辑弹窗）
 */
const customEditRequest = (options: any) => {
  options.onSuccess?.()
}

/**
 * 上传前校验（编辑弹窗）
 */
const beforeEditUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('图片大小不能超过 10MB！')
    return false
  }
  return true
}

/**
 * 删除图片
 */
const handleDelete = () => {
  const picId = route.params.id
  console.log(picId)
  if (!picId) return

  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这张图片吗？此操作不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deletePictureUsingDelete({ id: picId })
        if (res.data.code === 200) {
          message.success('删除成功')
          router.push('/picture')
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 页面加载时获取数据
onMounted(() => {
  fetchPictureDetail()
  fetchCategoryAndTags()
})
</script>

<style scoped>
#pictureDetailPage {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 64px);
}

/* 加载中 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

/* 顶部导航 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 主要内容区 */
.detail-container {
  display: flex;
  gap: 24px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 左侧图片区 */
.image-section {
  flex: 1;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  min-height: 600px;
}

.image-wrapper {
  position: relative;
  max-width: 100%;
  cursor: pointer;
}

.main-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 8px;
  color: #fff;
  gap: 8px;
}

.image-wrapper:hover .image-overlay {
  opacity: 1;
}

.preview-icon {
  font-size: 32px;
}

/* 右侧信息面板 */
.info-section {
  width: 400px;
  padding: 24px;
}

.info-card {
  height: 100%;
}

.picture-name {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #1a1a1a;
  line-height: 1.4;
}

.picture-intro {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.category-tag,
.item-tag {
  font-size: 13px;
  padding: 4px 12px;
}

/* 属性区域 */
.properties-section h3,
.uploader-section h3,
.admin-actions h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.property-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.property-item .label {
  font-size: 12px;
  color: #999;
}

.property-item .value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 上传者信息 */
.uploader-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.uploader-avatar {
  border: 2px solid #f0f0f0;
}

.uploader-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.upload-time {
  font-size: 13px;
  color: #999;
}

/* 管理操作 */
.action-buttons {
  display: flex;
  gap: 12px;
}

/* 编辑表单 */
.edit-form {
  padding: 16px 0 0;
}

/* 原图片预览 */
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

/* 响应式适配 */
@media (max-width: 1024px) {
  .detail-container {
    flex-direction: column;
  }

  .image-section {
    min-height: 400px;
    padding: 24px;
  }

  .info-section {
    width: 100%;
  }
}

@media (max-width: 768px) {
  #pictureDetailPage {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .picture-name {
    font-size: 20px;
  }

  .property-grid {
    grid-template-columns: 1fr;
  }
}
</style>