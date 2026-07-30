<template>
  <div id="pictureManagePage">
    <!-- 非管理员查看自己的图片 -->
    <template v-if="loginUserStore.loginUser && loginUserStore.loginUser.userstatus !== 'admin'">
      <div class="page-header">
        <h1>我的图片管理 - {{ loginUserStore.loginUser.username }}</h1>
        <div class="header-actions">
          <a-button type="primary" @click="router.push('/picture')">
            <PictureOutlined /> 浏览图库
          </a-button>
          <a-button type="primary" @click="openUploadModal">
            <UploadOutlined /> 上传图片
          </a-button>
        </div>
      </div>
    </template>

    <!-- 管理员查看所有图片 -->
    <template v-else-if="loginUserStore.loginUser?.userstatus === 'admin'">
      <div class="page-header">
        <h1>图片管理（管理员）</h1>
        <div class="header-actions">
          <a-button type="primary" @click="router.push('/picture')">
            <PictureOutlined /> 浏览图库
          </a-button>
          <a-button type="primary" @click="openUploadModal">
            <UploadOutlined /> 上传图片
          </a-button>
        </div>
      </div>
    </template>
    <!-- 未登录 -->
    <template v-else>
      <a-result status="warning" title="请先登录" sub-title="登录后可管理您的图片">
        <template #extra>
          <a-button type="primary" @click="router.push('/user/login')">立即登录</a-button>
        </template>
      </a-result>
    </template>

    <a-select v-model:value="searchParams.spaceScope" placeholder="空间范围" v-if="loginUserStore.loginUser != null">
      <a-select-option value="public">公共图库</a-select-option>
      <a-select-option value="space">私人空间</a-select-option>
    </a-select>

    <!-- 搜索和操作区域 -->
    <div v-if="loginUserStore.loginUser" class="search-area">
      <a-form layout="inline" :model="searchParams">
        <a-form-item>
          <a-input
            v-model:value="searchParams.name"
            placeholder="搜索图片名称"
            allow-clear
            style="width: 200px"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="分类">
          <a-select
            v-model:value="searchParams.category"
            placeholder="全部分类"
            allow-clear
            style="width: 120px"
          >
            <a-select-option v-for="cat in categoryList" :key="cat" :value="cat">
              {{ cat }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model:value="selectedTags"
            mode="multiple"
            placeholder="选择标签"
            allow-clear
            style="min-width: 180px"
            :max-tag-count="3"
            :options="tagList.map(tag => ({ label: tag, value: tag }))"
            @change="handleSearch"
          />
        </a-form-item>
        <a-form-item label="审核状态">
          <a-select
            v-model:value="searchParams.pictureCheck"
            placeholder="全部状态"
            allow-clear
            style="width: 120px"
            @change="handleSearch"
          >
            <a-select-option :value="0">
              <a-badge status="warning" text="待审核" />
            </a-select-option>
            <a-select-option :value="1">
              <a-badge status="success" text="审核通过" />
            </a-select-option>
            <a-select-option :value="2">
              <a-badge status="error" text="审核拒绝" />
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            <SearchOutlined /> 搜索
          </a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>

      <!-- 批量操作 -->
      <div class="batch-actions">
        <a-space>
          <a-checkbox
            v-model:checked="selectAll"
            :indeterminate="indeterminate"
            @change="handleSelectAll"
          >
            全选
          </a-checkbox>
          <span class="selected-count" v-if="selectedIds.length > 0">
            已选择 {{ selectedIds.length }} 张图片
          </span>
          <a-button
            type="primary"
            danger
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            <DeleteOutlined /> 批量删除
          </a-button>

          <a-button
            type="primary"
            v-if="searchParams.pictureCheck === 0"
            :disabled="selectedIds.length === 0"
            @click="handleBatchCheckPass"
          >
            <CloseCircleOutlined /> 批量审核通过
          </a-button>

          <a-button
            type="primary"
            danger
            v-if="searchParams.pictureCheck === 0"
            :disabled="selectedIds.length === 0"
            @click="handleBatchCheckRefuse"
          >
            <CheckCircleOutlined /> 批量审核拒绝
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 图片列表 -->
    <div v-if="loginUserStore.loginUser" class="picture-list">
      <a-table
        :columns="columns"
        :data-source="pictureList"
        :row-selection="rowSelection"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
        :locale="{ emptyText: '暂无图片数据，快去上传把~' }"
      >
        <!-- 图片预览 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'preview'">
            <div class="picture-preview">
              <img :src="record.url" :alt="record.name" @click="previewImage(record)" />
            </div>
          </template>

          <!-- 图片名称 -->
          <template v-if="column.key === 'name'">
            <div class="picture-name-cell">
              <span class="name">{{ record.name }}</span>
              <span class="intro" v-if="record.introduction">{{ record.introduction }}</span>
            </div>
          </template>

          <!-- 分类标签 -->
          <template v-if="column.key === 'category'">
            <a-tag color="blue" v-if="record.category">{{ record.category }}</a-tag>
            <span v-else class="empty-text">未分类</span>
          </template>

          <!-- 标签 -->
          <template v-if="column.key === 'tags'">
            <template v-if="record.tags && record.tags.length > 0">
              <a-tag v-for="tag in record.tags.slice(0, 2)" :key="tag" color="green">
                {{ tag }}
              </a-tag>
              <span v-if="record.tags.length > 2">...</span>
            </template>
            <span v-else class="empty-text">无标签</span>
          </template>

          <!-- 图片属性 -->
          <template v-if="column.key === 'size'">
            <div class="size-info">
              <span>{{ record.picformat?.toUpperCase() }}</span>
              <span>{{ formatSize(record.picsize) }}</span>
            </div>
          </template>

          <!-- 审核状态 -->
          <template v-if="column.key === 'pictureCheck'">
            <a-tag :color="getStatusColor(record.pictureCheck)">
              {{ getStatusText(record.pictureCheck) }}
            </a-tag>
          </template>

          <!-- 上传者（仅管理员可见） -->
          <template v-if="column.key === 'uploader'">
            <div class="uploader-info" v-if="record.createdUser">
              <a-avatar :src="record.createdUser.avatarurl" :size="24" />
              <span>{{ record.createdUser.username }}</span>
            </div>
          </template>

          <!-- 审核人 -->
          <template v-if="column.key === 'checkAdmin'">
            <div class="check-admin-info" v-if="record.checkAdminName">
              <span>{{ record.checkAdminName }}</span>
            </div>
            <span v-else-if="record.checkAdminId">
              <span>管理员(ID:{{ record.checkAdminId }})</span>
            </span>
            <span v-else class="empty-text">-</span>
          </template>

          <!-- 审核原因 -->
          <template v-if="column.key === 'checkMessage'">
            <div class="check-message" v-if="record.checkMessage">
              <a-tooltip :title="record.checkMessage">
                <span class="message-text">{{ record.checkMessage }}</span>
              </a-tooltip>
            </div>
            <span v-else class="empty-text">-</span>
          </template>

          <!-- 上传时间 -->
          <template v-if="column.key === 'createtime'">
            {{ formatTime(record.createtime) }}
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <!-- 管理员操作 -->
              <template v-if="isAdmin">
                <!-- 待审核状态显示审核按钮 -->
                <template v-if="record.pictureCheck === 0">
                  <a-button type="link" size="small" @click="handleCheck(record, 1)">
                    <CheckCircleOutlined /> 通过
                  </a-button>
                  <a-button type="link" danger size="small" @click="handleCheck(record, 2)">
                    <CloseCircleOutlined /> 拒绝
                  </a-button>
                </template>
                <!-- 审核拒绝状态显示撤回和删除按钮 -->
                <template v-else-if="record.pictureCheck === 2">
                  <a-button type="link" size="small" @click="handleWithdraw(record)">
                    <UndoOutlined /> 撤回
                  </a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">
                    <DeleteOutlined /> 删除
                  </a-button>
                </template>
                <!-- 审核通过状态显示编辑和删除按钮 -->
                <template v-else>
                  <a-button type="link" size="small" @click="handleEdit(record)">
                    <EditOutlined /> 编辑
                  </a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">
                    <DeleteOutlined /> 删除
                  </a-button>
                </template>
              </template>
              <!-- 普通用户操作 -->
              <template v-else>
                <!-- 审核拒绝状态显示重新上传 -->
                <template v-if="record.pictureCheck === 2">
                  <a-button type="link" size="small" @click="handleReupload(record)">
                    <UploadOutlined /> 重新上传
                  </a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">
                    <DeleteOutlined /> 删除
                  </a-button>
                </template>
                <!-- 其他状态显示编辑和删除 -->
                <template v-else>
                  <a-button type="link" size="small" @click="handleEdit(record)">
                    <EditOutlined /> 编辑
                  </a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">
                    <DeleteOutlined /> 删除
                  </a-button>
                </template>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

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
    <a-modal
      v-model:open="previewVisible"
      :title="previewPicture?.name"
      :footer="null"
      centered
    >
      <img :src="previewPicture?.url" style="width: 100%" />
    </a-modal>

    <!-- 图片上传弹窗组件 -->
    <PictureUploadModal
      v-model:open="uploadModalOpen"
      :category-list="categoryList"
      :tag-list="tagList"
      :mode="uploadModalMode"
      :initial-data="reuploadInitialData"
      :original-picture-url="reuploadOriginalUrl"
      @success="handleUploadSuccess"
      @cancel="handleUploadCancel"
    />

    <!-- 批量拉取图片弹窗（仅管理员） -->
    <BatchFetchModal
      v-model:open="batchFetchOpen"
      @success="fetchPictureList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, TablePaginationConfig } from 'ant-design-vue'
import { useLoginUserStore } from '../../stores/useLoginUserStore'
import {
  queryPicturePageCacheUsingPost,
  deletePictureUsingDelete,
  adminCheckPictureUsingPut,
  listPictureCategoryUsingGet,
  uploadPicUsingPost,
  adminCheckPictureBatchUsingPut
} from '../../api/pictureController'
import { getUserByIdUsingGet } from '../../api/userController'
import {
  SearchOutlined,
  DeleteOutlined,
  EditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  UndoOutlined,
  PictureOutlined,
  UploadOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'
import PictureUploadModal from '../../components/PictureUploadModal.vue'
  import BatchFetchModal from '../../components/BatchFetchModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 状态
const loading = ref(false)
const pictureList = ref<API.PictureVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 分类和标签
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedTags = ref<string[]>([])

// 搜索参数
const searchParams = ref({
  name: '',
  category: undefined as string | undefined,
  tags: undefined as string[] | undefined,
  pictureCheck: 1,
  spaceScope: 'public' as string | undefined,
})

// 选中的图片ID
const selectedIds = ref<number[]>([])

// 全选状态
const selectAll = ref(false)
const indeterminate = ref(false)

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

// 预览相关
const previewVisible = ref(false)
const previewPicture = ref<API.PictureVO | null>(null)

// 上传弹窗相关
const uploadModalOpen = ref(false)
const batchFetchOpen = ref(false)
const uploadModalMode = ref<'upload' | 'reupload'>('upload')
const reuploadInitialData = ref<{
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
} | undefined>(undefined)
const reuploadOriginalUrl = ref<string | undefined>(undefined)
const reuploadOriginalId = ref<number | undefined>(undefined)

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser?.userstatus === 'admin'
})

// 表格列定义
const columns = computed(() => {
  // 审核拒绝状态下显示审核人和审核原因，不显示格式/大小
  const isCheckRefuse = searchParams.value.pictureCheck === 2

  const baseColumns = [
    { title: '预览', key: 'preview', width: 80 },
    { title: '图片名称', key: 'name', width: 200 },
    { title: '分类', key: 'category', width: 100 },
    { title: '标签', key: 'tags', width: 150 },
    // 审核拒绝状态下不显示格式/大小，显示审核信息
    ...(isCheckRefuse ? [] : [{ title: '格式/大小', key: 'size', width: 120 }]),
    { title: '审核状态', key: 'pictureCheck', width: 100 },
    // 审核拒绝状态下显示审核人和审核原因
    ...(isCheckRefuse ? [{ title: '审核人', key: 'checkAdmin', width: 120 }] : []),
    ...(isCheckRefuse ? [{ title: '审核原因', key: 'checkMessage', width: 200 }] : []),
    { title: '上传时间', key: 'createtime', width: 150 },
    { title: '操作', key: 'action', width: 150, fixed: 'right' as const }
  ]

  // 管理员显示上传者列（仅在非审核拒绝状态或调整位置）
  if (isAdmin.value && !isCheckRefuse) {
    baseColumns.splice(6, 0, { title: '上传者', key: 'uploader', width: 120 })
  }

  return baseColumns
})

// 分页配置
const pagination = computed<TablePaginationConfig>(() => ({
  current: currentPage.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 张图片`,
  pageSizeOptions: ['10', '20', '50']
}))

// 行选择配置
const rowSelection = computed<TableProps['rowSelection']>(() => ({
  selectedRowKeys: selectedIds.value,
  onChange: (selectedKeys: (string | number)[]) => {
    selectedIds.value = selectedKeys as number[]
    updateSelectAllState()
  }
}))

/**
 * 更新全选状态
 */
const updateSelectAllState = () => {
  if (selectedIds.value.length === 0) {
    selectAll.value = false
    indeterminate.value = false
  } else if (selectedIds.value.length === pictureList.value.length) {
    selectAll.value = true
    indeterminate.value = false
  } else {
    selectAll.value = false
    indeterminate.value = true
  }
}

/**
 * 全选/取消全选
 */
const handleSelectAll = (e: any) => {
  const checked = e.target.checked
  if (checked) {
    selectedIds.value = pictureList.value.map(p => p.id!).filter(Boolean)
  } else {
    selectedIds.value = []
  }
  updateSelectAllState()
}

/**
 * 获取图片列表
 */
const fetchPictureList = async () => {
  loading.value = true
  try {
    // 用户只能看到自己的图片
    if(!isAdmin.value){
      const user_params: API.PictureQueryRequest = {
      current: currentPage.value,
      pageSize: pageSize.value,
      name: searchParams.value.name || undefined,
      category: searchParams.value.category,
      tags: selectedTags.value.length > 0 ? selectedTags.value : undefined,
      // 限制只能看自己的
      userId: loginUserStore.loginUser?.id,
      pictureCheck: searchParams.value.pictureCheck,
      spaceId: searchParams.value.spaceScope === 'public' ? 0 : loginUserStore.loginUser?.spaceId,
    }
      const res = await queryPicturePageCacheUsingPost(user_params)
      if (res.data.code === 200) {
        pictureList.value = res.data.data?.pictureList || []
        total.value = res.data.data?.total || 0
        // 查询审核人姓名
        await fetchCheckAdminNames()
      } else {
        message.error('获取图片列表失败：' + res.data.message)
      }
    }else{
      // 管理员能看到所有用户的图片
      const params: API.PictureQueryRequest = {
      current: currentPage.value,
      pageSize: pageSize.value,
      name: searchParams.value.name || undefined,
      category: searchParams.value.category,
      tags: selectedTags.value.length > 0 ? selectedTags.value : undefined,
      pictureCheck: searchParams.value.pictureCheck,
      spaceId: searchParams.value.spaceScope === 'public' ? 0 : -1,
    }
      const res = await queryPicturePageCacheUsingPost(params)
      if (res.data.code === 200) {
        pictureList.value = res.data.data?.pictureList || []
        total.value = res.data.data?.total || 0
        // 查询审核人姓名
        await fetchCheckAdminNames()
      } else {
        message.error('获取图片列表失败：' + res.data.message)
      }
    }
  } catch (error) {
    message.error('获取图片列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查询审核人姓名
 */
const fetchCheckAdminNames = async () => {
  // 获取所有需要查询的审核人ID（去重）
  const adminIds = [...new Set(
    pictureList.value
      .filter(pic => pic.checkAdminId && !pic.checkAdminName)
      .map(pic => pic.checkAdminId)
  )]

  if (adminIds.length === 0) return

  // 并行查询所有审核人信息
  const promises = adminIds.map(async (id) => {
    try {
      const res = await getUserByIdUsingGet({ id })
      if (res.data.code === 200 && res.data.data) {
        return { id, username: res.data.data.username }
      }
    } catch (error) {
      console.error('查询审核人失败:', error)
    }
    return null
  })

  const results = await Promise.all(promises)

  // 将查询到的姓名更新到图片列表中
  results.forEach(result => {
    if (result) {
      pictureList.value.forEach(pic => {
        if (pic.checkAdminId === result.id) {
          pic.checkAdminName = result.username
        }
      })
    }
  })
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
 * 搜索
 */
const handleSearch = () => {
  currentPage.value = 1
  selectedIds.value = []
  fetchPictureList()
}

/**
 * 重置搜索
 */
const resetSearch = () => {
  searchParams.value = {
    searchText: '',
    category: undefined,
    tags: undefined,
    pictureCheck: undefined
  }
  selectedTags.value = []
  currentPage.value = 1
  selectedIds.value = []
  fetchPictureList()
}

/**
 * 表格变化（分页）
 */
const handleTableChange = (pagination: TablePaginationConfig) => {
  currentPage.value = pagination.current || 1
  pageSize.value = pagination.pageSize || 10
  selectedIds.value = []
  fetchPictureList()
}

/**
 * 预览图片
 */
const previewImage = (picture: API.PictureVO) => {
  previewPicture.value = picture
  previewVisible.value = true
}

/**
 * 打开编辑弹窗
 */
const handleEdit = (record: API.PictureVO) => {
  editForm.value = {
    id: record.id,
    name: record.name || '',
    introduction: record.introduction || '',
    category: record.category,
    tags: record.tags || [],
    originalUrl: record.url  // 保存原图片URL
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
      // 用户上传接口
      const res = await uploadPicUsingPost(
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
        fetchPictureList()
      } else {
        message.error('更新失败：' + res.data.message)
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
 * 删除单个图片
 */
/**
 * 审核图片
 */
const handleCheck = async (record: API.PictureVO, checkResult: number) => {
  const actionText = checkResult === 1 ? '通过' : '拒绝'

  // 审核拒绝时需要输入拒绝原因
  if (checkResult === 2) {
    let rejectReason = ''
    Modal.confirm({
      title: '审核拒绝',
      content: h('div', [
        h('p', { style: 'margin-bottom: 8px;' }, '请输入拒绝原因：'),
        h('textarea', {
          placeholder: '请输入拒绝原因（必填）',
          style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; resize: vertical; min-height: 80px;',
          onInput: (e: any) => {
            rejectReason = e.target.value
          }
        })
      ]),
      okText: '确认拒绝',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        if (!rejectReason.trim()) {
          message.error('请输入拒绝原因')
          return Promise.reject('请输入拒绝原因')
        }
        try {
          const res = await adminCheckPictureUsingPut({
            picId: record.id,
            checkResult: checkResult,
            checkMessage: rejectReason
          })
          if (res.data.code === 200) {
            message.success('审核拒绝成功')
            fetchPictureList()
          } else {
            message.error('审核失败：' + res.data.message)
          }
        } catch (error) {
          message.error('审核失败')
        }
      }
    })
  } else {
    // 审核通过不需要原因
    Modal.confirm({
      title: '确认审核通过',
      content: '确定要通过这张图片吗？',
      okText: '确认通过',
      okType: 'primary',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await adminCheckPictureUsingPut({
            picId: record.id,
            checkResult: checkResult
          })
          if (res.data.code === 200) {
            message.success('审核通过成功')
            fetchPictureList()
          } else {
            message.error('审核失败：' + res.data.message)
          }
        } catch (error) {
          message.error('审核失败')
        }
      }
    })
  }
}

/**
 * 撤回审核（将审核拒绝改为审核通过）
 */
const handleWithdraw = async (record: API.PictureVO) => {
  Modal.confirm({
    title: '确认撤回',
    content: '确定要撤回该图片的审核拒绝状态，将其改为审核通过吗？',
    okText: '确认撤回',
    okType: 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await adminCheckPictureUsingPut({
          picId: record.id,
          checkResult: 1  // 改为审核通过
        })
        if (res.data.code === 200) {
          message.success('撤回成功，图片已改为审核通过状态')
          fetchPictureList()
        } else {
          message.error('撤回失败：' + res.data.message)
        }
      } catch (error) {
        message.error('撤回失败')
      }
    }
  })
}

const handleDelete = (id?: number) => {
  if (!id) return

  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这张图片吗？此操作不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deletePictureUsingDelete({ id })
        if (res.data.code === 200) {
          message.success('删除成功')
          fetchPictureList()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

/**
 * 打开上传弹窗
 */
const openUploadModal = () => {
  uploadModalMode.value = 'upload'
  reuploadInitialData.value = undefined
  reuploadOriginalUrl.value = undefined
  reuploadOriginalId.value = undefined
  uploadModalOpen.value = true
}

/**
 * 处理重新上传
 */
const handleReupload = (record: API.PictureVO) => {
  uploadModalMode.value = 'reupload'
  reuploadInitialData.value = {
    id: record.id,  // 传入原图片ID，后端会视为更新操作
    name: record.name,
    introduction: record.introduction,
    category: record.category,
    tags: record.tags
  }
  reuploadOriginalUrl.value = record.url
  reuploadOriginalId.value = record.id
  uploadModalOpen.value = true
}

/**
 * 上传成功回调
 */
const handleUploadSuccess = async () => {
  // 注意：后端 uploadPicture 接口已经处理了更新逻辑（包括删除旧图片）
  // 前端只需要刷新列表即可
  // 刷新列表
  fetchPictureList()
  // 重置重新上传数据
  reuploadInitialData.value = undefined
  reuploadOriginalUrl.value = undefined
  reuploadOriginalId.value = undefined
}

/**
 * 上传取消回调
 */
const handleUploadCancel = () => {
  // 重置重新上传数据
  reuploadInitialData.value = undefined
  reuploadOriginalUrl.value = undefined
  reuploadOriginalId.value = undefined
}

/**
 * 批量删除
 */
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择要删除的图片')
    return
  }

  Modal.confirm({
    title: '批量删除确认',
    content: `确定要删除选中的 ${selectedIds.value.length} 张图片吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      let successCount = 0
      let failCount = 0

      for (const id of selectedIds.value) {
        try {
          const res = await deletePictureUsingDelete({ id })
          if (res.data.code === 200) {
            successCount++
          } else {
            failCount++
          }
        } catch {
          failCount++
        }
      }

      if (successCount > 0) {
        message.success(`成功删除 ${successCount} 张图片`)
      }
      if (failCount > 0) {
        message.error(`${failCount} 张图片删除失败`)
      }

      selectedIds.value = []
      fetchPictureList()
    }
  })
}

/**
 * 批量审核通过
 */
const handleBatchCheckPass =()=>{
  if (selectedIds.value.length === 0) {
    message.warning('请先选择要审核的图片')
    return
  }
  
  Modal.confirm({
      title: '确认审核通过',
      content: `确定要通过这${selectedIds.value.length}图片吗？`,
      okText: '确认通过',
      okType: 'primary',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await adminCheckPictureBatchUsingPut({
            picIds: selectedIds.value,
            checkResult: 1
          })
          if (res.data.code === 200) {
            message.success('审核通过成功')
            fetchPictureList()
          } else {
            message.error('审核失败：' + res.data.message)
          }
        } catch (error) {
          message.error('审核失败')
        }
      }
    })

}

/**
 * 批量审核拒绝
 */
const handleBatchCheckRefuse =()=>{
  if (selectedIds.value.length === 0) {
    message.warning('请先选择要审核的图片')
    return
  }
  let rejectReason = ''
    Modal.confirm({
      title: '审核拒绝',
      content: h('div', [
        h('p', { style: 'margin-bottom: 8px;' }, '请输入拒绝原因：'),
        h('textarea', {
          placeholder: '请输入拒绝原因（必填）',
          style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; resize: vertical; min-height: 80px;',
          onInput: (e: any) => {
            rejectReason = e.target.value
          }
        })
      ]),
      okText: '确认拒绝',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        if (!rejectReason.trim()) {
          message.error('请输入拒绝原因')
          return Promise.reject('请输入拒绝原因')
        }
        try {
          const res = await adminCheckPictureBatchUsingPut({
            picIds: selectedIds.value,
            checkResult: 2,
            checkMessage: rejectReason
          })
          if (res.data.code === 200) {
            message.success('审核拒绝成功')
            fetchPictureList()
          } else {
            message.error('审核失败：' + res.data.message)
          }
        } catch (error) {
          message.error('审核失败')
        }
      }
    })
}

/**
 * 格式化文件大小
 */
const formatSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 获取审核状态文本
 */
const getStatusText = (status?: number) => {
  console.log(status)
  switch (status) {
    case 0:
      return '待审核'
    case 1:
      return '审核通过'
    case 2:
      return '审核拒绝'
    default:
      return '未知'
  }
}

/**
 * 获取审核状态颜色
 */
const getStatusColor = (status?: number) => {
  switch (status) {
    case 0:
      return 'blue'  // 待审核 - 蓝色
    case 1:
      return 'green'  // 审核通过 - 绿色
    case 2:
      return 'red'    // 审核拒绝 - 红色
    default:
      return 'default'
  }
}

/**
 * 格式化时间
 */
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 页面加载
onMounted(() => {
  if (loginUserStore.loginUser) {
    fetchPictureList()
    fetchCategoryAndTags()
  }
})
</script>

<style scoped>
#pictureManagePage {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-area {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.batch-actions {
  display: flex;
  align-items: center;
}

.selected-count {
  color: #1890ff;
  font-weight: 500;
}

.picture-list {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.picture-preview {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
}

.picture-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.picture-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.picture-name-cell .name {
  font-weight: 500;
  color: #333;
}

.picture-name-cell .intro {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.size-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: #666;
}

.uploader-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-text {
  color: #999;
  font-size: 12px;
}

/* 审核人信息 */
.check-admin-info {
  color: #333;
  font-size: 13px;
}

/* 审核原因 */
.check-message {
  max-width: 180px;
}

.check-message .message-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
  font-size: 13px;
}

.edit-form {
  padding: 16px 0 0;
}

/* 响应式 */
@media (max-width: 768px) {
  #pictureManagePage {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .search-area {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
