<template>
  <div id="mySpacePage">
    <!-- 加载中 -->
    <div v-if="pageLoading" class="loading-wrapper">
      <a-spin size="large" />
    </div>

    <!-- 状态A：未创建空间 -->
    <div v-else-if="!spaceId" class="empty-state">
      <a-empty description="">
        <template #image>
          <div class="empty-icon">
            <InboxOutlined style="font-size: 64px; color: #bfbfbf" />
          </div>
        </template>
        <p class="empty-title">你还没有创建私人空间</p>
        <p class="empty-desc">创建私人空间后，可以将图片存储到自己的空间中，方便管理和查看</p>
        <a-button type="primary" size="large" @click="showCreateSpaceModal = true">
          <PlusOutlined />
          创建我的空间
        </a-button>
      </a-empty>
    </div>

    <!-- 状态B/C：已有空间 -->
    <div v-else class="space-content">
      <!-- 空间信息卡片 -->
      <div class="space-info-card">
        <div class="space-info-header">
          <h2 class="space-name">{{ spaceInfo?.spaceName || '我的空间' }}</h2>
          <a-tag :color="levelColor">{{ levelText }}</a-tag>
        </div>
        <a-row :gutter="48" class="space-info-body">
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">容量使用</span>
              <span class="info-value">{{ formatSize(spaceInfo?.usedSize || 0) }} / {{ formatSize(spaceInfo?.maxSize || 0) }}</span>
            </div>
            <a-progress
              :percent="sizePercent"
              :stroke-color="sizePercent > 80 ? '#ff4d4f' : '#1890ff'"
              :show-info="false"
              size="small"
            />
          </a-col>
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">图片数量</span>
              <span class="info-value">{{ spaceInfo?.usedCount || 0 }} / {{ spaceInfo?.maxCount || 0 }} 张</span>
            </div>
            <a-progress
              :percent="countPercent"
              :stroke-color="countPercent > 80 ? '#ff4d4f' : '#52c41a'"
              :show-info="false"
              size="small"
            />
          </a-col>
        </a-row>
      </div>

      <!-- 状态C：空间无图片 -->
      <div v-if="!pictureList.length && !loadingPictures" class="empty-state">
        <a-empty description="">
          <template #image>
            <div class="empty-icon">
              <PictureOutlined style="font-size: 64px; color: #bfbfbf" />
            </div>
          </template>
          <p class="empty-title">空间里还没有图片</p>
          <p class="empty-desc">上传你的第一张图片到私人空间吧</p>
          <a-button type="primary" size="large" @click="openUploadModal">
            <PlusOutlined />
            上传图片
          </a-button>
        </a-empty>
      </div>

      <!-- 状态B：空间有图片 -->
      <div v-else>
        <!-- 操作栏 -->
        <div class="action-bar">
          <a-button type="primary" @click="openUploadModal">
            <PlusOutlined />
            上传图片
          </a-button>
        </div>

        <!-- 图片网格 -->
        <div class="picture-grid" v-if="!loadingPictures">
          <div
            v-for="picture in pictureList"
            :key="picture.id"
            class="picture-card"
            @click="goToDetail(picture.id!)"
          >
            <div class="picture-wrapper">
              <img :src="picture.url" :alt="picture.name" class="picture-img" />
              <div class="picture-overlay">
                <div class="picture-actions">
                  <a-button type="text" size="small" danger @click.stop="handleDelete(picture)">
                    <DeleteOutlined />
                  </a-button>
                </div>
              </div>
            </div>
            <div class="picture-name">{{ picture.name }}</div>
          </div>
        </div>

        <!-- 图片加载中 -->
        <div v-else class="loading-wrapper">
          <a-spin />
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <a-pagination
            v-model:current="searchParams.current"
            :total="total"
            :pageSize="searchParams.pageSize"
            show-less-items
            @change="onPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 创建空间弹窗 -->
    <a-modal
      v-model:open="showCreateSpaceModal"
      title="创建私人空间"
      :confirm-loading="creatingSpace"
      @ok="handleCreateSpace"
      @cancel="showCreateSpaceModal = false"
      ok-text="确认创建"
      cancel-text="取消"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="空间名称">
          <a-input
            v-model:value="createSpaceName"
            placeholder="请输入空间名称"
            :maxLength="20"
            show-count
          />
        </a-form-item>
        <a-alert
          message="每个用户只能创建一个私人空间，创建后不可更改名称"
          type="info"
          show-icon
          style="margin-top: 8px"
        />
      </a-form>
    </a-modal>

    <!-- 上传弹窗 -->
    <PictureUploadModal
      v-if="showUploadModal"
      :open="showUploadModal"
      :category-list="categoryList"
      :tag-list="tagList"
      :default-space-id="spaceId"
      @update:open="showUploadModal = $event"
      @success="handleUploadSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  InboxOutlined,
  PictureOutlined,
  PlusOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '../../stores/useLoginUserStore'
import {
  createSpaceUsingPost,
  querySpaceByIdUsingGet
} from '../../api/spaceController'
import { queryPicturePageCacheUsingPost } from '../../api/pictureController'
import { deletePictureUsingDelete } from '../../api/pictureController'
import { listPictureCategoryUsingGet } from '../../api/pictureController'
import PictureUploadModal from '../../components/PictureUploadModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// ========== 页面状态 ==========
const pageLoading = ref(true)
const loadingPictures = ref(false)
const creatingSpace = ref(false)
const showCreateSpaceModal = ref(false)
const showUploadModal = ref(false)
const createSpaceName = ref('')

// ========== 空间数据 ==========
const spaceId = computed(() => loginUserStore.loginUser?.spaceId)
const spaceInfo = ref<API.SpaceVO | null>(null)

// ========== 图片数据 ==========
const pictureList = ref<API.PictureVO[]>([])
const total = ref(0)
const searchParams = ref({
  current: 1,
  pageSize: 12,
  spaceId: 0 as number | undefined
})

// ========== 分类/标签 ==========
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])

// ========== 计算属性 ==========
const levelText = computed(() => {
  const level = spaceInfo.value?.spaceLevel
  if (level === 1) return '专业版'
  if (level === 2) return '专家版'
  return '普通版'
})

const levelColor = computed(() => {
  const level = spaceInfo.value?.spaceLevel
  if (level === 1) return 'blue'
  if (level === 2) return 'gold'
  return 'green'
})

const sizePercent = computed(() => {
  if (!spaceInfo.value?.maxSize) return 0
  return Math.round(((spaceInfo.value.usedSize || 0) / spaceInfo.value.maxSize) * 100)
})

const countPercent = computed(() => {
  if (!spaceInfo.value?.maxCount) return 0
  return Math.round(((spaceInfo.value.usedCount || 0) / spaceInfo.value.maxCount) * 100)
})

// ========== 方法 ==========

/** 格式化文件大小 */
const formatSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 加载空间信息 */
const loadSpaceInfo = async () => {
  if (!spaceId.value) return
  try {
    const res: any = await querySpaceByIdUsingGet({ spaceId: spaceId.value })
    if (res.data.code === 200 && res.data.data) {
      spaceInfo.value = res.data.data
    }
  } catch (error) {
    console.error('获取空间信息失败', error)
  }
}

/** 加载空间图片 */
const loadPictures = async () => {
  if (!spaceId.value) return
  loadingPictures.value = true
  try {
    const res: any = await queryPicturePageCacheUsingPost({
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize,
      spaceId: spaceId.value
    })
    if (res.data.code === 200 && res.data.data) {
      console.log(res.data.data)
      pictureList.value = res.data.data.pictureList || []
      total.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('获取空间图片失败', error)
  } finally {
    loadingPictures.value = false
  }
}

/** 创建空间 */
const handleCreateSpace = async () => {
  if (!createSpaceName.value.trim()) {
    message.warning('请输入空间名称')
    return
  }
  creatingSpace.value = true
  try {
    const res: any = await createSpaceUsingPost({ spaceName: createSpaceName.value.trim() })
    if (res.data.code === 200) {
      message.success('空间创建成功！')
      showCreateSpaceModal.value = false
      createSpaceName.value = ''
      // 刷新用户信息（获取新的 spaceId）
      await loginUserStore.fetchLoginUser()
      // 重新加载空间数据
      await loadSpaceInfo()
      await loadPictures()
    } else {
      message.error(res.data.message || '创建空间失败')
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '创建空间失败，请重试')
  } finally {
    creatingSpace.value = false
  }
}

/** 删除图片 */
const handleDelete = (picture: API.PictureVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除图片「${picture.name}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await deletePictureUsingDelete({ id: picture.id })
        if (res.data.code === 200) {
          message.success('删除成功')
          // 重新加载空间信息和图片列表
          await loadSpaceInfo()
          await loadPictures()
        } else {
          message.error(res.data.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败，请重试')
      }
    }
  })
}

/** 翻页 */
const onPageChange = (page: number) => {
  searchParams.value.current = page
  loadPictures()
}

/** 打开上传弹窗 */
const openUploadModal = () => {
  showUploadModal.value = true
}

/** 上传成功回调 */
const handleUploadSuccess = () => {
  // 重新加载空间信息和图片列表
  loadSpaceInfo()
  loadPictures()
}

/** 跳转图片详情 */
const goToDetail = (id: number) => {
  router.push(`/picture/detail/${id}`)
}

/** 加载分类标签 */
const loadCategoryAndTags = async () => {
  try {
    const res: any = await listPictureCategoryUsingGet()
    if (res.data.code === 200 && res.data.data) {
      categoryList.value = res.data.data.categorys || []
      tagList.value = res.data.data.tags || []
    }
  } catch (error) {
    console.error('获取分类标签失败', error)
  }
}

// ========== 生命周期 ==========
onMounted(async () => {
  pageLoading.value = true
  try {
    // 确保用户信息已加载
    if (!loginUserStore.loginUser) {
      await loginUserStore.fetchLoginUser()
    }
    // 加载分类标签
    loadCategoryAndTags()
    // 如果有空间，加载空间信息和图片
    if (spaceId.value) {
      searchParams.value.spaceId = spaceId.value
      await Promise.all([loadSpaceInfo(), loadPictures()])
    }
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
#mySpacePage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 48px;
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

/* 空间信息卡片 */
.space-content {
  margin-top: 8px;
}

.space-info-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.space-info-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.space-name {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.space-info-body {
  margin-top: 8px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/* 操作栏 */
.action-bar {
  margin-bottom: 16px;
}

/* 图片网格 */
.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.picture-card {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.picture-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.picture-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
}

.picture-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.picture-card:hover .picture-img {
  transform: scale(1.05);
}

.picture-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.picture-card:hover .picture-overlay {
  opacity: 1;
}

.picture-actions {
  display: flex;
  gap: 8px;
}

.picture-actions .ant-btn {
  color: #fff !important;
}

.picture-name {
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-bottom: 24px;
}
</style>