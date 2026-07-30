<template>
  <div class="picture-gallery">
    <!-- 顶部搜索区域 -->
    <div class="search-header">
      <h1 class="gallery-title">智能云图库</h1>
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="搜索图片名称、简介..."
        enter-button
        size="large"
        class="search-input"
        @search="handleSearch"
      />
      <div class="search-actions">
        <a-button type="primary" size="large" @click="openUpload">
          <UploadOutlined /> 上传图片
        </a-button>
        <a-button size="large" @click="openBatchFetch" v-if="isAdmin">
          <CloudDownloadOutlined /> 批量拉取
        </a-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <!-- 分类筛选 -->
      <div class="filter-block">
        <span class="filter-label">
          <FolderOutlined /> 分类：
        </span>
        <div class="filter-options">
          <span
            class="filter-option"
            :class="{ active: selectedCategory === '' }"
            @click="selectCategory('')"
          >
            全部
          </span>
          <span
            v-for="cat in categoryList"
            :key="cat"
            class="filter-option"
            :class="{ active: selectedCategory === cat }"
            @click="selectCategory(cat)"
          >
            {{ cat }}
          </span>
        </div>
      </div>

      <!-- 标签筛选 -->
      <div class="filter-block" v-if="tagList.length > 0">
        <span class="filter-label">
          <TagsOutlined /> 标签：
        </span>
        <div class="filter-options">
          <a-checkable-tag
            v-for="tag in tagList"
            :key="tag"
            :checked="selectedTags.includes(tag)"
            @change="(checked: boolean) => toggleTag(tag, checked)"
            class="tag-option"
          >
            {{ tag }}
          </a-checkable-tag>
        </div>
        <!-- 已选标签展示 -->
        <div v-if="selectedTags.length > 0" class="selected-tags">
          <span class="selected-label">已选：</span>
          <a-tag
            v-for="tag in selectedTags"
            :key="tag"
            closable
            @close="removeTag(tag)"
            class="selected-tag-item"
          >
            {{ tag }}
          </a-tag>
          <a-button type="link" size="small" @click="clearAllTags">
            清除全部
          </a-button>
        </div>
      </div>
    </div>

    <!-- 图片列表区域 -->
    <div class="gallery-content">
      <a-spin :spinning="loading" tip="加载中...">
        <!-- 空状态 -->
        <a-empty v-if="!loading && pictureList.length === 0" description="暂无图片" />

        <!-- 图片网格 -->
        <div v-else class="picture-grid">
          <div
            v-for="pic in pictureList"
            :key="pic.id"
            class="picture-item"
            @click="goToDetail(pic.id)"
          >
            <img :src="pic.thumbnailUrl || pic.url" :alt="pic.name" loading="lazy" />
            <div class="picture-info">
              <div class="picture-name">{{ pic.name }}</div>
              <div class="picture-meta">
                <span>{{ pic.picformat?.toUpperCase() }}</span>
                <span>{{ formatSize(pic.picsize) }}</span>
              </div>
            </div>
          </div>
        </div>
      </a-spin>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <a-pagination
          v-model:current="searchParams.current"
          v-model:pageSize="searchParams.pageSize"
          :total="total"
          :pageSizeOptions="['12', '24', '48']"
          showSizeChanger
          showQuickJumper
          :show-total="(total: number) => `共 ${total} 条`"
          @change="fetchPictureList"
        />
      </div>
    </div>

    <!-- 上传弹窗 -->
    <PictureUploadModal
      v-model:open="uploadOpen"
      @success="fetchPictureList"
    />

    <!-- 批量拉取弹窗 -->
    <BatchFetchModal
      v-model:open="batchFetchOpen"
      @success="fetchPictureList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UploadOutlined, CloudDownloadOutlined, FolderOutlined, TagsOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '../../stores/useLoginUserStore'
import {
  queryPicturePageCacheUsingPost,
  listPictureCategoryUsingGet
} from '../../api/pictureController'
import PictureUploadModal from '../../components/PictureUploadModal.vue'
import BatchFetchModal from '../../components/BatchFetchModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 判断是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser?.userstatus === 'admin')

// 加载状态
const loading = ref(false)

// 图片列表
const pictureList = ref<API.PictureVO[]>([])

// 总数
const total = ref(0)

// 弹窗状态
const uploadOpen = ref(false)
const batchFetchOpen = ref(false)

// 搜索参数
const searchParams = ref({
  current: 1,
  pageSize: 12,
  searchText: '',
  category: undefined as string | undefined,
  tags: undefined as string[] | undefined
})

// 分类列表
const categoryList = ref<string[]>([])

// 标签列表
const tagList = ref<string[]>([])

// 选中的分类
const selectedCategory = ref('')

// 选中的标签
const selectedTags = ref<string[]>([])

/**
 * 获取图片列表
 */
const fetchPictureList = async () => {
  loading.value = true
  try {
    const params: API.PictureQueryRequest = {
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize,
      searchText: searchParams.value.searchText || undefined,
      category: searchParams.value.category,
      tags: searchParams.value.tags,
      spaceId: 0,
    }

    // 管理员使用完整接口，普通用户使用脱敏接口(后端自己判断)
    const res = await queryPicturePageCacheUsingPost(params)
    if (res.data.code === 200) {
      pictureList.value = res.data.data?.pictureList || []
      total.value = res.data.data?.total || 0
    } else {
      message.error('获取图片失败：' + res.data.message)
    }
  } catch (error) {
    message.error('获取图片失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取分类和标签列表
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
 * 选择分类
 */
const selectCategory = (category: string) => {
  selectedCategory.value = category
  searchParams.value.category = category || undefined
  searchParams.value.current = 1
  fetchPictureList()
}

/**
 * 切换标签选择
 */
const toggleTag = (tag: string, checked: boolean) => {
  if (checked) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value = selectedTags.value.filter(t => t !== tag)
  }
  searchParams.value.tags = selectedTags.value.length > 0 ? selectedTags.value : undefined
  searchParams.value.current = 1
  fetchPictureList()
}

/**
 * 移除标签
 */
const removeTag = (tag: string) => {
  selectedTags.value = selectedTags.value.filter(t => t !== tag)
  searchParams.value.tags = selectedTags.value.length > 0 ? selectedTags.value : undefined
  searchParams.value.current = 1
  fetchPictureList()
}

/**
 * 清除所有标签
 */
const clearAllTags = () => {
  selectedTags.value = []
  searchParams.value.tags = undefined
  searchParams.value.current = 1
  fetchPictureList()
}

/**
 * 搜索
 */
const handleSearch = () => {
  searchParams.value.current = 1
  fetchPictureList()
}

/**
 * 打开上传弹窗
 */
const openUpload = () => {
  if (!loginUserStore.loginUser) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  uploadOpen.value = true
}

/**
 * 打开批量拉取弹窗
 */
const openBatchFetch = () => {
  batchFetchOpen.value = true
}

/**
 * 跳转到详情页
 */
const goToDetail = (id?: number) => {
  if (id) {
    router.push(`/picture/detail/${id}`)
  }
}

/**
 * 格式化文件大小
 */
const formatSize = (size?: number) => {
  // 先转数字，空值返回 0 B
  const numSize = Number(size);
  if (!numSize || isNaN(numSize)) return '0 B';
  
  const units = ['B', 'KB', 'MB', 'GB'];
  let index = 0;
  let value = numSize; // 确保是数字

  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index++;
  }
  
  return `${value.toFixed(1)} ${units[index]}`;
};

// 页面加载时获取数据
onMounted(() => {
  fetchPictureList()
  fetchCategoryAndTags()
})
</script>

<style scoped>
.picture-gallery {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 顶部搜索区域 */
.search-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px;
  text-align: center;
}

.gallery-title {
  color: #fff;
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 24px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.search-input {
  max-width: 600px;
  margin: 0 auto;
}

.search-input :deep(.ant-input) {
  height: 50px;
  font-size: 16px;
  border-radius: 25px 0 0 25px;
  padding-left: 24px;
}

.search-input :deep(.ant-input-search-button) {
  height: 50px;
  width: 80px;
  border-radius: 0 25px 25px 0;
  font-size: 16px;
}

.search-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 筛选区域 */
.filter-section {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.filter-block {
  margin-bottom: 16px;
}

.filter-block:last-child {
  margin-bottom: 0;
}

.filter-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-right: 12px;
  white-space: nowrap;
}

.filter-options {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.filter-option {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f5f5;
  color: #666;
  user-select: none;
}

.filter-option:hover {
  background: #e6f7ff;
  color: #1890ff;
}

.filter-option.active {
  background: #1890ff;
  color: #fff;
}

.tag-option {
  border-radius: 14px;
  padding: 4px 12px;
  font-size: 13px;
}

.selected-tags {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8e8e8;
}

.selected-label {
  font-size: 13px;
  color: #666;
  margin-right: 8px;
}

.selected-tag-item {
  margin-bottom: 4px;
}

/* 图片内容区域 */
.gallery-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* 图片网格 */
.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.picture-item {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.picture-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.picture-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.picture-info {
  padding: 12px 16px;
}

.picture-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}

.picture-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.picture-meta span:first-child {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 4px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 20px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-header {
    padding: 40px 16px;
  }

  .gallery-title {
    font-size: 28px;
  }

  .search-input {
    max-width: 100%;
  }

  .picture-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .picture-item img {
    height: 140px;
  }
}
</style>
